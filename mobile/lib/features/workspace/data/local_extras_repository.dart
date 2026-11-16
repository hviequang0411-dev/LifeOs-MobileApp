// LOCAL_ONLY: Repository quản lý dữ liệu mở rộng lưu cục bộ trên máy, chưa có backend.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/local_extras_database.dart';

export '../../../core/database/local_extras_database.dart';

final Provider<LifeOsLocalExtrasDatabase> localExtrasDatabaseProvider =
    Provider<LifeOsLocalExtrasDatabase>((Ref ref) {
  final LifeOsLocalExtrasDatabase db = LifeOsLocalExtrasDatabase.inMemory();
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

  void clearAll() {
    _db.clearAll();
  }
}

