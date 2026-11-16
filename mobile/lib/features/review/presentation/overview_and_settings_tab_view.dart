import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/database/sqlite_runtime.dart'
    show deleteLocalDatabaseFiles;
import '../../../core/l10n/vi_strings.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../auth/presentation/account_flows_sheets.dart';
import '../../sync/presentation/sync_diagnostics_and_logout_dialogs.dart';
import '../../workspace/data/local_extras_repository.dart';
import '../../workspace/presentation/widgets/focus_garden_sheet.dart';
import '../../workspace/presentation/widgets/global_search_sheet.dart';
import '../../workspace/presentation/widgets/life_wheel_sheet.dart';
import '../../ai/presentation/ai_assistant_screen.dart';
import '../../ai/presentation/widgets/ai_consent_sheet.dart';
import '../../ai/presentation/widgets/ai_usage_and_limits_sheet.dart';
import 'widgets/notification_settings_sheet.dart';
import 'widgets/review_wizard_sheet.dart';

/// Tab 4: Màn hình **Tổng quan, Trình hướng dẫn Phản tư (`UPW6`), Công cụ `LOCAL_ONLY` (`UPW7`) & Cài đặt (Phương án C)**
class OverviewAndSettingsTabView extends ConsumerWidget {
  const OverviewAndSettingsTabView({
    super.key,
    required this.onOpenDiagnostics,
  });

  final VoidCallback onOpenDiagnostics;

