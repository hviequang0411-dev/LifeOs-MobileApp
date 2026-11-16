import 'package:collection/collection.dart';
import 'entity_types.dart';

/// Kết quả hợp nhất 3 bên (`baseSnapshot`, `serverCurrent`, `clientLocal`)
/// khi Server trả về `status: CONFLICT` trong `POST /api/v1/sync/push`.
class ThreeWayMergeOutcome {
  const ThreeWayMergeOutcome({
    required this.autoMerged,
    required this.conflictingFields,
    required this.mergedPayload,
    required this.newBaseVersion,
  });

  final bool autoMerged;
  final List<String> conflictingFields;
  final Map<String, dynamic> mergedPayload;
  final int newBaseVersion;
}

/// Bộ hợp nhất xung đột 3 bên theo cấp trường (Field-Level 3-Way Merge)
/// tuân thủ `SYNC_RULES.md` §9.5, `MOBILE_MASTER_PLAN.md` §3.4 và khớp 100% với `merge-vectors.json`.
class ThreeWayMerger {
  const ThreeWayMerger._();

  static const Set<String> _metadataIgnoredFields = <String>{
    'version',
    'updatedAt',
    'createdAt',
    'deletedAt',
  };

  static const DeepCollectionEquality _deepEq = DeepCollectionEquality();

  static ThreeWayMergeOutcome merge({
    required SyncEntityType entityType,
    required Map<String, dynamic> baseSnapshot,
    required Map<String, dynamic> serverCurrent,
    required Map<String, dynamic> clientLocal,
  }) {
    final int newBaseVersion = (serverCurrent['version'] as num?)?.toInt() ?? 1;
    final Set<String> allFields = <String>{
      ...baseSnapshot.keys,
      ...serverCurrent.keys,
      ...clientLocal.keys,
    }..removeAll(_metadataIgnoredFields);

    final Map<String, dynamic> merged = <String, dynamic>{};
    final List<String> conflicts = <String>[];

    for (final String field in allFields) {
      final Object? baseVal = baseSnapshot[field];
      final Object? serverVal = serverCurrent[field];
      final Object? clientVal = clientLocal[field];

      final bool serverChanged = !_deepEq.equals(serverVal, baseVal);
      final bool clientChanged = !_deepEq.equals(clientVal, baseVal);

      if (!serverChanged && !clientChanged) {
        if (clientLocal.containsKey(field)) {
          merged[field] = clientVal;
        } else if (serverCurrent.containsKey(field)) {
          merged[field] = serverVal;
        }
      } else if (serverChanged && !clientChanged) {
        merged[field] = serverVal;
      } else if (!serverChanged && clientChanged) {
        merged[field] = clientVal;
      } else {
        // Cả hai bên cùng sửa trường này so với baseSnapshot
        if (_deepEq.equals(serverVal, clientVal)) {
          // Cả hai bên sửa thành cùng 1 giá trị giống nhau -> tự động hợp nhất
          merged[field] = clientVal;
        } else {
          // Xung đột thực sự trên trường `field`
          conflicts.add(field);
          // Mặc định giữ giá trị phía client trong bản nháp gộp để người dùng chọn trên Bottom Sheet
          merged[field] = clientVal;
        }
      }
    }

    // Với các thực thể tài chính (financeStrict), nếu có bất kỳ xung đột nào trên các trường tiền/tài khoản
    // hoặc ngay cả khi cần xác nhận tường minh, autoMerged chỉ true khi conflicts rỗng.
    final bool autoMerged = conflicts.isEmpty;

    return ThreeWayMergeOutcome(
      autoMerged: autoMerged,
      conflictingFields: conflicts,
      mergedPayload: merged,
      newBaseVersion: newBaseVersion,
    );
  }

  /// Tạo bản sao ghi chú khi cả 2 bên cùng sửa `content` của Note và người dùng chọn "Giữ cả hai thành bản sao".
  static Map<String, dynamic> createNoteConflictCopy({
    required String newNoteId,
    required Map<String, dynamic> clientNotePayload,
    required String conflictDateLabel,
  }) {
    final String originalTitle =
        (clientNotePayload['title'] as String? ?? 'Ghi chú').trim();
    return <String, dynamic>{
      ...clientNotePayload,
      'id': newNoteId,
      'title': '$originalTitle (Bản sao xung đột $conflictDateLabel)',
    };
  }
}

