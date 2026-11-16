import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/database/app_database.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/network/api_client.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/sync/sync_engine.dart';
import 'package:lifeos_mobile/features/auth/data/online_account_service.dart';
import 'package:lifeos_mobile/features/workspace/data/lifeos_local_repository.dart';

/// Harness giao thức in-memory mô phỏng đúng schema của `docs/api/openapi.yaml`
/// và `docs/engineering/SYNC_PROTOCOL.md` (KHÔNG phải backend thật).
/// Mọi response đều tuân thủ chặt chẽ OpenAPI — không có trường tự bịa.
class _StatefulProtocolServerHarness implements HttpClientAdapter {
  int _seq = 0;
  final Map<String, Map<String, dynamic>> _usersByEmail =
      <String, Map<String, dynamic>>{};
  final Map<String, String> _verificationTokens = <String, String>{};
  final Map<String, String> _deletionCodes = <String, String>{};
  final Map<String, Map<String, Map<String, dynamic>>> _appliedMutations =
      <String, Map<String, Map<String, dynamic>>>{};
  final Map<String, List<Map<String, dynamic>>> _changeLogs =
      <String, List<Map<String, dynamic>>>{};
  final Map<String, Map<String, Map<String, dynamic>>> _entities =
      <String, Map<String, Map<String, dynamic>>>{};

  bool networkOffline = false;

  /// Mô phỏng việc đọc email xác minh từ hộp thư (tương đương Mailpit ở môi trường thật),
  /// vì `POST /api/v1/auth/register` trả HTTP 202 với body rỗng theo `openapi.yaml` dòng 408.
  String? latestVerificationTokenForEmail(String email) {
    for (final MapEntry<String, String> entry in _verificationTokens.entries) {
      if (entry.value == email) {
        return entry.key;
      }
    }
    return null;
  }

  /// Mô phỏng việc đọc mã 6 chữ số xóa tài khoản từ hộp thư,
  /// vì `POST /api/v1/account/deletion/code` trả HTTP 202 với body rỗng theo `openapi.yaml` dòng 72.
  String? latestDeletionCodeForUser(String userId) => _deletionCodes[userId];

  Map<String, dynamic>? getServerEntity(
    String userId,
    String entityType,
    String id,
  ) {
    return _entities[userId]?['$entityType:$id'];
  }

  void seedExternalServerUpdate({
    required String userId,
    required String entityType,
    required String entityId,
    required String operation,
    required Map<String, dynamic> data,
  }) {
    final Map<String, Map<String, dynamic>> map = _entities.putIfAbsent(
      userId,
      () => <String, Map<String, dynamic>>{},
    );
    final String key = '$entityType:$entityId';
    final int currentVersion = (map[key]?['version'] as int?) ?? 0;
    final int newVersion = currentVersion + 1;
    final Map<String, dynamic> record = <String, dynamic>{
      ...data,
      'id': entityId,
      'version': newVersion,
    };
    map[key] = record;
    _seq += 1;
    _changeLogs.putIfAbsent(userId, () => <Map<String, dynamic>>[]).add(
      <String, dynamic>{
        'seq': _seq,
        'entityType': entityType,
        'entityId': entityId,
        'operation': operation,
        'version': newVersion,
        'data': operation == 'DELETE' ? null : record,
      },
    );
  }

  @override
  void close({bool force = false}) {}

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (networkOffline) {
      throw DioException.connectionError(
        requestOptions: options,
        reason: 'Simulating airplane/offline mode',
      );
    }

    final String path = options.uri.path;
    final String method = options.method.toUpperCase();
    final Map<String, dynamic> body = options.data is Map
        ? Map<String, dynamic>.from(options.data as Map<dynamic, dynamic>)
        : <String, dynamic>{};

    // 1. POST /api/v1/auth/register -> 202 empty body (openapi.yaml lines 396-416)
    if (method == 'POST' && path.endsWith('/auth/register')) {
      final String email = (body['email'] as String).trim().toLowerCase();
      final String userId = 'usr-${_usersByEmail.length + 1}';
      final String verifyToken = 'vtok-$userId';
      _usersByEmail[email] = <String, dynamic>{
        'id': userId,
        'email': email,
        'displayName': body['displayName'] ?? 'Người dùng',
        'password': body['password'],
        'timezone': body['timezone'] ?? 'Asia/Ho_Chi_Minh',
        'verified': false,
        'deletionScheduledAt': null,
      };
      _verificationTokens[verifyToken] = email;
      return _empty(202);
    }

