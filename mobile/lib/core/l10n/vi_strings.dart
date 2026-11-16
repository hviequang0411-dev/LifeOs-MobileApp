/// Tập trung toàn bộ chuỗi tiếng Việt tự nhiên của ứng dụng LifeOS
/// (đồng bộ với `lib/l10n/app_vi.arb` để sẵn sàng mở rộng đa ngôn ngữ về sau).
class ViStrings {
  ViStrings._();

  // Điều hướng chính (5 tab gốc)
  static const String appTitle = 'LifeOS';
  static const String navToday = 'Hôm nay';
  static const String navPlan = 'Kế hoạch';
  static const String navFinance = 'Tài chính';
  static const String navKnowledge = 'Tri thức';
  static const String navOverview = 'Tổng quan';

  // Chuỗi tiêu đề & nút hành động dùng chung
  static const String todayFocusTitle = 'Trọng tâm hôm nay';
  static const String confirmDeleteTitle = 'Xác nhận xóa';
  static const String cancelAction = 'Hủy';
  static const String deleteAction = 'Xóa';
  static const String saveAction = 'Lưu';
  static const String logoutSafeTitle = 'Đăng xuất an toàn';

  // Trạng thái đồng bộ & cam kết không mất dữ liệu
  static const String savedLocallyBadge = 'Đã lưu trên máy';
  static const String syncSynced = 'Đã lưu trên máy · Đã đồng bộ';
  static const String syncSyncing = 'Đang đồng bộ...';
  static const String syncPaused = 'Đã tạm dừng đồng bộ';
  static String syncOfflinePending(int count) =>
      count > 0 ? 'Ngoại tuyến · $count thay đổi chờ đẩy' : 'Ngoại tuyến · Đã lưu trên máy';
  static String syncPendingCount(int count) => '$count thay đổi chờ đồng bộ';
  static String syncConflictCount(int count) => '$count mục cần giải quyết xung đột';

  // Quick Capture
  static const String quickCaptureTitle = 'Thêm nhanh';
  static const String quickTabTask = 'Việc cần làm';
  static const String quickTabFinance = 'Khoản thu/chi';
  static const String quickTabNote = 'Ghi chú';
  static const String draftAutoSaved = 'Đã tự động lưu nháp trên máy';

  // Mức ưu tiên công việc (SYNC_PROTOCOL.md §3.1: NONE | LOW | MEDIUM | HIGH)
  static const String priorityNone = 'Không';
  static const String priorityLow = 'Thấp';
  static const String priorityMedium = 'Trung bình';
  static const String priorityHigh = 'Cao';

  static String formatTaskPriority(String? priority) {
    return switch ((priority ?? 'MEDIUM').trim().toUpperCase()) {
      'HIGH' => priorityHigh,
      'MEDIUM' => priorityMedium,
      'LOW' => priorityLow,
      'NONE' => priorityNone,
      _ => priorityMedium,
    };
  }

  // Xác nhận xóa nguy hiểm (nêu rõ tên mục và hệ quả)
  static String confirmDeleteProjectMessage(String projectName, int taskCount) =>
      'Xóa dự án "$projectName" sẽ chuyển $taskCount công việc bên trong về Hộp thư đến (Inbox). Bạn có chắc chắn muốn xóa?';

  static String confirmDeleteCategoryMessage(String categoryName, int taskCount) =>
      'Xóa phân loại "$categoryName" sẽ gỡ nhãn phân loại khỏi $taskCount công việc liên quan. Tiếp tục xóa?';

  static String confirmDeleteFinanceAccountMessage(String accountName) =>
      'Xóa tài khoản "$accountName" sẽ lưu trữ ẩn tài khoản này cùng lịch sử giao dịch liên quan. Tiếp tục?';