  void _openModalSheet(BuildContext context, Widget sheet) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => sheet,
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ThemeMode themeMode = ref.watch(themeModeProvider);
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> reviews = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.lifeReview))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<OutboxMutationRecord> outbox =
        ref.watch(outboxStreamProvider).valueOrNull ??
            const <OutboxMutationRecord>[];

    return ListView(
      padding: const EdgeInsets.fromLTRB(
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        LifeOsTokens.space4,
        112,
      ),
      children: <Widget>[
        // Trình hướng dẫn Phản tư (Interactive Guided Review Wizard - UPW6)
        Text(
          'Nhìn lại & Phản tư',
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: LifeOsTokens.space2),
        LifeOsCard(
          elevation: 2,
          padding: const EdgeInsets.all(LifeOsTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: tokens.domainGoal.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                    ),
                    child: Icon(
                      Icons.rate_review_outlined,
                      color: tokens.domainGoal,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: LifeOsTokens.space3),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          'Đã lưu ${reviews.length} bản tổng kết cá nhân',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: tokens.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Ghi lại phản tư 3 bước: số liệu, tâm trạng 1–5 sao & bài học đúc kết.',
                          style: TextStyle(
                            fontSize: 13,
                            color: tokens.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: LifeOsTokens.space3),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  style: FilledButton.styleFrom(
                    minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                  ),
                  onPressed: () =>
                      _openModalSheet(context, const ReviewWizardSheet()),
                  icon: const Icon(Icons.auto_awesome_outlined, size: 18),
                  label: const Text('Mở trình hướng dẫn phản tư'),
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Công cụ mở rộng & Tìm kiếm Offline
        Text(
          'Không gian chuyên sâu & Tìm kiếm',
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: LifeOsTokens.space2),
        LifeOsCard(
          elevation: 1,
          padding: EdgeInsets.zero,
          child: Column(
            children: <Widget>[
              ListTile(
                leading: Icon(Icons.search_rounded, color: tokens.domainTask),
                title: const Text('Tìm kiếm trên thiết bị'),
                subtitle: const Text(
                  'Tra cứu nhanh công việc và ghi chú ngay cả khi không có mạng',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () =>
                    _openModalSheet(context, const GlobalSearchSheet()),
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(Icons.radar_rounded, color: tokens.domainGoal),
                title: const Text('Bánh xe cuộc sống (8 khía cạnh)'),
                subtitle: const Text(
                  'Đánh giá cân bằng sống · Chỉ lưu trên thiết bị này',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () =>
                    _openModalSheet(context, const LifeWheelSheet()),
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(Icons.forest_outlined, color: tokens.domainHabit),
                title: const Text('Khu vườn tập trung'),
                subtitle: const Text(
                  'Đếm giờ tập trung sâu trồng cây · Chỉ lưu trên thiết bị này',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () =>
                    _openModalSheet(context, const FocusGardenSheet()),
              ),
              const Divider(height: 1),
              ListTile(
                key: const Key('tile_ai_assistant'),
                leading: Icon(Icons.auto_awesome_rounded, color: tokens.accentPrimary),
                title: const Text('Trợ lý AI (Online)'),
                subtitle: const Text(
                  'Hỏi đáp thông minh, lập kế hoạch và phân tích dữ liệu cá nhân',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (BuildContext ctx) => const AiAssistantScreen(),
                    ),
                  );
                },
              ),
            ],
          ),
        ),

        const SizedBox(height: LifeOsTokens.space4),

        // Đồng bộ, Thông báo, Quyền riêng tư & Tài khoản
        Text(
          'Tài khoản, Thông báo & Đồng bộ',
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: LifeOsTokens.space2),
        LifeOsCard(
          elevation: 1,
          padding: EdgeInsets.zero,
          child: Column(
            children: <Widget>[
              ListTile(
                key: const Key('tile_notification_settings'),
                leading: Icon(
                  Icons.notifications_active_outlined,
                  color: tokens.accentPrimary,
                ),
                title: const Text('Thông báo & Giờ im lặng'),
                subtitle: const Text(
                  'Bật/tắt nhắc theo loại, giờ im lặng và quyền riêng tư màn hình khóa',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () => _openModalSheet(
                  context,
                  const NotificationSettingsSheet(),
                ),
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(Icons.sync_rounded, color: tokens.accentPrimary),
                title: const Text('Trạng thái đồng bộ dữ liệu'),
                subtitle: Text(
                  '${outbox.length} thay đổi đang chờ gửi lên máy chủ',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: onOpenDiagnostics,
              ),
              const Divider(height: 1),
              ListTile(
                key: const Key('tile_sign_in_account'),
                leading: Icon(
                  Icons.account_circle_outlined,
                  color: tokens.domainTask,
                ),
                title: Text(
                  'Tài khoản · ${ref.watch(userDisplayNameProvider)}',
                ),
                subtitle: const Text(
                  'Đăng nhập, tạo tài khoản mới hoặc chuyển tài khoản đồng bộ',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () {
                  ref.read(isSignedOutProvider.notifier).state = true;
                },
              ),
              const Divider(height: 1),
              ListTile(
                key: const Key('tile_change_password'),
                leading: Icon(
                  Icons.password_rounded,
                  color: tokens.textSecondary,
                ),
                title: const Text('Đổi mật khẩu'),
                subtitle: const Text('Cập nhật mật khẩu bảo vệ tài khoản trực tuyến'),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () async {
                  final bool? ok = await showDialog<bool>(
                    context: context,
                    builder: (BuildContext ctx) => const ChangePasswordDialog(),
                  );
                  if (ok == true && context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Đã cập nhật mật khẩu thành công.'),
                      ),
                    );
                  }
                },
              ),
              const Divider(height: 1),
              ListTile(
                key: const Key('tile_logout_keep_local'),
                leading: Icon(Icons.logout_rounded, color: tokens.textPrimary),
                title: const Text('Đăng xuất tài khoản'),
                subtitle: const Text(
                  'Đăng xuất phiên làm việc hiện tại; giữ nguyên toàn bộ dữ liệu trên máy (Phương án C)',
                ),
                onTap: () async {
                  await ref.read(tokenStorageProvider).clearTokensOnly();
                  ref.read(isSignedOutProvider.notifier).state = true;
                },
              ),
              const Divider(height: 1),
              SwitchListTile(
                secondary: Icon(
                  Icons.dark_mode_outlined,
                  color: tokens.textSecondary,
                ),
                title: const Text('Chế độ giao diện tối'),
                value: themeMode == ThemeMode.dark,
                onChanged: (bool isDark) {
                  ref.read(themeModeProvider.notifier).state =
                      isDark ? ThemeMode.dark : ThemeMode.light;
                },
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(
                  Icons.battery_saver_outlined,
                  color: tokens.textSecondary,
                ),
                title: const Text(
                  'Hướng dẫn duy trì thông báo nền (Xiaomi/Samsung/Oppo)',
                ),
                subtitle: const Text(
                  'Bật "Tự khởi chạy" và "Không hạn chế pin" để nhận nhắc việc đúng phút khi đóng ứng dụng.',
                ),
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(Icons.logout_rounded, color: tokens.textPrimary),
                title: const Text(ViStrings.logoutSafeTitle),
                subtitle: const Text(
                  'Bảo vệ 100% thay đổi chưa đồng bộ với 3 lựa chọn an toàn',
                ),
                onTap: () {
                  showDialog<void>(
                    context: context,
                    builder: (BuildContext ctx) => const SafeLogoutDialog(),
                  );
                },
              ),
              const Divider(height: 1),
              ListTile(
                key: const Key('tile_ai_settings'),
                leading: Icon(
                  Icons.shield_outlined,
                  color: tokens.accentPrimary,
                ),
                title: const Text('Quyền riêng tư & Hạn mức AI'),
                subtitle: const Text(
                  'Quản lý 5 miền dữ liệu consent và kiểm tra hạn mức sử dụng',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () =>
                    _openModalSheet(context, const AiConsentSheet()),
              ),
              const Divider(height: 1),
              ListTile(
                key: const Key('tile_delete_ai_data'),
                leading: Icon(
                  Icons.delete_sweep_outlined,
                  color: tokens.danger,
                ),
                title: Text(
                  'Xóa toàn bộ dữ liệu AI trên server',
                  style: TextStyle(
                    color: tokens.danger,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                subtitle: const Text(
                  'Xóa hội thoại, tin nhắn, nhật ký ngữ cảnh và reset consent về tắt (DELETE /ai/data)',
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () {
                  _openModalSheet(
                    context,
                    const AiUsageAndLimitsSheet(),
                  );
                },
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(
                  Icons.delete_forever_outlined,
                  color: tokens.danger,
                ),
                title: Text(
                  'Xóa dữ liệu trên máy',
                  style: TextStyle(
                    color: tokens.danger,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                subtitle: const Text(
                  'Xóa toàn bộ dữ liệu gốc, hàng đợi đồng bộ và dữ liệu chỉ lưu trên thiết bị này',
                ),
                onTap: () {
                  showDialog<void>(
                    context: context,
                    builder: (BuildContext ctx) =>
                        const _WipeLocalDataDialog(),
                  );
                },
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Hộp thoại xác nhận Phương án C (`[CONFIRMED]`): Xóa dữ liệu trên máy
/// Hiển thị số thay đổi Outbox chưa đồng bộ + cảnh báo kho LOCAL_ONLY, bắt buộc nhập chữ xác nhận.
class _WipeLocalDataDialog extends ConsumerStatefulWidget {
  const _WipeLocalDataDialog();

  @override
  ConsumerState<_WipeLocalDataDialog> createState() =>
      _WipeLocalDataDialogState();
}

class _WipeLocalDataDialogState extends ConsumerState<_WipeLocalDataDialog> {
  static const String _requiredPhrase = 'XÓA DỮ LIỆU';
  final TextEditingController _confirmController = TextEditingController();
  bool _canConfirm = false;

  @override
  void initState() {
    super.initState();
    _confirmController.addListener(() {
      final bool matched =
          _confirmController.text.trim().toUpperCase() == _requiredPhrase;
      if (matched != _canConfirm) {
        setState(() => _canConfirm = matched);
      }
    });
  }

  @override
  void dispose() {
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _executeWipe() async {
    final String userId = ref.read(currentUserIdProvider);
    final AppDatabase db = ref.read(appDatabaseProvider);
    final LocalExtrasRepository localExtras =
        ref.read(localExtrasRepositoryProvider);
    final TokenStorage tokenStorage = ref.read(tokenStorageProvider);

    // 1. Xóa sạch dữ liệu người dùng trong CSDL gốc + Outbox + CSDL phụ LOCAL_ONLY
    db.clearUserData(userId);
    localExtras.clearAll();
    await tokenStorage.clearTokensOnly();

    // 2. Xóa tệp SQLite trên đĩa (hoặc VFS trên Web) nếu đang chạy ở chế độ file-backed
    deleteLocalDatabaseFiles(userId);

    // 3. Đặt lại trạng thái phiên và đưa về màn hình đăng nhập
    ref.read(accountDeletionPendingBannerProvider.notifier).state = false;
    ref.read(requiresReAuthBannerProvider.notifier).state = false;
    ref.read(isSignedOutProvider.notifier).state = true;

    if (mounted) {
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã xóa toàn bộ dữ liệu trên máy và đăng xuất an toàn.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<OutboxMutationRecord> outbox =
        ref.watch(outboxStreamProvider).valueOrNull ??
            const <OutboxMutationRecord>[];

    return AlertDialog(
      title: Row(
        children: <Widget>[
          Icon(Icons.warning_amber_rounded, color: tokens.danger),
          const SizedBox(width: 8),
          const Expanded(child: Text('Xóa dữ liệu trên máy?')),
        ],
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Hành động này sẽ xóa vĩnh viễn toàn bộ dữ liệu lưu cục bộ của tài khoản hiện tại khỏi thiết bị này:',
            style: TextStyle(color: tokens.textPrimary, fontSize: 13.5),
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: tokens.danger.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
              border: Border.all(color: tokens.danger.withValues(alpha: 0.4)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  '• ${outbox.length} thay đổi chưa đồng bộ trong Outbox sẽ bị mất vĩnh viễn.',
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: tokens.danger,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '• Toàn bộ dữ liệu chỉ lưu trên máy (Bánh xe cuộc sống, Khu vườn tập trung) cũng bị xóa sạch và không thể khôi phục.',
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: tokens.danger,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Text(
            'Để xác nhận, vui lòng nhập chính xác cụm từ "$_requiredPhrase":',
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w600,
              color: tokens.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _confirmController,
            autofocus: true,
            decoration: const InputDecoration(
              hintText: _requiredPhrase,
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text(ViStrings.cancelAction),
        ),
        FilledButton(
          style: FilledButton.styleFrom(
            backgroundColor: tokens.danger,
            foregroundColor: Colors.white,
          ),
          onPressed: _canConfirm ? _executeWipe : null,
          child: const Text('Xác nhận xóa vĩnh viễn'),
        ),
      ],
    );
  }
}