    // 2. POST /api/v1/auth/verify-email -> 204 empty body (openapi.yaml lines 459-479)
    if (method == 'POST' && path.endsWith('/auth/verify-email')) {
      final String? token = body['token'] as String?;
      final String? email = token == null ? null : _verificationTokens[token];
      if (email == null) {
        return _json(400, <String, dynamic>{
          'errorCode': 'AUTH_VERIFICATION_TOKEN_INVALID',
          'message': 'Token không hợp lệ',
        });
      }
      final Map<String, dynamic> user = _usersByEmail[email]!;
      user['verified'] = true;
      final String userId = user['id'] as String;
      // Seed default user_settings (SettingsResponse schema: openapi.yaml lines 8175-8201)
      seedExternalServerUpdate(
        userId: userId,
        entityType: 'user_settings',
        entityId: userId,
        operation: 'UPSERT',
        data: <String, dynamic>{
          'id': userId,
          'timezone': user['timezone'],
          'locale': 'vi',
          'defaultCurrency': 'VND',
          'weekStartDay': 1,
          'quietStart': null,
          'quietEnd': null,
          'showDetailsOnLockScreen': false,
          'updatedAt': '2026-11-08T00:00:00Z',
        },
      );
      // Seed system adjustment category per D112 (FinancialCategorySyncData: openapi.yaml lines 6699-6724)
      seedExternalServerUpdate(
        userId: userId,
        entityType: 'financial_category',
        entityId: 'fcat-adjust-$userId',
        operation: 'UPSERT',
        data: <String, dynamic>{
          'id': 'fcat-adjust-$userId',
          'name': 'Điều chỉnh số dư',
          'type': 'EXPENSE',
          'parentId': null,
          'icon': 'tune',
          'color': '#2D6A4F',
          'sortOrder': 0,
          'systemKey': 'ADJUSTMENT',
        },
      );
      return _empty(204);
    }

    // 3. POST /api/v1/auth/login -> 200 LoginResponse (openapi.yaml lines 7262-7278)
    if (method == 'POST' && path.endsWith('/auth/login')) {
      final String email = (body['email'] as String).trim().toLowerCase();
      final Map<String, dynamic>? user = _usersByEmail[email];
      if (user == null || user['password'] != body['password']) {
        return _json(401, <String, dynamic>{
          'errorCode': 'AUTH_INVALID_CREDENTIALS',
          'message': 'Sai thông tin đăng nhập',
        });
      }
      final String userId = user['id'] as String;
      final String deviceId = (body['deviceId'] as String?) ?? 'dev-$userId-1';
      return _json(200, <String, dynamic>{
        'accessToken': 'at-$userId',
        'refreshToken': 'rt-$userId',
        'tokenType': 'Bearer',
        'expiresIn': 900,
        'deviceId': deviceId,
        'user': <String, dynamic>{
          'id': userId,
          'email': email,
          'displayName': user['displayName'],
        },
      });
    }

    final String? authHeader = options.headers['Authorization'] as String?;
    final String userId =
        (authHeader != null && authHeader.startsWith('Bearer at-'))
            ? authHeader.substring('Bearer at-'.length)
            : 'usr-1';

    // 4. GET /api/v1/me/settings -> 200 SettingsResponse (openapi.yaml lines 8175-8201)
    if (method == 'GET' && path.endsWith('/me/settings')) {
      final Map<String, dynamic> settings =
          getServerEntity(userId, 'user_settings', userId) ??
              <String, dynamic>{
                'id': userId,
                'timezone': 'Asia/Ho_Chi_Minh',
                'locale': 'vi',
                'defaultCurrency': 'VND',
                'weekStartDay': 1,
                'quietStart': null,
                'quietEnd': null,
                'showDetailsOnLockScreen': false,
                'updatedAt': '2026-11-08T00:00:00Z',
                'version': 1,
              };
      return _json(200, settings);
    }

