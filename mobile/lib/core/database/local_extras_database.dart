// LOCAL_ONLY: Kho cơ sở dữ liệu mở rộng lưu cục bộ trên máy, chưa có backend.
// Tuyệt đối không có cột sync_state, không ghi vào outbox, không đồng bộ qua SyncEngine.
import 'dart:async';

import 'sqlite_runtime.dart';

/// Bản ghi đánh giá Bánh xe cuộc sống (8 khía cạnh từ 1 đến 10).
class LocalLifeWheelAssessment {
  const LocalLifeWheelAssessment({
    required this.id,
    required this.recordedAt,
    required this.career,
    required this.finance,
    required this.health,
    required this.relationships,
    required this.growth,
    required this.family,
    required this.leisure,
    required this.environment,
    this.notes,
  });

  factory LocalLifeWheelAssessment.fromRow(Map<String, dynamic> row) {
    return LocalLifeWheelAssessment(
      id: row['id'] as String,
      recordedAt: DateTime.fromMillisecondsSinceEpoch(row['recorded_at'] as int, isUtc: true),
      career: row['career'] as int,
      finance: row['finance'] as int,
      health: row['health'] as int,
      relationships: row['relationships'] as int,
      growth: row['growth'] as int,
      family: row['family'] as int,
      leisure: row['leisure'] as int,
      environment: row['environment'] as int,
      notes: row['notes'] as String?,
    );
  }

  final String id;
  final DateTime recordedAt;
  final int career;
  final int finance;
  final int health;
  final int relationships;
  final int growth;
  final int family;
  final int leisure;
  final int environment;
  final String? notes;

  double get averageScore =>
      (career + finance + health + relationships + growth + family + leisure + environment) / 8.0;

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'id': id,
      'recorded_at': recordedAt.toUtc().millisecondsSinceEpoch,
      'career': career,
      'finance': finance,
      'health': health,
      'relationships': relationships,
      'growth': growth,
      'family': family,
      'leisure': leisure,
      'environment': environment,
      'notes': notes,
    };
  }
}

/// Bản ghi phiên tập trung Pomodoro / Focus Garden (Khu vườn tập trung).
class LocalFocusSession {
  const LocalFocusSession({
    required this.id,
    this.taskId,
    required this.startedAt,
    required this.durationMinutes,
    required this.completed,
    this.tag,
  });

  factory LocalFocusSession.fromRow(Map<String, dynamic> row) {
    return LocalFocusSession(
      id: row['id'] as String,
      taskId: row['task_id'] as String?,
      startedAt: DateTime.fromMillisecondsSinceEpoch(row['started_at'] as int, isUtc: true),
      durationMinutes: row['duration_minutes'] as int,
      completed: (row['completed'] as int) == 1,
      tag: row['tag'] as String?,
    );
  }

  final String id;
  final String? taskId;
  final DateTime startedAt;
  final int durationMinutes;
  final bool completed;
  final String? tag;

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'id': id,
      'task_id': taskId,
      'started_at': startedAt.toUtc().millisecondsSinceEpoch,
      'duration_minutes': durationMinutes,
      'completed': completed ? 1 : 0,
      'tag': tag,
    };
  }
}

/// Quản lý cơ sở dữ liệu SQLite phụ cô lập theo từng user (`lifeos_local_extras_<userId>.sqlite`).
class LifeOsLocalExtrasDatabase {
  LifeOsLocalExtrasDatabase._(this._rawDb);

  factory LifeOsLocalExtrasDatabase.inMemory() {
    final CommonDatabase raw = openInMemoryRawDatabase();
    _createTables(raw);
    return LifeOsLocalExtrasDatabase._(raw);
  }

  factory LifeOsLocalExtrasDatabase.openForUser(String userId, {String? baseDirectory}) {
    final CommonDatabase raw = openUserExtrasRawDatabase(
      userId,
      baseDirectory: baseDirectory,
    );
    _createTables(raw);
    return LifeOsLocalExtrasDatabase._(raw);
  }

  final CommonDatabase _rawDb;
  final StreamController<void> _changesController = StreamController<void>.broadcast();

  static void _createTables(CommonDatabase db) {
    db.execute('''
      CREATE TABLE IF NOT EXISTS local_life_wheel_assessments (
        id TEXT PRIMARY KEY,
        recorded_at INTEGER NOT NULL,
        career INTEGER NOT NULL,
        finance INTEGER NOT NULL,
        health INTEGER NOT NULL,
        relationships INTEGER NOT NULL,
        growth INTEGER NOT NULL,
        family INTEGER NOT NULL,
        leisure INTEGER NOT NULL,
        environment INTEGER NOT NULL,
        notes TEXT
      );

      CREATE TABLE IF NOT EXISTS local_focus_sessions (
        id TEXT PRIMARY KEY,
        task_id TEXT,
        started_at INTEGER NOT NULL,
        duration_minutes INTEGER NOT NULL,
        completed INTEGER NOT NULL,
        tag TEXT
      );

      CREATE TABLE IF NOT EXISTS local_device_prefs (
        pref_key TEXT PRIMARY KEY,
        pref_value TEXT NOT NULL
      );
    ''');
  }

