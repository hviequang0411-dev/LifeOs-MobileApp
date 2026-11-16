import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

/// Máy chủ proxy phát triển cục bộ (Same-Origin + CORS) cho Flutter Web trên `localhost`.
///
/// Chuyển tiếp mọi yêu cầu `/api/*` tới Spring Boot backend (`http://localhost:8080`)
/// và phục vụ thư mục `mobile/build/web` trên cùng nguồn (`http://localhost:8090`),
/// giúp kiểm thử toàn bộ UI + Auth + Sync thật trên Chrome/Edge mà không cần sửa `backend/`.
class LifeOsWebDevProxy {
  LifeOsWebDevProxy({
    required this.webRootDir,
    Uri? backendBaseUri,
    this.host = 'localhost',
    this.port = 8090,
  }) : backendBaseUri = backendBaseUri ?? Uri.parse('http://localhost:8080');

  final Directory webRootDir;
  final Uri backendBaseUri;
  final String host;
  final int port;

  HttpServer? _server;
  final HttpClient _httpClient = HttpClient()
    ..connectionTimeout = const Duration(seconds: 10);

  int get boundPort => _server?.port ?? port;

  Future<void> start() async {
    if (_server != null) return;
    _server = await HttpServer.bind(InternetAddress.loopbackIPv4, port);
    _server!.listen(_handleRequest);
  }

  Future<void> stop() async {
    final HttpServer? s = _server;
    _server = null;
    if (s != null) {
      await s.close(force: true);
    }
    _httpClient.close(force: true);
  }

  Future<void> _handleRequest(HttpRequest request) async {
    _applyCorsHeaders(request.response.headers);

    if (request.method.toUpperCase() == 'OPTIONS') {
      request.response.statusCode = HttpStatus.noContent;
      await request.response.close();
      return;
    }

    final String path = request.uri.path;
    if (path == '/healthz') {
      request.response.statusCode = HttpStatus.ok;
      request.response.headers.contentType = ContentType.json;
      request.response.write(
        jsonEncode(<String, dynamic>{
          'status': 'ok',
          'proxyOrigin': 'http://$host:$boundPort',
          'backendOrigin': backendBaseUri.toString(),
          'webRoot': webRootDir.path,
        }),
      );
      await request.response.close();
      return;
    }

    if (path == '/api' || path.startsWith('/api/')) {
      await _proxyToBackend(request);
      return;
    }

    await _serveStaticWebAsset(request);
  }

  Future<void> _proxyToBackend(HttpRequest request) async {
    final String query = request.uri.hasQuery ? '?${request.uri.query}' : '';
    final Uri targetUri = Uri.parse(
      '${backendBaseUri.origin}${request.uri.path}$query',
    );

    try {
      final HttpClientRequest upstreamReq =
          await _httpClient.openUrl(request.method, targetUri);
      request.headers.forEach((String name, List<String> values) {
        final String lower = name.toLowerCase();
        if (lower == 'host' ||
            lower == 'connection' ||
            lower == 'content-length' ||
            lower == 'transfer-encoding' ||
            lower == 'origin' ||
            lower == 'referer') {
          return;
        }
        for (final String v in values) {
          upstreamReq.headers.add(name, v);
        }
      });

      final List<int> bodyBytes = await _readAllBytes(request);
      if (bodyBytes.isNotEmpty) {
        upstreamReq.contentLength = bodyBytes.length;
        upstreamReq.add(bodyBytes);
      }

      final HttpClientResponse upstreamRes = await upstreamReq.close();
      final List<int> resBytes = await _readAllBytes(upstreamRes);

      request.response.statusCode = upstreamRes.statusCode;
      upstreamRes.headers.forEach((String name, List<String> values) {
        final String lower = name.toLowerCase();
        if (lower == 'transfer-encoding' ||
            lower == 'connection' ||
            lower == 'content-length' ||
            lower.startsWith('access-control-')) {
          return;
        }
        for (final String v in values) {
          request.response.headers.add(name, v);
        }
      });
      _applyCorsHeaders(request.response.headers);
      request.response.contentLength = resBytes.length;
      request.response.add(resBytes);
      await request.response.close();
    } catch (e) {
      request.response.statusCode = HttpStatus.badGateway;
      request.response.headers.contentType = ContentType.json;
      request.response.write(
        jsonEncode(<String, dynamic>{
          'errorCode': 'PROXY_BACKEND_UNREACHABLE',
          'message':
              'Không kết nối được backend tại ${backendBaseUri.origin}: $e',
        }),
      );
      await request.response.close();
    }
  }