    // 5. POST /api/v1/sync/push -> 200 PushResponse (openapi.yaml lines 7860-7866)
    if (method == 'POST' && path.endsWith('/sync/push')) {
      final List<Map<String, dynamic>> mutations =
          (body['mutations'] as List<dynamic>)
              .map((dynamic e) =>
                  Map<String, dynamic>.from(e as Map<dynamic, dynamic>))
              .toList();
      final Map<String, Map<String, dynamic>> userApplied =
          _appliedMutations.putIfAbsent(
        userId,
        () => <String, Map<String, dynamic>>{},
      );
      final Map<String, Map<String, dynamic>> userEntities =
          _entities.putIfAbsent(
        userId,
        () => <String, Map<String, dynamic>>{},
      );
      final List<Map<String, dynamic>> results = <Map<String, dynamic>>[];

      for (final Map<String, dynamic> m in mutations) {
        final String mutationId = m['mutationId'] as String;
        if (userApplied.containsKey(mutationId)) {
          final Map<String, dynamic> prev = userApplied[mutationId]!;
          results.add(<String, dynamic>{
            ...prev,
            'status': 'ALREADY_APPLIED',
            'originalStatus': prev['status'],
          });
          continue;
        }

        final String entityType = m['entityType'] as String;
        final String entityId = m['entityId'] as String;
        final String op = (m['operation'] ?? m['op']) as String;
        final int? baseVersion = (m['baseVersion'] as num?)?.toInt();
        final Map<String, dynamic> data = m['data'] is Map
            ? Map<String, dynamic>.from(m['data'] as Map<dynamic, dynamic>)
            : (m['payload'] is Map
                ? Map<String, dynamic>.from(
                    m['payload'] as Map<dynamic, dynamic>)
                : <String, dynamic>{});

        final String key = '$entityType:$entityId';
        final Map<String, dynamic>? existing = userEntities[key];
        final int currentVersion = (existing?['version'] as int?) ?? 0;

        if ((op == 'UPDATE' || op == 'DELETE') &&
            existing != null &&
            baseVersion != null &&
            baseVersion != currentVersion) {
          final Map<String, dynamic> conflictRes = <String, dynamic>{
            'mutationId': mutationId,
            'status': 'CONFLICT',
            'entityVersion': currentVersion,
            'errorCode': 'SYNC_VERSION_CONFLICT',
            'conflictReason': 'VERSION_MISMATCH',
            'message': 'Phiên bản trên máy chủ đã thay đổi',
            'serverSnapshot': existing,
            'serverCurrent': existing,
            'originalStatus': null,
            'fieldErrors': null,
          };
          userApplied[mutationId] = conflictRes;
          results.add(conflictRes);
          continue;
        }

        final int newVersion = currentVersion + 1;
        final Map<String, dynamic> merged = <String, dynamic>{
          if (existing != null) ...existing,
          ...data,
          'id': entityId,
          'version': newVersion,
          if (op == 'DELETE') 'deletedAt': '2026-11-08T01:00:00Z',
        };
        userEntities[key] = merged;
        _seq += 1;
        _changeLogs.putIfAbsent(userId, () => <Map<String, dynamic>>[]).add(
          <String, dynamic>{
            'seq': _seq,
            'entityType': entityType,
            'entityId': entityId,
            'operation': op == 'DELETE' ? 'DELETE' : 'UPSERT',
            'version': newVersion,
            'data': op == 'DELETE' ? null : merged,
          },
        );

        final Map<String, dynamic> appliedRes = <String, dynamic>{
          'mutationId': mutationId,
          'status': 'APPLIED',
          'version': newVersion,
          'entityVersion': newVersion,
          'errorCode': null,
          'message': null,
          'conflictReason': null,
          'serverSnapshot': null,
          'originalStatus': null,
          'fieldErrors': null,
        };
        userApplied[mutationId] = appliedRes;
        results.add(appliedRes);
      }

      return _json(200, <String, dynamic>{'results': results});
    }