  Stream<void> watchChanges() => _changesController.stream;

  // --- CRUD Bánh xe cuộc sống (Life Wheel) ---
  void insertLifeWheelAssessment(LocalLifeWheelAssessment assessment) {
    final CommonPreparedStatement stmt = _rawDb.prepare('''
      INSERT INTO local_life_wheel_assessments (
        id, recorded_at, career, finance, health, relationships, growth, family, leisure, environment, notes
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    ''');
    try {
      stmt.execute(<Object?>[
        assessment.id,
        assessment.recordedAt.toUtc().millisecondsSinceEpoch,
        assessment.career,
        assessment.finance,
        assessment.health,
        assessment.relationships,
        assessment.growth,
        assessment.family,
        assessment.leisure,
        assessment.environment,
        assessment.notes,
      ]);
      _changesController.add(null);
    } finally {
      stmt.dispose();
    }
  }

  List<LocalLifeWheelAssessment> getLifeWheelHistory() {
    final ResultSet rs = _rawDb.select(
      'SELECT * FROM local_life_wheel_assessments ORDER BY recorded_at DESC;',
    );
    return rs.map((Row row) => LocalLifeWheelAssessment.fromRow(row)).toList();
  }

  LocalLifeWheelAssessment? getLatestLifeWheelAssessment() {
    final List<LocalLifeWheelAssessment> list = getLifeWheelHistory();
    return list.isEmpty ? null : list.first;
  }

  // --- CRUD Khu vườn tập trung (Focus Garden) ---
  void insertFocusSession(LocalFocusSession session) {
    final CommonPreparedStatement stmt = _rawDb.prepare('''
      INSERT INTO local_focus_sessions (
        id, task_id, started_at, duration_minutes, completed, tag
      ) VALUES (?, ?, ?, ?, ?, ?);
    ''');
    try {
      stmt.execute(<Object?>[
        session.id,
        session.taskId,
        session.startedAt.toUtc().millisecondsSinceEpoch,
        session.durationMinutes,
        session.completed ? 1 : 0,
        session.tag,
      ]);
      _changesController.add(null);
    } finally {
      stmt.dispose();
    }
  }

  List<LocalFocusSession> getFocusSessions() {
    final ResultSet rs = _rawDb.select(
      'SELECT * FROM local_focus_sessions ORDER BY started_at DESC;',
    );
    return rs.map((Row row) => LocalFocusSession.fromRow(row)).toList();
  }

  int getTotalCompletedFocusMinutes() {
    final ResultSet rs = _rawDb.select(
      'SELECT SUM(duration_minutes) as total FROM local_focus_sessions WHERE completed = 1;',
    );
    if (rs.isEmpty) return 0;
    final Object? val = rs.first['total'];
    return val is int ? val : 0;
  }

  int getPlantedTreesCount() {
    final ResultSet rs = _rawDb.select(
      'SELECT COUNT(*) as count FROM local_focus_sessions WHERE completed = 1;',
    );
    if (rs.isEmpty) return 0;
    final Object? val = rs.first['count'];
    return val is int ? val : 0;
  }

  // --- Cài đặt cục bộ trên thiết bị (theme_mode, notification_prefs) ---
  String? getPref(String key) {
    final ResultSet rs = _rawDb.select(
      'SELECT pref_value FROM local_device_prefs WHERE pref_key = ? LIMIT 1;',
      <Object?>[key],
    );
    if (rs.isEmpty) return null;
    return rs.first['pref_value'] as String?;
  }

  void setPref(String key, String value) {
    final CommonPreparedStatement stmt = _rawDb.prepare('''
      INSERT INTO local_device_prefs (pref_key, pref_value)
      VALUES (?, ?)
      ON CONFLICT(pref_key) DO UPDATE SET pref_value = excluded.pref_value;
    ''');
    try {
      stmt.execute(<Object?>[key, value]);
      _changesController.add(null);
    } finally {
      stmt.dispose();
    }
  }

  void deletePref(String key) {
    final CommonPreparedStatement stmt = _rawDb.prepare(
      'DELETE FROM local_device_prefs WHERE pref_key = ?;',
    );
    try {
      stmt.execute(<Object?>[key]);
      _changesController.add(null);
    } finally {
      stmt.dispose();
    }
  }

  void clearAll() {
    _rawDb.execute('DELETE FROM local_life_wheel_assessments;');
    _rawDb.execute('DELETE FROM local_focus_sessions;');
    _rawDb.execute('DELETE FROM local_device_prefs;');
    _changesController.add(null);
  }

  void close() {
    _changesController.close();
    _rawDb.dispose();
  }
}

