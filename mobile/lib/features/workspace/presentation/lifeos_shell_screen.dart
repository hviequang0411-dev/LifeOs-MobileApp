import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../../auth/data/online_account_service.dart';
import '../../auth/presentation/sign_in_welcome_view.dart';
import '../../calendar/presentation/plan_tab_view.dart';
import '../../finance/presentation/finance_tab_view.dart';
import '../../knowledge/presentation/knowledge_and_goals_tab_view.dart';
import '../../notification/data/local_notification_service.dart';
import '../../review/presentation/overview_and_settings_tab_view.dart';
import '../../sync/presentation/sync_diagnostics_and_logout_dialogs.dart';
import '../../tasks/presentation/my_day_tab_view.dart';
import '../../tasks/presentation/widgets/routine_player_sheet.dart';
import '../data/lifeos_local_repository.dart';
import 'quick_capture_bottom_sheet.dart';

export '../../calendar/presentation/plan_tab_view.dart';
export '../../finance/presentation/finance_tab_view.dart';
export '../../knowledge/presentation/knowledge_and_goals_tab_view.dart';
export '../../review/presentation/overview_and_settings_tab_view.dart';
export '../../sync/presentation/sync_diagnostics_and_logout_dialogs.dart';
export '../../tasks/presentation/my_day_tab_view.dart';
export 'quick_capture_bottom_sheet.dart';

/// Khung điều hướng thích ứng 5 Tab gốc của LifeOS (Điện thoại: `NavigationBar` · Tablet >= 600dp: `NavigationRail`).
class LifeOsShellScreen extends ConsumerStatefulWidget {
  const LifeOsShellScreen({super.key});

  @override
  ConsumerState<LifeOsShellScreen> createState() => _LifeOsShellScreenState();
}