  // Đăng xuất an toàn 3 lựa chọn (MOBILE_MASTER_PLAN §3.5)
  static const String logoutCleanTitle = 'Đăng xuất khỏi thiết bị';
  static const String logoutCleanSubtitle =
      'Toàn bộ dữ liệu trên máy đã được đồng bộ lên máy chủ. Xóa bộ nhớ đệm cục bộ và đăng xuất an toàn.';
  static String logoutDirtyTitle(int pendingCount) =>
      'Còn $pendingCount thay đổi chưa đồng bộ lên máy chủ';
  static const String logoutOptionSyncFirst = 'Đồng bộ ngay rồi đăng xuất (Khuyến nghị)';
  static const String logoutOptionExportBackup = 'Xuất tệp sao lưu JSON cục bộ rồi đăng xuất';
  static const String logoutOptionForceDiscard = 'Buộc đăng xuất và xóa thay đổi chưa đồng bộ';
  static const String logoutForceConfirmWord = 'XÓA';

  // Ánh xạ mã lỗi chuẩn từ Backend (`ErrorCode.java`) sang thông điệp tiếng Việt rõ nghĩa, không lộ mã kỹ thuật.
  static String mapBackendErrorCode(String? errorCode, {String? serverMessage}) {
    switch (errorCode) {
      case 'VALIDATION_ERROR':
      case 'VALIDATION_FAILED':
      case 'MALFORMED_REQUEST':
        return 'Thông tin nhập chưa hợp lệ. Vui lòng kiểm tra lại các trường được đánh dấu.';
      case 'INVALID_CREDENTIALS':
      case 'AUTH_INVALID_CREDENTIALS':
        return 'Email hoặc mật khẩu chưa chính xác. Vui lòng kiểm tra và thử lại.';
      case 'EMAIL_ALREADY_EXISTS':
      case 'ENTITY_ALREADY_EXISTS':
        return 'Địa chỉ email hoặc dữ liệu này đã tồn tại. Bạn có thể đăng nhập hoặc đặt lại mật khẩu.';
      case 'EMAIL_NOT_VERIFIED':
      case 'AUTH_EMAIL_NOT_VERIFIED':
        return 'Tài khoản chưa xác minh email. Vui lòng kiểm tra hộp thư để lấy mã xác minh và hoàn tất kích hoạt.';
      case 'TOKEN_EXPIRED':
      case 'UNAUTHORIZED':
      case 'AUTH_TOKEN_EXPIRED':
      case 'AUTH_TOKEN_INVALID':
        return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại (dữ liệu trên máy vẫn được giữ nguyên).';
      case 'REFRESH_TOKEN_REUSED':
      case 'REFRESH_TOKEN_INVALID':
      case 'AUTH_REFRESH_TOKEN_INVALID':
        return 'Phiên bảo mật trên thiết bị này không còn hiệu lực. Vui lòng đăng nhập lại.';
      case 'AUTH_VERIFICATION_TOKEN_INVALID':
        return 'Mã xác minh email không đúng hoặc đã hết hạn. Bạn có thể yêu cầu gửi lại mã mới.';
      case 'AUTH_RESET_TOKEN_INVALID':
        return 'Mã đặt lại mật khẩu không đúng hoặc đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.';
      case 'AUTH_PASSWORD_TOO_WEAK':
        return 'Mật khẩu cần tối thiểu 8 ký tự, đủ an toàn theo chính sách bảo mật.';
      case 'AUTH_CURRENT_PASSWORD_WRONG':
        return 'Mật khẩu hiện tại chưa chính xác. Vui lòng nhập lại.';
      case 'ACCOUNT_LOCKED':
      case 'RATE_LIMITED':
        return 'Bạn đã thao tác quá nhanh. Vui lòng đợi ít phút rồi thử lại.';
      case 'CLIENT_UPGRADE_REQUIRED':
        return 'Phiên bản ứng dụng đã cũ, vui lòng cập nhật lên bản mới nhất để tiếp tục đồng bộ.';
      case 'ACCOUNT_DELETION_PENDING':
        return 'Tài khoản đang trong thời gian chờ xóa (7 ngày). Hãy hủy yêu cầu xóa nếu bạn muốn tiếp tục sử dụng.';
      case 'INVALID_DELETION_CODE':
        return 'Mã xác nhận xóa tài khoản 6 chữ số không đúng hoặc đã hết hạn.';
      case 'VERSION_CONFLICT':
      case 'ENTITY_VERSION_CONFLICT':
      case 'CONFLICT':
        return 'Mục này vừa được cập nhật từ thiết bị khác. Hãy chọn phiên bản bạn muốn giữ lại.';
      case 'PAYLOAD_TOO_LARGE':
        return 'Nội dung vượt quá dung lượng cho phép (Ghi chú tối đa 192 KB, mục khác tối đa 64 KB).';
      case 'BATCH_LIMIT_EXCEEDED':
      case 'SYNC_BATCH_TOO_LARGE':
        return 'Số lượng mục đồng bộ vượt giới hạn cho phép; ứng dụng sẽ tự động chia nhỏ lô.';
      case 'MUTATION_TOO_OLD':
        return 'Thay đổi ngoại tuyến này đã quá 30 ngày nên không thể tự động áp dụng. Vui lòng xem lại và xác nhận.';
      case 'SYNC_FULL_RESYNC_REQUIRED':
      case 'SYNC_SNAPSHOT_EXPIRED':
        return 'Dữ liệu đồng bộ cần tải lại từ đầu để đảm bảo chính xác.';
      case 'RESOURCE_NOT_FOUND':
      case 'NOT_FOUND':
      case 'TASK_NOT_FOUND':
      case 'PROJECT_NOT_FOUND':
      case 'HABIT_NOT_FOUND':
      case 'ROUTINE_NOT_FOUND':
      case 'FINANCE_TRANSACTION_NOT_FOUND':
      case 'FINANCE_RECURRING_NOT_FOUND':
      case 'FINANCE_GOAL_NOT_FOUND':
      case 'GOAL_NOT_FOUND':
      case 'MILESTONE_NOT_FOUND':
        return 'Mục dữ liệu không còn tồn tại hoặc đã bị xóa trên thiết bị khác.';
      case 'FORBIDDEN':
        return 'Bạn không có quyền thực hiện thao tác này.';
      case 'INSUFFICIENT_BALANCE':
        return 'Số dư tài khoản không đủ để thực hiện giao dịch này.';
      case 'CURRENCY_MISMATCH':
      case 'FINANCE_CURRENCY_MISMATCH':
        return 'Hai tài khoản hoặc mục tiêu phải có cùng đơn vị tiền tệ.';
      case 'FINANCE_BUDGET_EXISTS':
        return 'Đã có ngân sách đang hoạt động cho danh mục và tiền tệ này.';
      case 'FINANCE_NAME_TAKEN':
      case 'TAG_NAME_TAKEN':
      case 'CATEGORY_NAME_TAKEN':
        return 'Tên này đã được sử dụng. Vui lòng chọn tên khác.';
      case 'CIRCULAR_DEPENDENCY':
      case 'INVALID_HIERARCHY':
        return 'Không thể tạo liên kết vòng lặp hoặc vượt quá số cấp cho phép.';
      case 'AI_DISABLED':
      case 'AI_CONSENT_REQUIRED':
      case 'AI_CONTEXT_DOMAIN_NOT_ALLOWED':
      case 'AI_CONVERSATION_NOT_FOUND':
      case 'AI_MESSAGE_TOO_LARGE':
      case 'AI_RATE_LIMITED':
      case 'AI_QUOTA_EXCEEDED':
      case 'AI_BUDGET_EXCEEDED':
      case 'AI_PROVIDER_UNAVAILABLE':
      case 'AI_RESPONSE_INVALID':
      case 'AI_ACTION_NOT_FOUND':
      case 'AI_ACTION_EXPIRED':
      case 'AI_ACTION_STATE_CONFLICT':
      case 'AI_ACTION_FORBIDDEN':
      case 'AI_ACTION_VERSION_CONFLICT':
        return mapAiError(errorCode).message;
      case 'SERVICE_UNAVAILABLE':
      case 'INTERNAL_ERROR':
        return 'Hệ thống tạm thời bận, vui lòng thử lại sau ít phút. Dữ liệu trên thiết bị vẫn an toàn.';
      default:
        return 'Không thể kết nối tới máy chủ lúc này. Dữ liệu của bạn vẫn được lưu an toàn trên thiết bị.';
    }
  }

