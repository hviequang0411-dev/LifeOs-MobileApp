import 'package:uuid/uuid.dart';

import '../../../core/database/app_database.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/time/user_day_clock.dart';

/// Repository cục bộ cho miền **Calendar, Event Exception, Time Block & Reminder**
/// (`docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md` D120–D138 & `SYNC_PROTOCOL.md` §3.7).
///
/// Tuân thủ tuyệt đối:
/// - `calendar_event` (`D124`–`D126`, `SYNC_PROTOCOL.md` dòng 159, `openapi.yaml` dòng 6134–6164):
///   lưu `allDay`, `timezone` (IANA), `startLocal`/`endLocal` (`YYYY-MM-DDTHH:mm:ss`) và `recurrence`
///   dạng đối tượng JSON `{freq, interval, byWeekday, byMonthDay, until, count}` (không lưu chuỗi RRULE thô).
/// - `event_exception` (`D127`, `SYNC_PROTOCOL.md` dòng 160, `openapi.yaml` dòng 6165–6188):
///   `{eventId, originalStartLocal, kind ('CANCELLED'|'MODIFIED'), title, description, location, startLocal, endLocal}`.
class CalendarLocalRepository {
  CalendarLocalRepository({
    required this.db,
    Uuid? uuid,
  }) : _uuid = uuid ?? const Uuid();

  final AppDatabase db;
  final Uuid _uuid;

  static const List<String> _weekdayCodes = <String>[
    'MO',
    'TU',
    'WE',
    'TH',
    'FR',
    'SA',
    'SU',
  ];

  static Map<String, dynamic> _normalizeRecurrenceObject(
    Map<String, dynamic> raw,
  ) {
    final Map<String, dynamic> out = <String, dynamic>{
      'freq': (raw['freq'] as String? ?? 'DAILY').toUpperCase(),
      'interval': (raw['interval'] as num?)?.toInt() ?? 1,
    };
    final dynamic rawWeekdays = raw['byWeekday'];
    if (rawWeekdays is List<dynamic> && rawWeekdays.isNotEmpty) {
      out['byWeekday'] = rawWeekdays.map((dynamic w) {
        if (w is int && w >= 1 && w <= 7) {
          return _weekdayCodes[w - 1];
        }
        return w.toString().toUpperCase();
      }).toList();
    }
    final dynamic rawMonthDays = raw['byMonthDay'];
    if (rawMonthDays is List<dynamic> && rawMonthDays.isNotEmpty) {
      out['byMonthDay'] =
          rawMonthDays.map((dynamic m) => (m as num).toInt()).toList();
    }
    if (raw['until'] != null) {
      out['until'] = raw['until'];
    } else if (raw['count'] != null) {
      out['count'] = (raw['count'] as num).toInt();
    }
    return out;
  }

