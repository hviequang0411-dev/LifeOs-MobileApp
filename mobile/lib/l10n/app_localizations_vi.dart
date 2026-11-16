// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Vietnamese (`vi`).
class AppLocalizationsVi extends AppLocalizations {
  AppLocalizationsVi([String locale = 'vi']) : super(locale);

  @override
  String get appTitle => 'LifeOS';

  @override
  String get navToday => 'Hôm nay';

  @override
  String get navPlan => 'Kế hoạch';

  @override
  String get navFinance => 'Tài chính';

  @override
  String get navKnowledge => 'Tri thức';

  @override
  String get navOverview => 'Tổng quan';

  @override
  String get quickCaptureButton => 'Thêm nhanh';

  @override
  String get syncStatusSynced => 'Đã lưu trên máy · Đã đồng bộ';

  @override
  String get syncStatusSyncing => 'Đang đồng bộ dữ liệu...';

  @override
  String get syncStatusOffline => 'Ngoại tuyến · Đã lưu an toàn trên máy';

  @override
  String get syncStatusPaused => 'Tạm dừng đồng bộ';

  @override
  String get syncStatusNeedsAttention => 'Cần xem lại mục đồng bộ';

  @override
  String get greetingMorning => 'Chào buổi sáng';

  @override
  String get greetingAfternoon => 'Chào buổi chiều';

  @override
  String get greetingEvening => 'Chào buổi tối';

  @override
  String get todayFocusTitle => 'Trọng tâm hôm nay';

  @override
  String get tasksTodayTitle => 'Việc cần làm hôm nay';

  @override
  String get tasksOverdueTitle => 'Quá hạn cần xử lý';

  @override
  String get habitsTodayTitle => 'Thói quen trong ngày';

  @override
  String get routinesTitle => 'Quy trình (Routines)';

  @override
  String get financeSnapshotTitle => 'Tài chính hôm nay';

  @override
  String get maskAmountTooltip => 'Ẩn/hiện số tiền nhạy cảm';

  @override
  String get emptyTasksTitle => 'Hôm nay bạn chưa có việc cần làm nào';

  @override
  String get emptyTasksSubtitle =>
      'Chạm vào nút Thêm nhanh để ghi lại việc quan trọng nhất trong ngày.';

  @override
  String get emptyHabitsTitle => 'Bắt đầu xây dựng thói quen tốt';

  @override
  String get emptyHabitsSubtitle =>
      'Mỗi bước nhỏ đều được lưu giữ an toàn ngay cả khi ngoại tuyến.';

  @override
  String get emptyFinanceTitle => 'Chưa có giao dịch nào trong kỳ';

  @override
  String get emptyFinanceSubtitle =>
      'Thêm tài khoản hoặc ghi lại khoản thu/chi đầu tiên của bạn.';

  @override
  String get emptyNotesTitle => 'Kho tri thức cá nhân đang trống';

  @override
  String get emptyNotesSubtitle =>
      'Viết ghi chú (tối đa 192 KB) và gắn liên kết tới Công việc hoặc Mục tiêu.';

  @override
  String get emptyGoalsTitle => 'Thiết lập mục tiêu dài hạn';

  @override
  String get emptyGoalsSubtitle =>
      'Chia nhỏ mục tiêu thành các cột mốc, liên kết với việc làm và thói quen.';

  @override
  String get priorityNone => 'Không';

  @override
  String get priorityLow => 'Thấp';

  @override
  String get priorityMedium => 'Trung bình';

  @override
  String get priorityHigh => 'Cao';

  @override
  String get confirmDeleteTitle => 'Xác nhận xóa';

  @override
  String get cancelAction => 'Hủy';

  @override
  String get saveAction => 'Lưu';

  @override
  String get deleteAction => 'Xóa';

  @override
  String get retryAction => 'Thử lại';

  @override
  String get logoutSafeTitle => 'Đăng xuất an toàn';

  @override
  String get accountDeletionPendingBanner =>
      'Tài khoản đang trong thời gian chờ xóa (7 ngày). Bạn có thể hủy yêu cầu bất cứ lúc nào để khôi phục toàn bộ dữ liệu.';
}