  /// Ánh xạ chi tiết 15 mã lỗi AI sang thông điệp tiếng Việt kèm hành động gợi ý rõ ràng, không lộ mã kỹ thuật.
  static AiErrorDescriptor mapAiError(String? errorCode) {
    switch (errorCode) {
      case 'AI_DISABLED':
        return const AiErrorDescriptor(
          message: 'Tính năng Trợ lý AI hiện đang tạm tắt trên hệ thống. Dữ liệu và các tính năng khác của bạn vẫn hoạt động bình thường.',
          suggestedAction: 'Thử lại sau ít phút',
          actionType: AiErrorActionType.retry,
        );
      case 'AI_CONSENT_REQUIRED':
        return const AiErrorDescriptor(
          message: 'Cần quyền truy cập dữ liệu để trả lời câu hỏi này. Bạn hãy cấp quyền cho miền dữ liệu tương ứng trong cài đặt.',
          suggestedAction: 'Mở cài đặt quyền AI',
          actionType: AiErrorActionType.openConsent,
        );
      case 'AI_CONTEXT_DOMAIN_NOT_ALLOWED':
        return const AiErrorDescriptor(
          message: 'Yêu cầu sử dụng miền dữ liệu chưa được cấp quyền trong cài đặt riêng tư.',
          suggestedAction: 'Cập nhật quyền riêng tư',
          actionType: AiErrorActionType.openConsent,
        );
      case 'AI_CONVERSATION_NOT_FOUND':
        return const AiErrorDescriptor(
          message: 'Cuộc hội thoại không tồn tại hoặc đã được xóa trước đó.',
          suggestedAction: 'Bắt đầu cuộc trò chuyện mới',
          actionType: AiErrorActionType.newConversation,
        );
      case 'AI_MESSAGE_TOO_LARGE':
        return const AiErrorDescriptor(
          message: 'Nội dung tin nhắn quá dài (vượt quá 8.000 ký tự). Vui lòng rút ngắn nội dung và gửi lại.',
          suggestedAction: 'Rút ngắn tin nhắn',
          actionType: AiErrorActionType.none,
        );
      case 'AI_RATE_LIMITED':
        return const AiErrorDescriptor(
          message: 'Bạn đang gửi tin nhắn quá nhanh. Vui lòng chờ một chút trước khi gửi yêu cầu tiếp theo.',
          suggestedAction: 'Chờ và thử lại',
          actionType: AiErrorActionType.retry,
        );
      case 'AI_QUOTA_EXCEEDED':
        return const AiErrorDescriptor(
          message: 'Bạn đã dùng hết hạn mức yêu cầu AI trong ngày hoặc trong tháng. Hạn mức sẽ tự động được làm mới theo kỳ.',
          suggestedAction: 'Kiểm tra hạn mức AI',
          actionType: AiErrorActionType.viewLimits,
        );
      case 'AI_BUDGET_EXCEEDED':
        return const AiErrorDescriptor(
          message: 'Hệ thống AI tạm thời chạm trần ngân sách vận hành của kỳ này. Bạn có thể sử dụng lại khi kỳ mới bắt đầu.',
          suggestedAction: 'Xem hạn mức sử dụng',
          actionType: AiErrorActionType.viewLimits,
        );
      case 'AI_PROVIDER_UNAVAILABLE':
        return const AiErrorDescriptor(
          message: 'Dịch vụ AI từ nhà cung cấp tạm thời gián đoạn hoặc quá tải. Vui lòng thử lại sau ít phút.',
          suggestedAction: 'Thử gửi lại',
          actionType: AiErrorActionType.retry,
        );
      case 'AI_RESPONSE_INVALID':
        return const AiErrorDescriptor(
          message: 'Phản hồi từ AI không đúng định dạng tiêu chuẩn hoặc bị từ chối an toàn. Vui lòng thử hỏi theo cách khác.',
          suggestedAction: 'Đặt lại câu hỏi',
          actionType: AiErrorActionType.none,
        );
      case 'AI_ACTION_NOT_FOUND':
        return const AiErrorDescriptor(
          message: 'Đề xuất hành động không tìm thấy hoặc đã bị xóa.',
          suggestedAction: 'Yêu cầu đề xuất mới',
          actionType: AiErrorActionType.none,
        );
      case 'AI_ACTION_EXPIRED':
        return const AiErrorDescriptor(
          message: 'Đề xuất hành động đã quá thời hạn xác nhận (15 phút).',
          suggestedAction: 'Yêu cầu AI tạo đề xuất mới',
          actionType: AiErrorActionType.none,
        );
      case 'AI_ACTION_STATE_CONFLICT':
        return const AiErrorDescriptor(
          message: 'Đề xuất hành động này đã được xử lý hoặc đã bị hủy trước đó.',
          suggestedAction: 'Kiểm tra lại trạng thái',
          actionType: AiErrorActionType.none,
        );
      case 'AI_ACTION_FORBIDDEN':
        return const AiErrorDescriptor(
          message: 'Hành động đề xuất không nằm trong danh mục an toàn được phép thực thi.',
          suggestedAction: 'Thực hiện thao tác thủ công',
          actionType: AiErrorActionType.none,
        );
      case 'AI_ACTION_VERSION_CONFLICT':
        return const AiErrorDescriptor(
          message: 'Dữ liệu mục tiêu đã được chỉnh sửa từ thiết bị khác. Vui lòng tạo đề xuất mới từ dữ liệu hiện tại.',
          suggestedAction: 'Đọc lại và đề xuất lại',
          actionType: AiErrorActionType.none,
        );
      default:
        return const AiErrorDescriptor(
          message: 'Không thể kết nối tới dịch vụ AI lúc này. Vui lòng kiểm tra kết nối mạng và thử lại.',
          suggestedAction: 'Thử lại',
          actionType: AiErrorActionType.retry,
        );
    }
  }

  /// Kiểm tra định dạng email cơ bản trên giao diện.
  static bool isValidEmail(String raw) {
    final String email = raw.trim();
    if (email.isEmpty || email.contains(' ')) return false;
    final int at = email.indexOf('@');
    if (at <= 0 || at >= email.length - 3) return false;
    final String domain = email.substring(at + 1);
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.');
  }

  /// Kiểm tra độ dài mật khẩu theo `PasswordPolicy.java` (8–128 ký tự).
  static bool isValidPassword(String password) {
    final int len = password.runes.length;
    return len >= 8 && len <= 128 && password.trim().isNotEmpty;
  }
}

/// Loại hành động người dùng có thể thực hiện để xử lý lỗi AI.
enum AiErrorActionType {
  none,
  retry,
  openConsent,
  viewLimits,
  newConversation,
}

/// Mô tả lỗi AI thân thiện kèm thông điệp tiếng Việt và hành động gợi ý rõ ràng.
class AiErrorDescriptor {
  const AiErrorDescriptor({
    required this.message,
    required this.suggestedAction,
    this.actionType = AiErrorActionType.none,
  });

  final String message;
  final String suggestedAction;
  final AiErrorActionType actionType;
}