    // 6. GET /api/v1/sync/pull -> 200 PullResponse (openapi.yaml lines 7844-7854)
    if (method == 'GET' && path.endsWith('/sync/pull')) {
      final int cursor = SyncEngine.decodeCursor(
        options.uri.queryParameters['cursor'],
        0,
      );
      final int limit =
          int.tryParse(options.uri.queryParameters['limit'] ?? '100') ?? 100;
      final List<Map<String, dynamic>> allLogs =
          _changeLogs[userId] ?? const <Map<String, dynamic>>[];

      final List<Map<String, dynamic>> filteredBySeq = allLogs
          .where((Map<String, dynamic> log) => (log['seq'] as int) > cursor)
          .toList();
      final List<Map<String, dynamic>> page =
          filteredBySeq.take(limit).toList();
      final bool hasMore = filteredBySeq.length > limit;
      final int nextCursorSeq =
          page.isEmpty ? cursor : (page.last['seq'] as int);
      final String nextCursorEncoded = SyncEngine.encodeCursor(nextCursorSeq);

      final Map<String, Map<String, dynamic>> latestByEntity =
          <String, Map<String, dynamic>>{};
      for (final Map<String, dynamic> item in page) {
        final String k = '${item['entityType']}:${item['entityId']}';
        latestByEntity[k] = <String, dynamic>{
          'entityType': item['entityType'],
          'entityId': item['entityId'],
          'operation': item['operation'],
          'version': item['version'],
          'data': item['data'],
        };
      }

      return _json(200, <String, dynamic>{
        'changes': latestByEntity.values.toList(),
        'nextCursor': nextCursorEncoded,
        'hasMore': hasMore,
      });
    }

    // 7. Account export endpoints (ExportResponse: openapi.yaml lines 6665-6685)
    if (method == 'POST' && path.endsWith('/account/export')) {
      return _json(202, <String, dynamic>{
        'id': 'exp-100',
        'status': 'COMPLETED',
        'requestedAt': '2026-11-08T01:00:00Z',
        'completedAt': '2026-11-08T01:00:01Z',
        'expiresAt': '2026-11-09T01:00:00Z',
      });
    }
    if (method == 'GET' && path.endsWith('/account/export/exp-100')) {
      return _json(200, <String, dynamic>{
        'id': 'exp-100',
        'status': 'COMPLETED',
        'requestedAt': '2026-11-08T01:00:00Z',
        'completedAt': '2026-11-08T01:00:01Z',
        'expiresAt': '2026-11-09T01:00:00Z',
      });
    }
    if (method == 'GET' && path.endsWith('/account/export/exp-100/download')) {
      final Uint8List payloadBytes = Uint8List.fromList(
        utf8.encode('PK\x03\x04-LIFEOS-EXPORT-ZIP'),
      );
      return ResponseBody.fromBytes(
        payloadBytes,
        200,
        headers: <String, List<String>>{
          Headers.contentTypeHeader: <String>['application/zip'],
        },
      );
    }

    // 8. Account deletion endpoints (openapi.yaml lines 29-77, 5710-5727)
    if (method == 'POST' && path.endsWith('/account/deletion/code')) {
      _deletionCodes[userId] = '654321';
      return _empty(202);
    }
    if (method == 'POST' && path.endsWith('/account/deletion')) {
      if (body['code'] != _deletionCodes[userId]) {
        return _json(400, <String, dynamic>{
          'errorCode': 'ACCOUNT_DELETION_CODE_INVALID',
          'message': 'Mã xác nhận không đúng',
        });
      }
      return _json(200, <String, dynamic>{
        'cancellable': true,
        'deletionScheduledAt': '2026-12-08T01:00:00Z',
      });
    }
    if (method == 'DELETE' && path.endsWith('/account/deletion')) {
      return _empty(204);
    }

    return _json(404, <String, dynamic>{
      'errorCode': 'NOT_FOUND',
      'message': 'Không tìm thấy endpoint $method $path',
    });
  }

  ResponseBody _empty(int status) {
    return ResponseBody.fromString(
      '',
      status,
      headers: <String, List<String>>{},
    );
  }

  ResponseBody _json(int status, Map<String, dynamic> data) {
    return ResponseBody.fromString(
      jsonEncode(data),
      status,
      headers: <String, List<String>>{
        Headers.contentTypeHeader: <String>[Headers.jsonContentType],
      },
    );
  }
}

