import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/database/local_extras_database.dart';
import 'package:lifeos_mobile/core/database/sqlite_runtime.dart';

import '../tool/web_dev_proxy.dart';

void main() {
  group('LifeOsWebDevProxy & Web Portability', () {
    test('Phục vụ tĩnh HTML/WASM, xử lý CORS OPTIONS và chuyển tiếp /api/*',
        () async {
      final Directory tempDir =
          Directory.systemTemp.createTempSync('lifeos_web_proxy_test_');
      addTearDown(() {
        if (tempDir.existsSync()) {
          tempDir.deleteSync(recursive: true);
        }
      });

      File('${tempDir.path}${Platform.pathSeparator}index.html')
          .writeAsStringSync('<html><body>LifeOS Web OK</body></html>');
      File('${tempDir.path}${Platform.pathSeparator}sqlite3.wasm')
          .writeAsBytesSync(<int>[0x00, 0x61, 0x73, 0x6d]);

      // Mock backend trên cổng ngẫu nhiên
      final HttpServer mockBackend =
          await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      addTearDown(() => mockBackend.close(force: true));

      mockBackend.listen((HttpRequest req) async {
        final String body = await utf8.decodeStream(req);
        req.response.statusCode = HttpStatus.created;
        req.response.headers.contentType = ContentType.json;
        req.response.write(
          jsonEncode(<String, dynamic>{
            'method': req.method,
            'path': req.uri.path,
            'query': req.uri.query,
            'auth': req.headers.value('authorization'),
            'echoBody': body,
          }),
        );
        await req.response.close();
      });

      final LifeOsWebDevProxy proxy = LifeOsWebDevProxy(
        webRootDir: tempDir,
        backendBaseUri: Uri.parse('http://localhost:${mockBackend.port}'),
        port: 0,
      );
      await proxy.start();
      addTearDown(proxy.stop);

      final HttpClient client = HttpClient();
      addTearDown(() => client.close(force: true));

      // 1. Kiểm tra index.html (SPA)
      final HttpClientRequest htmlReq = await client.getUrl(
        Uri.parse('http://localhost:${proxy.boundPort}/'),
      );
      final HttpClientResponse htmlRes = await htmlReq.close();
      final String htmlText = await utf8.decodeStream(htmlRes);
      expect(htmlRes.statusCode, HttpStatus.ok);
      expect(htmlText, contains('LifeOS Web OK'));

      // 2. Kiểm tra MIME type application/wasm cho sqlite3.wasm
      final HttpClientRequest wasmReq = await client.getUrl(
        Uri.parse('http://localhost:${proxy.boundPort}/sqlite3.wasm'),
      );
      final HttpClientResponse wasmRes = await wasmReq.close();
      await wasmRes.drain<void>();
      expect(wasmRes.statusCode, HttpStatus.ok);
      expect(
        wasmRes.headers.contentType?.mimeType,
        'application/wasm',
      );

      // 3. Kiểm tra CORS preflight OPTIONS
      final HttpClientRequest optReq = await client.openUrl(
        'OPTIONS',
        Uri.parse('http://localhost:${proxy.boundPort}/api/v1/auth/login'),
      );
      final HttpClientResponse optRes = await optReq.close();
      await optRes.drain<void>();
      expect(optRes.statusCode, HttpStatus.noContent);
      expect(optRes.headers.value('access-control-allow-origin'), '*');

      // 4. Kiểm tra chuyển tiếp POST /api/v1/auth/login kèm body + Authorization
      final HttpClientRequest apiReq = await client.postUrl(
        Uri.parse(
          'http://localhost:${proxy.boundPort}/api/v1/auth/login?remember=1',
        ),
      );
      apiReq.headers.contentType = ContentType.json;
      apiReq.headers.set('authorization', 'Bearer tok-123');
      apiReq.write('{"email":"test@lifeos.vn"}');
      final HttpClientResponse apiRes = await apiReq.close();
      final Map<String, dynamic> apiJson =
          (jsonDecode(await utf8.decodeStream(apiRes)) as Map<dynamic, dynamic>)
              .cast<String, dynamic>();

      expect(apiRes.statusCode, HttpStatus.created);
      expect(apiRes.headers.value('access-control-allow-origin'), '*');
      expect(apiJson['method'], 'POST');
      expect(apiJson['path'], '/api/v1/auth/login');
      expect(apiJson['query'], 'remember=1');
      expect(apiJson['auth'], 'Bearer tok-123');
      expect(apiJson['echoBody'], '{"email":"test@lifeos.vn"}');
    });

    test('Trả về 502 JSON khi backend không khả dụng', () async {
      final Directory tempDir =
          Directory.systemTemp.createTempSync('lifeos_web_proxy_502_');
      addTearDown(() {
        if (tempDir.existsSync()) {
          tempDir.deleteSync(recursive: true);
        }
      });

      final LifeOsWebDevProxy proxy = LifeOsWebDevProxy(
        webRootDir: tempDir,
        backendBaseUri: Uri.parse('http://127.0.0.1:59999'),
        port: 0,
      );
      await proxy.start();
      addTearDown(proxy.stop);

      final HttpClient client = HttpClient();
      addTearDown(() => client.close(force: true));

      final HttpClientRequest req = await client.getUrl(
        Uri.parse('http://localhost:${proxy.boundPort}/api/v1/tasks'),
      );
      final HttpClientResponse res = await req.close();
      final Map<String, dynamic> body =
          (jsonDecode(await utf8.decodeStream(res)) as Map<dynamic, dynamic>)
              .cast<String, dynamic>();

      expect(res.statusCode, HttpStatus.badGateway);
      expect(body['errorCode'], 'PROXY_BACKEND_UNREACHABLE');
    });

    test('SQLite runtime abstraction khởi tạo và thao tác an toàn', () async {
      await initializeSqliteRuntime();
      final AppDatabase db = AppDatabase.inMemory();
      expect(db.runQuickIntegrityCheck(), isTrue);
      db.close();

      final LifeOsLocalExtrasDatabase extras =
          LifeOsLocalExtrasDatabase.inMemory();
      expect(extras.getPlantedTreesCount(), 0);
      extras.close();

      deleteLocalDatabaseFiles('nonexistent-user-id');
    });
  });
}