class _LifeOsShellScreenState extends ConsumerState<LifeOsShellScreen>
    with WidgetsBindingObserver {
  int _selectedIndex = 0;
  bool _fabVisible = true;
  bool _cancellingDeletion = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        ref.read(localNotificationCoordinatorProvider).initializeOnAppStart();
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && mounted) {
      _rescheduleNotificationsSilently();
    }
  }

  Future<void> _rescheduleNotificationsSilently() async {
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final Map<String, dynamic> settings = repo.getUserSettings(userId);
    final NotificationPreferences prefs =
        ref.read(notificationPreferencesProvider);
    await ref.read(localNotificationCoordinatorProvider).rescheduleFromDatabase(
          userId: userId,
          userSettings: settings,
          preferences: prefs,
          nowUtc: ref.read(referenceUtcProvider),
        );
  }

  void _handleNotificationRoute(NotificationRouteTarget target) {
    final int nextTab = target.tabIndex.clamp(0, _tabs.length - 1);
    setState(() {
      _selectedIndex = nextTab;
      _fabVisible = true;
    });
    if (target.targetType == 'ROUTINE' && target.targetId.isNotEmpty) {
      final String userId = ref.read(currentUserIdProvider);
      final LocalEntityRow? routine = ref.read(appDatabaseProvider).getEntity(
            userId: userId,
            entityType: SyncEntityType.routine,
            entityId: target.targetId,
          );
      if (routine != null) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) return;
          showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            backgroundColor: Colors.transparent,
            builder: (BuildContext ctx) => RoutinePlayerSheet(routine: routine),
          );
        });
      }
    }
  }

  static const List<({IconData icon, IconData activeIcon, String label})>
      _tabs = <({IconData icon, IconData activeIcon, String label})>[
    (
      icon: Icons.wb_sunny_outlined,
      activeIcon: Icons.wb_sunny_rounded,
      label: ViStrings.navToday,
    ),
    (
      icon: Icons.event_note_outlined,
      activeIcon: Icons.event_note_rounded,
      label: ViStrings.navPlan,
    ),
    (
      icon: Icons.account_balance_wallet_outlined,
      activeIcon: Icons.account_balance_wallet_rounded,
      label: ViStrings.navFinance,
    ),
    (
      icon: Icons.menu_book_outlined,
      activeIcon: Icons.menu_book_rounded,
      label: ViStrings.navKnowledge,
    ),
    (
      icon: Icons.insights_outlined,
      activeIcon: Icons.insights_rounded,
      label: ViStrings.navOverview,
    ),
  ];

  void _openQuickCaptureSheet(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (BuildContext ctx) => const QuickCaptureBottomSheet(),
    );
  }

  void _openSyncDiagnosticsSheet(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (BuildContext ctx) => const SyncDiagnosticsBottomSheet(),
    );
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<NotificationRouteTarget?>(
      pendingNotificationRouteProvider,
      (NotificationRouteTarget? prev, NotificationRouteTarget? next) {
        if (next != null) {
          ref.read(pendingNotificationRouteProvider.notifier).state = null;
          _handleNotificationRoute(next);
        }
      },
    );

    final bool isSignedOut = ref.watch(isSignedOutProvider);
    if (isSignedOut) {
      return const SignInWelcomeView();
    }
    final bool deletionPending =
        ref.watch(accountDeletionPendingBannerProvider);
    final bool requiresReAuth = ref.watch(requiresReAuthBannerProvider);

    final Widget rawTabBody = switch (_selectedIndex) {
      0 => MyDayTabView(
          onOpenQuickCapture: () => _openQuickCaptureSheet(context),
          onOpenDiagnostics: () => _openSyncDiagnosticsSheet(context),
        ),
      1 => const PlanTabView(),
      2 => const FinanceTabView(),
      3 => const KnowledgeAndGoalsTabView(),
      _ => OverviewAndSettingsTabView(
          onOpenDiagnostics: () => _openSyncDiagnosticsSheet(context),
        ),
    };

    final Widget activeTabBody = NotificationListener<UserScrollNotification>(
      onNotification: (UserScrollNotification n) {
        if (n.metrics.axis == Axis.vertical) {
          if (n.direction == ScrollDirection.reverse && _fabVisible) {
            setState(() => _fabVisible = false);
          } else if (n.direction == ScrollDirection.forward && !_fabVisible) {
            setState(() => _fabVisible = true);
          }
        }
        return false;
      },
      child: rawTabBody,
    );

    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        final double screenWidth = constraints.maxWidth > 0
            ? constraints.maxWidth
            : MediaQuery.sizeOf(context).width;
        final bool isTablet = screenWidth >= LifeOsTokens.phoneMaxBreakpoint;

        return AppSwitcherPrivacyBlurWrapper(
          enabled: true,
          child: Scaffold(
            body: SafeArea(
              child: Column(
                children: <Widget>[
                  if (deletionPending) _buildDeletionCooldownBanner(context),
                  if (requiresReAuth) _buildReAuthBanner(context),
                  Expanded(
                    child: isTablet
                        ? Row(
                            children: <Widget>[
                              NavigationRail(
                                selectedIndex: _selectedIndex,
                                onDestinationSelected: (int idx) => setState(() {
                                  _selectedIndex = idx;
                                  _fabVisible = true;
                                }),
                                labelType: NavigationRailLabelType.all,
                                leading: Padding(
                                  padding:
                                      const EdgeInsets.symmetric(vertical: 12),
                                  child: FloatingActionButton(
                                    tooltip: ViStrings.quickCaptureTitle,
                                    onPressed: () =>
                                        _openQuickCaptureSheet(context),
                                    child: const Icon(Icons.add_rounded),
                                  ),
                                ),
                                destinations: _tabs
                                    .map(
                                      (e) => NavigationRailDestination(
                                        icon: Icon(e.icon),
                                        selectedIcon: Icon(e.activeIcon),
                                        label: Text(e.label),
                                      ),
                                    )
                                    .toList(),
                              ),
                              const VerticalDivider(width: 1),
                              Expanded(child: activeTabBody),
                            ],
                          )
                        : activeTabBody,
                  ),
                ],
              ),
            ),
            floatingActionButton: isTablet
                ? null
                : AnimatedSlide(
                    duration: const Duration(milliseconds: 200),
                    offset: _fabVisible ? Offset.zero : const Offset(0, 1.8),
                    child: AnimatedOpacity(
                      duration: const Duration(milliseconds: 180),
                      opacity: _fabVisible ? 1.0 : 0.0,
                      child: IgnorePointer(
                        ignoring: !_fabVisible,
                        child: FloatingActionButton.extended(
                          key: const Key('fab_quick_capture'),
                          onPressed: () => _openQuickCaptureSheet(context),
                          icon: const Icon(Icons.add_rounded),
                          label: const Text(ViStrings.quickCaptureTitle),
                        ),
                      ),
                    ),
                  ),
            bottomNavigationBar: isTablet
                ? null
                : NavigationBar(
                    selectedIndex: _selectedIndex,
                    onDestinationSelected: (int idx) => setState(() {
                      _selectedIndex = idx;
                      _fabVisible = true;
                    }),
                    destinations: _tabs
                        .map(
                          (e) => NavigationDestination(
                            icon: Icon(e.icon),
                            selectedIcon: Icon(e.activeIcon),
                            label: e.label,
                          ),
                        )
                        .toList(),
                  ),
          ),
        );
      },
    );
  }

  Future<void> _cancelDeletionRequest(BuildContext context) async {
    setState(() => _cancellingDeletion = true);
    try {
      await ref.read(onlineAccountServiceProvider).cancelAccountDeletion();
      if (!mounted) return;
      ref.read(accountDeletionPendingBannerProvider.notifier).state = false;
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã hủy yêu cầu xóa tài khoản. Tài khoản của bạn tiếp tục hoạt động bình thường.',
            ),
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(OnlineAccountService.formatAccountErrorVi(e)),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _cancellingDeletion = false);
      }
    }
  }

  Widget _buildDeletionCooldownBanner(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Container(
      width: double.infinity,
      color: tokens.danger.withValues(alpha: 0.14),
      padding: const EdgeInsets.symmetric(
        horizontal: LifeOsTokens.space4,
        vertical: LifeOsTokens.space2,
      ),
      child: Row(
        children: <Widget>[
          Icon(Icons.warning_amber_rounded, color: tokens.danger, size: 20),
          const SizedBox(width: 8),
          const Expanded(
            child: Text(
              'Tài khoản đang trong 7 ngày chờ xóa. Dữ liệu cục bộ vẫn an toàn.',
              style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600),
            ),
          ),
          TextButton(
            key: const Key('btn_banner_cancel_deletion'),
            onPressed: _cancellingDeletion
                ? null
                : () => _cancelDeletionRequest(context),
            child: _cancellingDeletion
                ? const SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Hủy yêu cầu xóa'),
          ),
        ],
      ),
    );
  }

  Widget _buildReAuthBanner(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Container(
      width: double.infinity,
      color: tokens.warning.withValues(alpha: 0.16),
      padding: const EdgeInsets.symmetric(
        horizontal: LifeOsTokens.space4,
        vertical: LifeOsTokens.space2,
      ),
      child: Row(
        children: <Widget>[
          Icon(Icons.lock_clock_outlined, color: tokens.warning, size: 20),
          const SizedBox(width: 8),
          const Expanded(
            child: Text(
              'Phiên đăng nhập hết hạn. Dữ liệu trên máy vẫn được giữ nguyên.',
              style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600),
            ),
          ),
          TextButton(
            key: const Key('btn_banner_reauth'),
            onPressed: () {
              ref.read(requiresReAuthBannerProvider.notifier).state = false;
              ref.read(isSignedOutProvider.notifier).state = true;
            },
            child: const Text('Đăng nhập lại'),
          ),
        ],
      ),
    );
  }
}
