// LOCAL_ONLY: Repository quản lý dữ liệu mở rộng lưu cục bộ trên máy, chưa có backend.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/local_extras_database.dart';
import '../../../core/providers/app_providers.dart';

export '../../../core/database/local_extras_database.dart';

/// Bật mở tệp SQLite cục bộ theo từng người dùng trong app thật (`main.dart`).
/// Mặc định `false` để các bài kiểm thử dùng kho trong bộ nhớ cô lập.
final Provider<bool> usePersistentLocalExtrasProvider =
    Provider<bool>((Ref ref) => false);

/// Thư mục lưu tệp SQLite cục bộ trên nền tảng Native (Mobile/Desktop).
final Provider<String?> localStorageBaseDirProvider =
    Provider<String?>((Ref ref) => null);

final Provider<LifeOsLocalExtrasDatabase> localExtrasDatabaseProvider =
    Provider<LifeOsLocalExtrasDatabase>((Ref ref) {
  final bool persistent = ref.watch(usePersistentLocalExtrasProvider);
  final String userId = ref.watch(currentUserIdProvider);
  final String? baseDir = ref.watch(localStorageBaseDirProvider);

  late final LifeOsLocalExtrasDatabase db;
  if (persistent) {
    try {
      db = LifeOsLocalExtrasDatabase.openForUser(
        userId,
        baseDirectory: baseDir,
      );
    } catch (_) {
      db = LifeOsLocalExtrasDatabase.inMemory();
    }
  } else {
    db = LifeOsLocalExtrasDatabase.inMemory();
  }
  ref.onDispose(db.close);
  return db;
});

final Provider<LocalExtrasRepository> localExtrasRepositoryProvider =
    Provider<LocalExtrasRepository>((Ref ref) {
  final LifeOsLocalExtrasDatabase db = ref.watch(localExtrasDatabaseProvider);
  return LocalExtrasRepository(db);
});

class LocalExtrasRepository {
  const LocalExtrasRepository(this._db);

  final LifeOsLocalExtrasDatabase _db;

  Stream<void> watchChanges() => _db.watchChanges();

  // Bánh xe cuộc sống
  void recordLifeWheelAssessment(LocalLifeWheelAssessment assessment) {
    _db.insertLifeWheelAssessment(assessment);
  }

  List<LocalLifeWheelAssessment> getLifeWheelHistory() {
    return _db.getLifeWheelHistory();
  }

  LocalLifeWheelAssessment? getLatestLifeWheelAssessment() {
    return _db.getLatestLifeWheelAssessment();
  }

  // Khu vườn tập trung (Pomodoro / Focus Garden)
  void recordFocusSession(LocalFocusSession session) {
    _db.insertFocusSession(session);
  }

  List<LocalFocusSession> getFocusSessions() {
    return _db.getFocusSessions();
  }

  int getTotalFocusMinutes() {
    return _db.getTotalCompletedFocusMinutes();
  }

  int getPlantedTreesCount() {
    return _db.getPlantedTreesCount();
  }

  String? getPref(String key) => _db.getPref(key);

  void setPref(String key, String value) => _db.setPref(key, value);

  void deletePref(String key) => _db.deletePref(key);

  void clearAll() {
    _db.clearAll();
  }
}

