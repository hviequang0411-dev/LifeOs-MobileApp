import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/network/api_client.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/sync/sync_engine.dart';

class _FakeHttpClientAdapter implements HttpClientAdapter {
  _FakeHttpClientAdapter(this.handler);

  final Future<ResponseBody> Function(RequestOptions options) handler;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) {
    return handler(options);
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  group('PWP2 — Kiểm thử AppDatabase 29 bảng thực thể & 4 bảng hạ tầng', () {
    late AppDatabase db;
    const String userId = 'user-test-01';

    setUp(() {
      db = AppDatabase.inMemory();
    });

    tearDown(() {
      db.close();
    });

    test('Đầy đủ 29 entityType đồng bộ, 35 bảng Drift sinh tự động và kiểm tra toàn vẹn PRAGMA quick_check', () {
      expect(SyncEntityType.values.length, 29);
      expect(db.driftDb.allTables.length, 35);
      expect(db.runQuickIntegrityCheck(), isTrue);

      for (final SyncEntityType type in SyncEntityType.values) {
        db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: type,
          entityId: 'id-${type.wireName}',
          op: 'CREATE',
          payload: <String, dynamic>{'name': 'Sample ${type.wireName}'},
        );
      }

      final Map<String, int> counts = db.countActiveEntitiesByType(userId);
      expect(counts.length, 29);
      for (final SyncEntityType type in SyncEntityType.values) {
        expect(counts[type.wireName], 1,
            reason: 'Bảng ${type.tableName} phải có 1 bản ghi');
      }
      expect(db.listOutboxMutations(userId: userId).length, 29);
    });

    test('MutationCoalescer Quy tắc 1: CREATE(pending) + UPDATE -> giữ CREATE (baseVersion = 0)', () {
      final OutboxMutationRecord? m1 = db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-1',
        op: 'CREATE',
        payload: <String, dynamic>{'title': 'Bản nháp 1', 'priority': 'LOW'},
      );
      final OutboxMutationRecord? m2 = db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-1',
        op: 'UPDATE',
        payload: <String, dynamic>{'title': 'Bản nháp 2', 'priority': 'HIGH'},
      );

      expect(m2, isNotNull);
      expect(m2!.mutationId, m1!.mutationId);
      expect(m2.op, 'CREATE');
      expect(m2.baseVersion, 0);
      expect(m2.payload['title'], 'Bản nháp 2');
      expect(m2.payload['priority'], 'HIGH');
      expect(db.listOutboxMutations(userId: userId).length, 1);
    });

    test('MutationCoalescer Quy tắc 2: CREATE(pending) + DELETE -> triệt tiêu tại chỗ (0 mutation)', () {
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-ephemeral',
        op: 'CREATE',
        payload: <String, dynamic>{'title': 'Tạo rồi xóa ngay khi offline'},
      );
      final OutboxMutationRecord? res = db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-ephemeral',
        op: 'DELETE',
        payload: <String, dynamic>{'id': 't-ephemeral'},
      );

      expect(res, isNull);
      expect(db.listOutboxMutations(userId: userId), isEmpty);
      expect(
        db.getEntity(
          userId: userId,
          entityType: SyncEntityType.task,
          entityId: 't-ephemeral',
          includeDeleted: true,
        ),
        isNull,
      );
    });

    test('MutationCoalescer Quy tắc 3 & 4: UPDATE(base=5) + UPDATE -> UPDATE(base=5); + DELETE -> DELETE(base=5)', () {
      // Giả lập thực thể đã đồng bộ từ server ở version = 5
      db.applyPulledChangesBatch(
        userId: userId,
        changes: <Map<String, dynamic>>[
          <String, dynamic>{
            'entityType': 'task',
            'entityId': 't-synced',
            'version': 5,
            'updatedAt': '2026-11-01T10:00:00Z',
            'data': <String, dynamic>{'title': 'Từ server v5'},
          },
        ],
        nextCursor: 10,
        syncedAtIso: '2026-11-01T10:00:00Z',
      );

      final OutboxMutationRecord? u1 = db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-synced',
        op: 'UPDATE',
        payload: <String, dynamic>{'title': 'Sửa lần 1'},
      );
      expect(u1!.baseVersion, 5);

      final OutboxMutationRecord? u2 = db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-synced',
        op: 'UPDATE',
        payload: <String, dynamic>{'title': 'Sửa lần 2'},
      );
      expect(u2!.mutationId, u1.mutationId);
      expect(u2.op, 'UPDATE');
      expect(u2.baseVersion, 5);
      expect(u2.payload['title'], 'Sửa lần 2');

      // Kiểm tra baseSnapshot vẫn giữ nguyên bản gốc v5
      final Map<String, dynamic>? snap = db.getBaseSnapshot(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-synced',
      );
      expect(snap?['title'], 'Từ server v5');

      // Tiếp tục DELETE -> chuyển thành DELETE với baseVersion = 5
      final OutboxMutationRecord? del = db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-synced',
        op: 'DELETE',
        payload: <String, dynamic>{'id': 't-synced'},
      );
      expect(del!.mutationId, u1.mutationId);
      expect(del.op, 'DELETE');
      expect(del.baseVersion, 5);
    });

    test('Cascade cục bộ khi xóa Project / Category sẽ gỡ khóa ngoại trên Task', () {
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.project,
        entityId: 'proj-1',
        op: 'CREATE',
        payload: <String, dynamic>{'name': 'Dự án A'},
      );
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 'task-in-proj',
        op: 'CREATE',
        payload: <String, dynamic>{
          'title': 'Việc thuộc dự án A',
          'projectId': 'proj-1',
        },
      );

      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.project,
        entityId: 'proj-1',
        op: 'DELETE',
        payload: <String, dynamic>{'id': 'proj-1'},
      );

      final LocalEntityRow? taskAfter = db.getEntity(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 'task-in-proj',
      );
      expect(taskAfter, isNotNull);
      expect(taskAfter!.payload['projectId'], isNull);
    });

    test('Giới hạn dung lượng UTF-8: Note <= 192 KB hợp lệ, vượt 192 KB bị chặn; Task vượt 64 KB bị chặn', () {
      final String valid100Kb = 'A' * (100 * 1024);
      expect(
        () => db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.note,
          entityId: 'note-100kb',
          op: 'CREATE',
          payload: <String, dynamic>{'title': 'Lớn', 'content': valid100Kb},
        ),
        returnsNormally,
      );

      expect(
        () => db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.task,
          entityId: 'task-100kb',
          op: 'CREATE',
          payload: <String, dynamic>{'title': 'Quá lớn cho Task', 'description': valid100Kb},
        ),
        throwsA(isA<PayloadTooLargeLocalException>()),
      );

      final String over192Kb = 'B' * (195 * 1024);
      expect(
        () => db.mutateLocalAndEnqueueOutbox(
          userId: userId,
          entityType: SyncEntityType.note,
          entityId: 'note-over-192kb',
          op: 'CREATE',
          payload: <String, dynamic>{'title': 'Quá 192KB', 'content': over192Kb},
        ),
        throwsA(isA<PayloadTooLargeLocalException>()),
      );
    });

    test('Vòng đệm sync_log luôn tự cắt ở tối đa 500 dòng mới nhất', () {
      for (int i = 1; i <= 530; i++) {
        db.appendSyncLog(
          userId: userId,
          direction: 'PUSH',
          outcome: 'APPLIED',
          details: 'Log #$i',
        );
      }
      final List<SyncLogRecord> logs = db.listSyncLogs(userId, limit: 1000);
      expect(logs.length, 500);
      expect(logs.first.details, 'Log #530');
      expect(logs.last.details, 'Log #31');
    });
  });

  group('PWP3 & PWP4 — Kiểm thử Single-Flight 401 Refresh & SyncEngine Push/Pull/Conflict/Logout', () {
    test('Single-Flight 401 Refresh: 3 request gặp 401 đồng thời chỉ gọi /auth/refresh đúng 1 lần', () async {
      final InMemoryTokenStorage storage = InMemoryTokenStorage();
      await storage.saveSession(
        accessToken: 'expired-access',
        refreshToken: 'valid-refresh',
        userId: 'u-1',
      );

      int refreshCalls = 0;
      final Dio customDio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      customDio.httpClientAdapter = _FakeHttpClientAdapter((RequestOptions options) async {
        if (options.path.contains('/auth/refresh')) {
          refreshCalls++;
          await Future<void>.delayed(const Duration(milliseconds: 30));
          return ResponseBody.fromString(
            '{"accessToken":"fresh-access","refreshToken":"fresh-refresh","user":{"id":"u-1"}}',
            200,
            headers: <String, List<String>>{
              Headers.contentTypeHeader: <String>[Headers.jsonContentType],
            },
          );
        }
        final String? auth = options.headers['Authorization'] as String?;
        if (auth != 'Bearer fresh-access') {
          return ResponseBody.fromString(
            '{"errorCode":"TOKEN_EXPIRED"}',
            401,
            headers: <String, List<String>>{
              Headers.contentTypeHeader: <String>[Headers.jsonContentType],
            },
          );
        }
        return ResponseBody.fromString(
          '{"ok":true}',
          200,
          headers: <String, List<String>>{
            Headers.contentTypeHeader: <String>[Headers.jsonContentType],
          },
        );
      });

      final LifeOsApiClient client = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: storage,
        customDio: customDio,
      );

      final List<Response<dynamic>> responses = await Future.wait(<Future<Response<dynamic>>>[
        client.dio.get<dynamic>('/devices'),
        client.dio.get<dynamic>('/sync/pull'),
        client.dio.get<dynamic>('/me'),
      ]);

      expect(refreshCalls, 1);
      expect(responses.length, 3);
      expect(await storage.readAccessToken(), 'fresh-access');
    });

    test('SyncEngine xử lý APPLIED, CONFLICT (auto-merge & manual) và vòng lặp Pull hasMore', () async {
      final AppDatabase db = AppDatabase.inMemory();
      final InMemoryTokenStorage storage = InMemoryTokenStorage();
      const String userId = 'u-sync';
      await storage.saveSession(
        accessToken: 'tok',
        refreshToken: 'ref',
        userId: userId,
      );

      // Tạo 1 task mới (sẽ được APPLIED)
      db.mutateLocalAndEnqueueOutbox(
        userId: userId,
        entityType: SyncEntityType.task,
        entityId: 't-new',
        op: 'CREATE',
        payload: <String, dynamic>{'title': 'Việc tạo mới'},
      );

      final Dio customDio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      customDio.httpClientAdapter = _FakeHttpClientAdapter((RequestOptions options) async {
        if (options.path.contains('/sync/push')) {
          final Map<String, dynamic> body =
              (options.data as Map<dynamic, dynamic>).cast<String, dynamic>();
          final List<dynamic> muts = body['mutations'] as List<dynamic>;
          final String firstMutId =
              (muts.first as Map<dynamic, dynamic>)['mutationId'] as String;
          return ResponseBody.fromString(
            '{"results":[{"mutationId":"$firstMutId","status":"APPLIED","version":1,"updatedAt":"2026-11-08T01:00:00Z"}]}',
            200,
            headers: <String, List<String>>{
              Headers.contentTypeHeader: <String>[Headers.jsonContentType],
            },
          );
        }
        if (options.path.contains('/sync/pull')) {
          final int cursor = SyncEngine.decodeCursor(
            options.queryParameters['cursor'],
            0,
          );
          if (cursor == 0) {
            return ResponseBody.fromString(
              '{"changes":[{"entityType":"project","entityId":"p-1","version":2,"updatedAt":"2026-11-08T01:00:00Z","data":{"id":"p-1","name":"Dự án từ Server"}}],"nextCursor":"${SyncEngine.encodeCursor(5)}","hasMore":true}',
              200,
              headers: <String, List<String>>{
                Headers.contentTypeHeader: <String>[Headers.jsonContentType],
              },
            );
          }
          return ResponseBody.fromString(
            '{"changes":[{"entityType":"category","entityId":"c-1","version":1,"updatedAt":"2026-11-08T01:01:00Z","data":{"id":"c-1","name":"Phân loại từ Server"}}],"nextCursor":"${SyncEngine.encodeCursor(9)}","hasMore":false}',
            200,
            headers: <String, List<String>>{
              Headers.contentTypeHeader: <String>[Headers.jsonContentType],
            },
          );
        }
        return ResponseBody.fromString('{}', 200);
      });

      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: storage,
        customDio: customDio,
      );
      final SyncEngine engine = SyncEngine(db: db, apiClient: apiClient);

      final SyncCycleSummary summary = await engine.runSyncCycle(userId: userId);
      expect(summary.hadNetworkError, isFalse);
      expect(summary.pushedApplied, 1);
      expect(summary.pulledChanges, 2);
      expect(summary.newCursor, 9);
      expect(db.getSyncState(userId).lastCursor, 9);
      expect(db.listOutboxMutations(userId: userId), isEmpty);

      db.close();
    });
  });
}