  Future<void> _serveStaticWebAsset(HttpRequest request) async {
    final String rawPath = request.uri.path == '/' ? '/index.html' : request.uri.path;
    final String cleanRel = rawPath
        .split('/')
        .where((String seg) => seg.isNotEmpty && seg != '.' && seg != '..')
        .join(Platform.pathSeparator);

    File candidate = File(
      '${webRootDir.path}${Platform.pathSeparator}$cleanRel',
    );
    if (!candidate.existsSync()) {
      final bool looksLikeAsset = cleanRel.contains('.');
      if (!looksLikeAsset) {
        candidate = File(
          '${webRootDir.path}${Platform.pathSeparator}index.html',
        );
      }
    }

    if (!candidate.existsSync()) {
      request.response.statusCode = HttpStatus.notFound;
      request.response.headers.contentType = ContentType.html;
      request.response.write(
        '<!doctype html><html lang="vi"><meta charset="utf-8">'
        '<title>LifeOS Web chưa build</title><body style="font-family:sans-serif;padding:24px">'
        '<h2>Chưa có bản build Flutter Web tại <code>${_escapeHtml(webRootDir.path)}</code></h2>'
        '<p>Hãy chạy: <code>flutter build web --no-tree-shake-icons</code> trong thư mục <code>mobile/</code>.</p>'
        '</body></html>',
      );
      await request.response.close();
      return;
    }

    final String contentType = _contentTypeForPath(candidate.path);
    request.response.statusCode = HttpStatus.ok;
    request.response.headers.set(HttpHeaders.contentTypeHeader, contentType);
    if (candidate.path.endsWith('.wasm') ||
        candidate.path.endsWith('.ttf') ||
        candidate.path.endsWith('.otf')) {
      request.response.headers.set(
        HttpHeaders.cacheControlHeader,
        'public, max-age=3600',
      );
    } else {
      request.response.headers.set(
        HttpHeaders.cacheControlHeader,
        'no-cache',
      );
    }
    final List<int> bytes = await candidate.readAsBytes();
    request.response.contentLength = bytes.length;
    request.response.add(bytes);
    await request.response.close();
  }

  void _applyCorsHeaders(HttpHeaders headers) {
    headers.set('Access-Control-Allow-Origin', '*');
    headers.set(
      'Access-Control-Allow-Methods',
      'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    );
    headers.set(
      'Access-Control-Allow-Headers',
      'Origin, Content-Type, Accept, Authorization, X-Device-Id, X-Request-Id',
    );
    headers.set('Access-Control-Max-Age', '86400');
  }

  static Future<List<int>> _readAllBytes(Stream<List<int>> stream) async {
    final BytesBuilder builder = BytesBuilder(copy: false);
    await for (final List<int> chunk in stream) {
      builder.add(chunk);
    }
    return builder.takeBytes();
  }

  static String _contentTypeForPath(String filePath) {
    final String lower = filePath.toLowerCase();
    if (lower.endsWith('.html')) return 'text/html; charset=utf-8';
    if (lower.endsWith('.js') || lower.endsWith('.mjs')) {
      return 'application/javascript; charset=utf-8';
    }
    if (lower.endsWith('.wasm')) return 'application/wasm';
    if (lower.endsWith('.css')) return 'text/css; charset=utf-8';
    if (lower.endsWith('.json')) return 'application/json; charset=utf-8';
    if (lower.endsWith('.png')) return 'image/png';
    if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image/jpeg';
    if (lower.endsWith('.svg')) return 'image/svg+xml';
    if (lower.endsWith('.ico')) return 'image/x-icon';
    if (lower.endsWith('.ttf')) return 'font/ttf';
    if (lower.endsWith('.otf')) return 'font/otf';
    if (lower.endsWith('.woff2')) return 'font/woff2';
    return 'application/octet-stream';
  }

  static String _escapeHtml(String raw) => raw
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;');
}

Future<void> main(List<String> args) async {
  int port = 8090;
  String backendUrl = 'http://localhost:8080';
  String webDirPath = 'build/web';

  for (final String arg in args) {
    if (arg.startsWith('--port=')) {
      port = int.tryParse(arg.substring('--port='.length)) ?? port;
    } else if (arg.startsWith('--backend=')) {
      backendUrl = arg.substring('--backend='.length);
    } else if (arg.startsWith('--web-dir=')) {
      webDirPath = arg.substring('--web-dir='.length);
    }
  }

  final LifeOsWebDevProxy proxy = LifeOsWebDevProxy(
    webRootDir: Directory(webDirPath),
    backendBaseUri: Uri.parse(backendUrl),
    port: port,
  );
  await proxy.start();

  stdout.writeln('============================================================');
  stdout.writeln('LifeOS Web Dev Proxy đang chạy tại: http://localhost:${proxy.boundPort}');
  stdout.writeln('  • Phục vụ Web tĩnh từ : ${proxy.webRootDir.absolute.path}');
  stdout.writeln('  • Chuyển tiếp /api/*  -> ${proxy.backendBaseUri}');
  stdout.writeln('  • Mailpit (mã email)  -> http://localhost:8025');
  stdout.writeln('Nhấn Ctrl+C để dừng.');
  stdout.writeln('============================================================');

  await ProcessSignal.sigint.watch().first;
  await proxy.stop();
}