  /// Tạo sự kiện lịch (`calendar_event` — `D124`–`D126` & `openapi.yaml` `CreateEventRequest` dòng 6134–6164).
  String createCalendarEvent({
    required String userId,
    required String title,
    required String startLocal, // YYYY-MM-DDTHH:mm:ss
    required String endLocal, // YYYY-MM-DDTHH:mm:ss
    bool allDay = false,
    String? startDate,
    String? endDate,
    String timezone = 'Asia/Ho_Chi_Minh',
    Map<String, dynamic>? recurrence,
    String? description,
    String? location,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.calendarEvent,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'title': title.trim(),
        if (description != null) 'description': description.trim(),
        if (location != null) 'location': location.trim(),
        'allDay': allDay,
        'timezone': timezone,
        'startLocal': startLocal,
        'endLocal': endLocal,
        if (startDate != null) 'startDate': startDate,
        if (endDate != null) 'endDate': endDate,
        if (recurrence != null)
          'recurrence': _normalizeRecurrenceObject(recurrence),
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tạo ngoại lệ cho 1 lần xuất hiện của sự kiện lặp (`event_exception` — `D127` & `SYNC_PROTOCOL.md` §3.7 dòng 160).
  String recordEventOccurrenceException({
    required String userId,
    required String eventId,
    required String originalStartLocal, // YYYY-MM-DDTHH:mm:ss
    required String kind, // 'CANCELLED' | 'MODIFIED'
    String? startLocal,
    String? endLocal,
    String? title,
    String? description,
    String? location,
    DateTime? nowUtc,
  }) {
    final String normalizedKind = kind.toUpperCase();
    if (normalizedKind != 'CANCELLED' && normalizedKind != 'MODIFIED') {
      throw ArgumentError('kind của event_exception phải là CANCELLED hoặc MODIFIED: "$kind"');
    }
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.calendarEventException,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'eventId': eventId,
        'originalStartLocal': originalStartLocal,
        'kind': normalizedKind,
        if (title != null) 'title': title,
        if (description != null) 'description': description,
        if (location != null) 'location': location,
        if (startLocal != null) 'startLocal': startLocal,
        if (endLocal != null) 'endLocal': endLocal,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tách chuỗi sự kiện lặp ("Từ lần này trở đi" — `PHASE_4` UC-CAL-03 dòng 41 & `SYNC_PROTOCOL.md` §3.7 dòng 159):
  /// Đặt `recurrence.until` của sự kiện cũ bằng ngày ngay trước `originalStartLocal` (`YYYY-MM-DD`),
  /// đồng thời tạo `calendar_event` mới bắt đầu từ `newStartLocal`.
  String splitRecurringEventFrom({
    required String userId,
    required String originalEventId,
    required String originalStartLocal, // YYYY-MM-DDTHH:mm:ss
    required String newStartLocal,
    required String newEndLocal,
    required String newTitle,
    Map<String, dynamic>? newRecurrence,
    DateTime? nowUtc,
  }) {
    final LocalEntityRow? orig = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.calendarEvent,
      entityId: originalEventId,
    );
    if (orig == null) {
      throw StateError('Không tìm thấy sự kiện gốc: $originalEventId');
    }

    final String splitDateIso = originalStartLocal.split('T').first;
    final String untilDateIso = UserDayClock.addDays(splitDateIso, -1);

    final Map<String, dynamic> origRecurrence =
        (orig.payload['recurrence'] is Map<dynamic, dynamic>)
            ? (orig.payload['recurrence'] as Map<dynamic, dynamic>)
                .cast<String, dynamic>()
            : <String, dynamic>{'freq': 'DAILY', 'interval': 1};

    final Map<String, dynamic> truncatedRecurrence = _normalizeRecurrenceObject(
      <String, dynamic>{
        ...origRecurrence,
        'until': untilDateIso,
        'count': null,
      },
    );

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.calendarEvent,
      entityId: originalEventId,
      op: 'UPDATE',
      payload: <String, dynamic>{
        ...orig.payload,
        'recurrence': truncatedRecurrence,
      },
      nowUtc: nowUtc,
    );

    return createCalendarEvent(
      userId: userId,
      title: newTitle,
      startLocal: newStartLocal,
      endLocal: newEndLocal,
      allDay: (orig.payload['allDay'] as bool?) ?? false,
      timezone: (orig.payload['timezone'] as String?) ?? 'Asia/Ho_Chi_Minh',
      recurrence: newRecurrence ?? Map<String, dynamic>.from(origRecurrence),
      description: orig.payload['description'] as String?,
      location: orig.payload['location'] as String?,
      nowUtc: nowUtc,
    );
  }

  /// Tạo lời nhắc (`reminder` — `SYNC_PROTOCOL.md` §3.7 dòng 162 & `openapi.yaml` dòng 6333–6359).
  String createReminder({
    required String userId,
    required String targetType, // TASK | EVENT | HABIT | STANDALONE
    String? targetId,
    required String mode, // OFFSET | ABSOLUTE
    int? offsetMinutes,
    String? localDate,
    String? localTime,
    String? title,
    bool urgent = false,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.reminder,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'targetType': targetType.toUpperCase(),
        if (targetId != null) 'targetId': targetId,
        'mode': mode.toUpperCase(),
        if (offsetMinutes != null) 'offsetMinutes': offsetMinutes,
        if (localDate != null) 'localDate': localDate,
        if (localTime != null) 'localTime': localTime,
        if (title != null) 'title': title,
        'urgent': urgent,
        'enabled': true,
      },
      nowUtc: nowUtc,
    );
    return id;
  }
}

