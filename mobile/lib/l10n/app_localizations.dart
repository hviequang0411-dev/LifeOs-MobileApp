import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_vi.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[Locale('vi')];

  /// No description provided for @appTitle.
  ///
  /// In vi, this message translates to:
  /// **'LifeOS'**
  String get appTitle;

  /// No description provided for @navToday.
  ///
  /// In vi, this message translates to:
  /// **'Hôm nay'**
  String get navToday;

  /// No description provided for @navPlan.
  ///
  /// In vi, this message translates to:
  /// **'Kế hoạch'**
  String get navPlan;

  /// No description provided for @navFinance.
  ///
  /// In vi, this message translates to:
  /// **'Tài chính'**
  String get navFinance;

  /// No description provided for @navKnowledge.
  ///
  /// In vi, this message translates to:
  /// **'Tri thức'**
  String get navKnowledge;

  /// No description provided for @navOverview.
  ///
  /// In vi, this message translates to:
  /// **'Tổng quan'**
  String get navOverview;

  /// No description provided for @quickCaptureButton.
  ///
  /// In vi, this message translates to:
  /// **'Thêm nhanh'**
  String get quickCaptureButton;

  /// No description provided for @syncStatusSynced.
  ///
  /// In vi, this message translates to:
  /// **'Đã lưu trên máy · Đã đồng bộ'**
  String get syncStatusSynced;

  /// No description provided for @syncStatusSyncing.
  ///
  /// In vi, this message translates to:
  /// **'Đang đồng bộ dữ liệu...'**
  String get syncStatusSyncing;

  /// No description provided for @syncStatusOffline.
  ///
  /// In vi, this message translates to:
  /// **'Ngoại tuyến · Đã lưu an toàn trên máy'**
  String get syncStatusOffline;

  /// No description provided for @syncStatusPaused.
  ///
  /// In vi, this message translates to:
  /// **'Tạm dừng đồng bộ'**
  String get syncStatusPaused;

  /// No description provided for @syncStatusNeedsAttention.
  ///
  /// In vi, this message translates to:
  /// **'Cần xem lại mục đồng bộ'**
  String get syncStatusNeedsAttention;

  /// No description provided for @greetingMorning.
  ///
  /// In vi, this message translates to:
  /// **'Chào buổi sáng'**
  String get greetingMorning;

  /// No description provided for @greetingAfternoon.
  ///
  /// In vi, this message translates to:
  /// **'Chào buổi chiều'**
  String get greetingAfternoon;

  /// No description provided for @greetingEvening.
  ///
  /// In vi, this message translates to:
  /// **'Chào buổi tối'**
  String get greetingEvening;

  /// No description provided for @todayFocusTitle.
  ///
  /// In vi, this message translates to:
  /// **'Trọng tâm hôm nay'**
  String get todayFocusTitle;

  /// No description provided for @tasksTodayTitle.
  ///
  /// In vi, this message translates to:
  /// **'Việc cần làm hôm nay'**
  String get tasksTodayTitle;

  /// No description provided for @tasksOverdueTitle.
  ///
  /// In vi, this message translates to:
  /// **'Quá hạn cần xử lý'**
  String get tasksOverdueTitle;

  /// No description provided for @habitsTodayTitle.
  ///
  /// In vi, this message translates to:
  /// **'Thói quen trong ngày'**
  String get habitsTodayTitle;

  /// No description provided for @routinesTitle.
  ///
  /// In vi, this message translates to:
  /// **'Quy trình (Routines)'**
  String get routinesTitle;

  /// No description provided for @financeSnapshotTitle.
  ///
  /// In vi, this message translates to:
  /// **'Tài chính hôm nay'**
  String get financeSnapshotTitle;

  /// No description provided for @maskAmountTooltip.
  ///
  /// In vi, this message translates to:
  /// **'Ẩn/hiện số tiền nhạy cảm'**
  String get maskAmountTooltip;

  /// No description provided for @emptyTasksTitle.
  ///
  /// In vi, this message translates to:
  /// **'Hôm nay bạn chưa có việc cần làm nào'**
  String get emptyTasksTitle;

  /// No description provided for @emptyTasksSubtitle.
  ///
  /// In vi, this message translates to:
  /// **'Chạm vào nút Thêm nhanh để ghi lại việc quan trọng nhất trong ngày.'**
  String get emptyTasksSubtitle;

  /// No description provided for @emptyHabitsTitle.
  ///
  /// In vi, this message translates to:
  /// **'Bắt đầu xây dựng thói quen tốt'**
  String get emptyHabitsTitle;

  /// No description provided for @emptyHabitsSubtitle.
  ///
  /// In vi, this message translates to:
  /// **'Mỗi bước nhỏ đều được lưu giữ an toàn ngay cả khi ngoại tuyến.'**
  String get emptyHabitsSubtitle;

  /// No description provided for @emptyFinanceTitle.
  ///
  /// In vi, this message translates to:
  /// **'Chưa có giao dịch nào trong kỳ'**
  String get emptyFinanceTitle;

  /// No description provided for @emptyFinanceSubtitle.
  ///
  /// In vi, this message translates to:
  /// **'Thêm tài khoản hoặc ghi lại khoản thu/chi đầu tiên của bạn.'**
  String get emptyFinanceSubtitle;

  /// No description provided for @emptyNotesTitle.
  ///
  /// In vi, this message translates to:
  /// **'Kho tri thức cá nhân đang trống'**
  String get emptyNotesTitle;

  /// No description provided for @emptyNotesSubtitle.
  ///
  /// In vi, this message translates to:
  /// **'Viết ghi chú (tối đa 192 KB) và gắn liên kết tới Công việc hoặc Mục tiêu.'**
  String get emptyNotesSubtitle;

  /// No description provided for @emptyGoalsTitle.
  ///
  /// In vi, this message translates to:
  /// **'Thiết lập mục tiêu dài hạn'**
  String get emptyGoalsTitle;

  /// No description provided for @emptyGoalsSubtitle.
  ///
  /// In vi, this message translates to:
  /// **'Chia nhỏ mục tiêu thành các cột mốc, liên kết với việc làm và thói quen.'**
  String get emptyGoalsSubtitle;

  /// No description provided for @priorityNone.
  ///
  /// In vi, this message translates to:
  /// **'Không'**
  String get priorityNone;

  /// No description provided for @priorityLow.
  ///
  /// In vi, this message translates to:
  /// **'Thấp'**
  String get priorityLow;

  /// No description provided for @priorityMedium.
  ///
  /// In vi, this message translates to:
  /// **'Trung bình'**
  String get priorityMedium;

  /// No description provided for @priorityHigh.
  ///
  /// In vi, this message translates to:
  /// **'Cao'**
  String get priorityHigh;

  /// No description provided for @confirmDeleteTitle.
  ///
  /// In vi, this message translates to:
  /// **'Xác nhận xóa'**
  String get confirmDeleteTitle;

  /// No description provided for @cancelAction.
  ///
  /// In vi, this message translates to:
  /// **'Hủy'**
  String get cancelAction;

  /// No description provided for @saveAction.
  ///
  /// In vi, this message translates to:
  /// **'Lưu'**
  String get saveAction;

  /// No description provided for @deleteAction.
  ///
  /// In vi, this message translates to:
  /// **'Xóa'**
  String get deleteAction;

  /// No description provided for @retryAction.
  ///
  /// In vi, this message translates to:
  /// **'Thử lại'**
  String get retryAction;

  /// No description provided for @logoutSafeTitle.
  ///
  /// In vi, this message translates to:
  /// **'Đăng xuất an toàn'**
  String get logoutSafeTitle;

  /// No description provided for @accountDeletionPendingBanner.
  ///
  /// In vi, this message translates to:
  /// **'Tài khoản đang trong thời gian chờ xóa (7 ngày). Bạn có thể hủy yêu cầu bất cứ lúc nào để khôi phục toàn bộ dữ liệu.'**
  String get accountDeletionPendingBanner;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['vi'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'vi':
      return AppLocalizationsVi();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