void main() {
  test(
    'Protocol Harness E2E (9 luồng a-i khớp schema docs/api/openapi.yaml trên server giả in-memory)',
    () async {
      final _StatefulProtocolServerHarness server =
          _StatefulProtocolServerHarness();
      final AppDatabase db = AppDatabase.inMemory();
      addTearDown(db.close);

      final InMemoryTokenStorage tokenStorage = InMemoryTokenStorage();
      final Dio dio =
          Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      dio.httpClientAdapter = server;

      final LifeOsApiClient apiClient = LifeOsApiClient(
        baseUrl: 'http://localhost:8080/api/v1',
        tokenStorage: tokenStorage,
        customDio: dio,
      );
      final OnlineAccountService accountService =
          OnlineAccountService(apiClient: apiClient);
      final SyncEngine syncEngine = SyncEngine(db: db, apiClient: apiClient);
      final LifeOsLocalRepository repo = LifeOsLocalRepository(db: db);

      // (a) Đăng ký -> đọc token từ hộp thư mô phỏng -> verify-email -> đăng nhập -> pull user_settings
      await accountService.register(
        email: 'minh@example.com',
        password: 'Password123!',
        displayName: 'Minh Nguyễn',
      );
      final String verifyToken = server.latestVerificationTokenForEmail(
        'minh@example.com',
      )!;
      await accountService.verifyEmail(token: verifyToken);
      final ({
        String? deviceId,
        String displayName,
        String email,
        String userId
      }) session = await accountService.login(
        email: 'minh@example.com',
        password: 'Password123!',
        deviceName: 'Pixel 8 Pro',
        platform: 'ANDROID',
      );
      final String userId = session.userId;
      expect(userId, 'usr-1');

      final SyncCycleSummary initialSync =
          await syncEngine.runSyncCycle(userId: userId);
      expect(initialSync.hadNetworkError, isFalse);
      final Map<String, dynamic> settings = repo.getUserSettings(userId);
      expect(settings['defaultCurrency'], 'VND');
      expect(settings['weekStartDay'], 1);
      expect(settings.containsKey('dayStartHour'), isFalse);

      // (b) Bật chế độ máy bay -> tạo project + 3 task + habit + check-in + giao dịch -> tắt máy bay -> sync
      server.networkOffline = true;
      final String projId = repo.createProject(
        userId: userId,
        name: 'Ra mắt LifeOS Mobile',
        color: '#2D6A4F',
      );
      final List<String> taskIds = <String>[];
      for (int i = 1; i <= 3; i++) {
        taskIds.add(
          repo.createTask(
            userId: userId,
            title: 'Công việc ưu tiên #$i',
            projectId: projId,
            dueDate: '2026-11-08',
          ),
        );
      }
      final String habitId = repo.createHabit(
        userId: userId,
        name: 'Đọc sách 30 phút',
        frequencyType: 'DAILY',
      );
      repo.checkInHabit(
        userId: userId,
        habitId: habitId,
        entryDateIso: '2026-11-08',
        status: 'DONE',
      );
      final String cashAccId = repo.createFinanceAccount(
        userId: userId,
        name: 'Ví tiền mặt',
        accountType: 'CASH',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(2000000),
      );
      repo.createFinanceTransaction(
        userId: userId,
        accountId: cashAccId,
        kind: 'EXPENSE',
        amount: Decimal10k.fromWhole(65000),
        occurredOnIso: '2026-11-08',
        note: 'Bún bò Huế',
      );

      final SyncCycleSummary failedOfflineSync =
          await syncEngine.runSyncCycle(userId: userId);
      expect(failedOfflineSync.hadNetworkError, isTrue);
      expect(
        db.listOutboxMutations(userId: userId).length,
        greaterThanOrEqualTo(7),
      );

      server.networkOffline = false;
      final SyncCycleSummary onlineSync =
          await syncEngine.runSyncCycle(userId: userId);
      expect(onlineSync.hadNetworkError, isFalse);
      expect(db.listOutboxMutations(userId: userId), isEmpty);
      expect(
        server.getServerEntity(userId, 'project', projId)?['name'],
        'Ra mắt LifeOS Mobile',
      );

      // (c) Hoàn thành task -> sync
      repo.toggleTaskCompletion(userId: userId, taskId: taskIds.first);
      await syncEngine.runSyncCycle(userId: userId);
      expect(
        server.getServerEntity(userId, 'task', taskIds.first)?['status'],
        'DONE',
      );

      // (d) Chuyển khoản (account_transfer) + điều chỉnh số dư (reconcile) -> đối chiếu số dư
      final String bankAccId = repo.createFinanceAccount(
        userId: userId,
        name: 'Tài khoản ngân hàng',
        accountType: 'BANK',
        currency: 'VND',
        openingBalance: Decimal10k.fromWhole(5000000),
      );
      final String trfId = repo.createAccountTransfer(
        userId: userId,
        fromAccountId: bankAccId,
        toAccountId: cashAccId,
        amount: Decimal10k.fromWhole(500000),
        occurredOnIso: '2026-11-08',
        note: 'Rút tiền mặt ATM',
      );
      expect(
        repo.computeAccountBalance(userId: userId, accountId: bankAccId),
        Decimal10k.fromWhole(4500000),
      );
      expect(
        repo.computeAccountBalance(userId: userId, accountId: cashAccId),
        Decimal10k.fromWhole(2435000),
      );

      repo.reconcileAccountBalance(
        userId: userId,
        accountId: cashAccId,
        actualBalance: Decimal10k.fromWhole(2500000),
        occurredOnIso: '2026-11-08',
      );
      expect(
        repo.computeAccountBalance(userId: userId, accountId: cashAccId),
        Decimal10k.fromWhole(2500000),
      );
      await syncEngine.runSyncCycle(userId: userId);
      expect(
        server.getServerEntity(userId, 'account_transfer', trfId)?['amount'],
        '500000',
      );

      // (e) Sự kiện lặp -> sửa 1 kỳ (event_exception) -> sửa từ kỳ này trở đi (split) -> sync
      final String evtId = repo.createCalendarEvent(
        userId: userId,
        title: 'Họp kiến trúc',
        startLocal: '2026-11-02T09:00:00',
        endLocal: '2026-11-02T10:00:00',
        timezone: 'Asia/Ho_Chi_Minh',
        recurrence: const <String, dynamic>{
          'freq': 'WEEKLY',
          'interval': 1,
          'byWeekday': <int>[1],
          'byMonthDay': <int>[],
          'until': null,
          'count': null,
        },
      );
      repo.recordEventOccurrenceException(
        userId: userId,
        eventId: evtId,
        originalStartLocal: '2026-11-09T09:00:00',
        kind: 'CANCELLED',
      );
      final String newSeriesId = repo.splitRecurringEventFrom(
        userId: userId,
        originalEventId: evtId,
        originalStartLocal: '2026-11-16T09:00:00',
        newStartLocal: '2026-11-16T10:00:00',
        newEndLocal: '2026-11-16T11:00:00',
        newTitle: 'Họp kiến trúc (khung giờ mới)',
      );
      await syncEngine.runSyncCycle(userId: userId);
      expect(
        (server.getServerEntity(userId, 'calendar_event', evtId)?['recurrence']
            as Map<dynamic, dynamic>)['until'],
        '2026-11-15',
      );
      expect(
        server.getServerEntity(
          userId,
          'calendar_event',
          newSeriesId,
        )?['title'],
        'Họp kiến trúc (khung giờ mới)',
      );

      // (f) Note Markdown + linkNoteToTarget (TASK / GOAL theo D189) + Goal/Milestone + Weekly Review
      final String goalId = repo.createGoal(
        userId: userId,
        title: 'Hoàn thiện LifeOS v1',
        category: 'CAREER',
        startDateIso: '2026-11-01',
        targetDateIso: '2026-12-31',
        milestoneTitles: const <String>['Xong 15 gói PWP Mobile'],
      );
      final String noteId = repo.saveNote(
        userId: userId,
        title: 'Kế hoạch kiểm thử',
        content: 'Kiểm tra kỹ đồng bộ offline-first và hiệu năng.',
      );
      final String nlId = repo.linkNoteToTarget(
        userId: userId,
        noteId: noteId,
        targetType: 'GOAL',
        targetId: goalId,
      );
      final String revId = repo.saveLifeReview(
        userId: userId,
        periodType: 'WEEK',
        periodKey: '2026-11-02',
        reflection: 'Kiến trúc rõ ràng, test xanh',
        mood: 5,
      );
      await syncEngine.runSyncCycle(userId: userId);
      expect(
        server.getServerEntity(userId, 'note_link', nlId)?['targetType'],
        'GOAL',
      );
      expect(
        server.getServerEntity(userId, 'review', revId)?['periodKey'],
        '2026-11-02',
      );

      // (g) Sửa cùng một trường trên cùng một task ở server + ở mobile -> CONFLICT -> chọn giữ bản local -> push lại
      final String conflictTaskId = taskIds[1];
      server.seedExternalServerUpdate(
        userId: userId,
        entityType: 'task',
        entityId: conflictTaskId,
        operation: 'UPSERT',
        data: <String, dynamic>{
          'title': 'Công việc #2 (sửa từ thiết bị khác)',
          'projectId': projId,
          'status': 'TODO',
          'priority': 'HIGH',
          'dueDate': '2026-11-08',
        },
      );
      repo.updateTask(
        userId: userId,
        taskId: conflictTaskId,
        patch: const <String, dynamic>{
          'title': 'Công việc #2 (bản ưu tiên trên điện thoại)',
        },
      );
      await syncEngine.runSyncCycle(userId: userId);
      final List<OutboxMutationRecord> conflicts = db.listOutboxMutations(
        userId: userId,
        statuses: const <String>['NEEDS_USER_ACTION'],
      );
      expect(conflicts, hasLength(1));

      syncEngine.resolveConflictByUser(
        userId: userId,
        conflictMutation: conflicts.first,
        chosenPayload: <String, dynamic>{
          ...conflicts.first.payload,
          'title': 'Công việc #2 (bản ưu tiên trên điện thoại)',
        },
        serverVersion: 2,
      );
      await syncEngine.runSyncCycle(userId: userId);
      expect(
        db.listOutboxMutations(
          userId: userId,
          statuses: const <String>['NEEDS_USER_ACTION'],
        ),
        isEmpty,
      );
      expect(
        server.getServerEntity(userId, 'task', conflictTaskId)?['title'],
        'Công việc #2 (bản ưu tiên trên điện thoại)',
      );

      // (h) Xuất dữ liệu tài khoản -> tải zip
      final Map<String, dynamic> exportJob =
          await accountService.requestDataExport();
      final Map<String, dynamic> exportStatus =
          await accountService.getDataExportStatus(
        exportJob['id'] as String,
      );
      expect(exportStatus['status'], 'COMPLETED');
      final Uint8List zipBytes = await accountService.downloadDataExportZip(
        exportJob['id'] as String,
      );
      expect(
        utf8.decode(zipBytes),
        startsWith('PK\x03\x04-LIFEOS-EXPORT-ZIP'),
      );

      // (i) Yêu cầu xóa tài khoản -> xác nhận mã 6 số -> hủy yêu cầu xóa
      await accountService.requestAccountDeletionCode();
      final String delCode = server.latestDeletionCodeForUser(userId)!;
      final Map<String, dynamic> delResp =
          await accountService.confirmAccountDeletion(
        sixDigitCode: delCode,
        password: 'Password123!',
      );
      expect(delResp['cancellable'], isTrue);
      expect(delResp['deletionScheduledAt'], '2026-12-08T01:00:00Z');
      await accountService.cancelAccountDeletion();

      // (j) Đảm bảo thực thể vẫn được lưu trong SQLite
      expect(
        db.getEntity(
          userId: userId,
          entityType: SyncEntityType.task,
          entityId: conflictTaskId,
        ),
        isNotNull,
      );
    },
  );
}
