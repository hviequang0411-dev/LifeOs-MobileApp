import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/sync/sync_engine.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';

/// Màn hình Chẩn đoán Đồng bộ & Giải quyết xung đột 3 bên (`PWP4`, `PWP15`).
class SyncDiagnosticsBottomSheet extends ConsumerStatefulWidget {
  const SyncDiagnosticsBottomSheet({super.key});

  @override
  ConsumerState<SyncDiagnosticsBottomSheet> createState() =>
      _SyncDiagnosticsBottomSheetState();
}

class _SyncDiagnosticsBottomSheetState
    extends ConsumerState<SyncDiagnosticsBottomSheet> {
  bool _isSyncing = false;
  bool _showTechnicalDetails = false;
  String? _feedback;
  bool _feedbackIsError = false;

  String _formatEntityTypeVi(SyncEntityType type) {
    return switch (type) {
      SyncEntityType.task => 'Công việc',
      SyncEntityType.project => 'Dự án',
      SyncEntityType.category => 'Phân loại',
      SyncEntityType.habit => 'Thói quen',
      SyncEntityType.habitEntry => 'Điểm danh thói quen',
      SyncEntityType.routine => 'Chuỗi thói quen',
      SyncEntityType.routineStep => 'Bước chuỗi thói quen',
      SyncEntityType.routineSession => 'Phiên chạy chuỗi thói quen',
      SyncEntityType.routineStepLog => 'Kết quả bước chuỗi thói quen',
      SyncEntityType.financeAccount => 'Ví / Tài khoản',
      SyncEntityType.financeCategory => 'Danh mục tài chính',
      SyncEntityType.financeTransaction => 'Giao dịch tài chính',
      SyncEntityType.accountTransfer => 'Chuyển khoản giữa ví',
      SyncEntityType.budget => 'Ngân sách',
      SyncEntityType.recurringTransaction => 'Giao dịch định kỳ',
      SyncEntityType.savingGoal => 'Mục tiêu tiết kiệm',
      SyncEntityType.savingContribution => 'Khoản góp tiết kiệm',
      SyncEntityType.calendarEvent => 'Sự kiện lịch',
      SyncEntityType.calendarEventException => 'Ngoại lệ sự kiện',
      SyncEntityType.timeBlock => 'Khối tập trung',
      SyncEntityType.reminder => 'Lời nhắc',
      SyncEntityType.note => 'Ghi chú',
      SyncEntityType.tag => 'Thẻ',
      SyncEntityType.noteTag => 'Gắn thẻ ghi chú',
      SyncEntityType.noteLink => 'Liên kết ghi chú',
      SyncEntityType.goal => 'Mục tiêu',
      SyncEntityType.goalMilestone => 'Cột mốc mục tiêu',
      SyncEntityType.lifeReview => 'Bản phản tư',
      SyncEntityType.userSettings => 'Cài đặt cá nhân',
    };
  }

  Future<void> _runManualSync(String userId) async {
    if (_isSyncing) return;
    setState(() {
      _isSyncing = true;
      _feedback = null;
      _feedbackIsError = false;
    });
    try {
      final SyncCycleSummary summary =
          await ref.read(syncEngineProvider).runSyncCycle(userId: userId);
      if (!mounted) return;
      setState(() {
        if (summary.hadNetworkError) {
          _feedback =
              'Chưa thể kết nối máy chủ lúc này. Dữ liệu vẫn được giữ an toàn trên máy.';
          _feedbackIsError = true;
        } else if (summary.pushedConflictsNeedUser > 0) {
          _feedback =
              'Đã đồng bộ (nhận ${summary.pulledChanges} mục), phát hiện ${summary.pushedConflictsNeedUser} xung đột cần bạn chọn.';
          _feedbackIsError = true;
        } else {
          _feedback =
              'Đồng bộ hoàn tất: đã gửi ${summary.pushedApplied} thay đổi, nhận ${summary.pulledChanges} cập nhật.';
          _feedbackIsError = false;
        }
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _feedback =
            'Không thể hoàn tất đồng bộ lúc này. Vui lòng thử lại khi có mạng ổn định.';
        _feedbackIsError = true;
      });
    } finally {
      if (mounted) {
        setState(() => _isSyncing = false);
      }
    }
  }

  Future<void> _exportOutboxJson(AppDatabase db, String userId) async {
    final Map<String, dynamic> backup = db.exportUnsyncedBackupJson(userId);
    final String prettyJson = const JsonEncoder.withIndent('  ').convert(backup);
    await Clipboard.setData(ClipboardData(text: prettyJson));
    if (!mounted) return;
    setState(() {
      _feedback =
          'Đã sao chép bản sao lưu JSON (${backup['pendingCount']} mục chờ gửi) vào bộ nhớ tạm.';
      _feedbackIsError = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final AppDatabase db = ref.watch(appDatabaseProvider);
    final LocalSyncStateRecord state =
        ref.watch(syncStateStreamProvider).valueOrNull ??
            db.getSyncState(userId);
    final List<OutboxMutationRecord> outbox =
        ref.watch(outboxStreamProvider).valueOrNull ??
            const <OutboxMutationRecord>[];
    final List<OutboxMutationRecord> conflicts = outbox
        .where((OutboxMutationRecord m) => m.status == 'NEEDS_USER_ACTION')
        .toList();
    final List<SyncLogRecord> logs = db.listSyncLogs(userId, limit: 30);
    final bool dbHealthy = db.runQuickIntegrityCheck();

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.88,
        ),
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Expanded(
                    child: Text(
                      'Chẩn đoán đồng bộ & Hàng đợi',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    tooltip: 'Đóng',
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: tokens.bgElevated,
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  border: Border.all(color: tokens.borderSubtle),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      'Đang chờ đồng bộ: ${outbox.length} mục · Kiểm tra dữ liệu: ${dbHealthy ? "Ổn định" : "Cần kiểm tra"}',
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w600,
                        color: tokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    InkWell(
                      onTap: () => setState(
                        () => _showTechnicalDetails = !_showTechnicalDetails,
                      ),
                      child: Text(
                        _showTechnicalDetails
                            ? 'Ẩn thông số kỹ thuật'
                            : 'Xem thông số kỹ thuật (mốc đồng bộ)',
                        style: TextStyle(
                          fontSize: 12,
                          color: tokens.accentPrimary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    if (_showTechnicalDetails) ...<Widget>[
                      const SizedBox(height: 6),
                      Text(
                        'Mốc đồng bộ hiện tại: ${state.lastCursor <= 0 ? "Chưa đồng bộ lần đầu" : state.lastCursor}',
                        style: TextStyle(
                          fontSize: 11.5,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (_feedback != null) ...<Widget>[
                const SizedBox(height: 10),
                if (_feedbackIsError)
                  LifeOsErrorBanner(message: _feedback!)
                else
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: tokens.success.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                    ),
                    child: Text(
                      _feedback!,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: tokens.success,
                      ),
                    ),
                  ),
              ],
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Tạm dừng đồng bộ tự động'),
                value: state.syncPaused,
                onChanged: (bool paused) {
                  db.upsertSyncState(userId: userId, syncPaused: paused);
                },
              ),
              if (conflicts.isNotEmpty) ...<Widget>[
                const Divider(),
                Text(
                  'Mục cần bạn giải quyết xung đột (${conflicts.length}):',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 6),
                ...conflicts.map((OutboxMutationRecord c) {
                  final SyncEngine syncEngine = ref.read(syncEngineProvider);
                  final String displayTitle = (c.payload['title'] as String?) ??
                      (c.payload['name'] as String?) ??
                      _formatEntityTypeVi(c.entityType);
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: LifeOsCard(
                      elevation: 1,
                      padding: const EdgeInsets.all(10),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            '${_formatEntityTypeVi(c.entityType)} · $displayTitle',
                            style: const TextStyle(fontWeight: FontWeight.w700),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            c.lastErrorMessage ??
                                'Dữ liệu thay đổi trên hai thiết bị cùng lúc',
                            style: TextStyle(
                              fontSize: 12.5,
                              color: tokens.textSecondary,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            runSpacing: 6,
                            children: <Widget>[
                              OutlinedButton(
                                onPressed: () {
                                  final Map<String, dynamic> serverVer =
                                      c.serverConflict ?? c.payload;
                                  final int srvVer =
                                      (serverVer['version'] as num?)?.toInt() ??
                                          (c.baseVersion + 1);
                                  syncEngine.resolveConflictByUser(
                                    userId: userId,
                                    conflictMutation: c,
                                    chosenPayload: serverVer,
                                    serverVersion: srvVer,
                                  );
                                },
                                child: const Text('Giữ bản Máy chủ'),
                              ),
                              FilledButton.tonal(
                                onPressed: () {
                                  final int srvVer =
                                      (c.serverConflict?['version'] as num?)
                                              ?.toInt() ??
                                          (c.baseVersion + 1);
                                  syncEngine.resolveConflictByUser(
                                    userId: userId,
                                    conflictMutation: c,
                                    chosenPayload: c.payload,
                                    serverVersion: srvVer,
                                  );
                                },
                                child: const Text('Giữ bản Trên máy'),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ],
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  key: const Key('btn_sync_now'),
                  style: FilledButton.styleFrom(
                    minimumSize:
                        const Size.fromHeight(LifeOsTokens.minTouchTarget),
                  ),
                  onPressed: _isSyncing ? null : () => _runManualSync(userId),
                  icon: _isSyncing
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.sync_rounded),
                  label: Text(
                    _isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ngay bây giờ',
                  ),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  key: const Key('btn_export_outbox_backup'),
                  style: OutlinedButton.styleFrom(
                    minimumSize:
                        const Size.fromHeight(LifeOsTokens.minTouchTarget),
                  ),
                  onPressed: () => _exportOutboxJson(db, userId),
                  icon: const Icon(Icons.download_outlined, size: 18),
                  label: const Text('Xuất bản sao lưu dữ liệu chờ (JSON)'),
                ),
              ),
              const SizedBox(height: 14),
              Text(
                'Nhật ký đồng bộ gần đây (${logs.length}):',
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w700,
                  color: tokens.textPrimary,
                ),
              ),
              const SizedBox(height: 6),
              if (logs.isEmpty)
                Text(
                  'Chưa có bản ghi đồng bộ nào trong phiên này.',
                  style: TextStyle(fontSize: 12.5, color: tokens.textSecondary),
                )
              else
                ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 160),
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: logs.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (BuildContext ctx, int idx) {
                      final SyncLogRecord log = logs[idx];
                      final String timeShort = log.createdAt.length >= 19
                          ? log.createdAt.substring(11, 19)
                          : log.createdAt;
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 6),
                        child: Text(
                          '[$timeShort] ${log.direction} · ${log.outcome}${log.details != null && log.details!.isNotEmpty ? ' — ${log.details}' : ''}',
                          style: TextStyle(
                            fontSize: 12,
                            color: tokens.textSecondary,
                          ),
                        ),
                      );
                    },
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Hộp thoại Đăng xuất an toàn 3 lựa chọn (`D167`, `SYN-A13`).
class SafeLogoutDialog extends ConsumerStatefulWidget {
  const SafeLogoutDialog({super.key});

  @override
  ConsumerState<SafeLogoutDialog> createState() => _SafeLogoutDialogState();
}

class _SafeLogoutDialogState extends ConsumerState<SafeLogoutDialog> {
  final TextEditingController _confirmWordController = TextEditingController();
  bool _loading = false;
  String? _errorText;
  Map<String, dynamic>? _exportedBackup;
  bool _backupCopied = false;

  @override
  void dispose() {
    _confirmWordController.dispose();
    super.dispose();
  }

  Future<void> _executeSafeLogout({
    DirtyLogoutChoice? choiceWhenDirty,
    String? confirmationWord,
  }) async {
    setState(() {
      _loading = true;
      _errorText = null;
    });
    final String userId = ref.read(currentUserIdProvider);
    final SyncEngine syncEngine = ref.read(syncEngineProvider);
    try {
      final ({bool loggedOut, Map<String, dynamic>? exportedBackup}) res =
          await syncEngine.performSafeLogout(
        userId: userId,
        choiceWhenDirty: choiceWhenDirty,
        confirmationWordForForceDiscard: confirmationWord,
      );
      if (!mounted) return;
      if (!res.loggedOut) {
        setState(() {
          _loading = false;
          _errorText =
              'Không thể đồng bộ hết dữ liệu chờ lúc này. Hãy kiểm tra mạng hoặc chọn xuất sao lưu JSON.';
        });
        return;
      }

      if (res.exportedBackup != null) {
        setState(() {
          _loading = false;
          _exportedBackup = res.exportedBackup;
        });
        return;
      }

      ref.read(isSignedOutProvider.notifier).state = true;
      Navigator.of(context).pop();
    } on ArgumentError catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _errorText = e.message?.toString() ??
            'Vui lòng nhập chính xác "${ViStrings.logoutForceConfirmWord}" để xác nhận.';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _errorText = 'Không thể hoàn tất đăng xuất lúc này. Vui lòng thử lại.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<OutboxMutationRecord> outbox =
        ref.watch(outboxStreamProvider).valueOrNull ??
            const <OutboxMutationRecord>[];

    if (_exportedBackup != null) {
      final String jsonStr =
          const JsonEncoder.withIndent('  ').convert(_exportedBackup);
      final Object? count = _exportedBackup!['pendingCount'];
      return AlertDialog(
        title: const Text('Đã xuất bản sao lưu an toàn'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Text(
              'Phiên đăng nhập đã được ngắt an toàn và lưu dự phòng $count thay đổi chưa đồng bộ.',
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              key: const Key('btn_copy_logout_backup'),
              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: jsonStr));
                if (mounted) {
                  setState(() => _backupCopied = true);
                }
              },
              icon: Icon(
                _backupCopied ? Icons.check_rounded : Icons.copy_rounded,
              ),
              label: Text(
                _backupCopied
                    ? 'Đã sao chép JSON sao lưu'
                    : 'Sao chép JSON sao lưu',
              ),
            ),
          ],
        ),
        actions: <Widget>[
          FilledButton(
            key: const Key('btn_finish_backup_logout'),
            onPressed: () {
              ref.read(isSignedOutProvider.notifier).state = true;
              Navigator.of(context).pop();
            },
            child: const Text('Hoàn tất đăng xuất'),
          ),
        ],
      );
    }

    if (outbox.isEmpty) {
      return AlertDialog(
        title: const Text(ViStrings.logoutCleanTitle),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            const Text(ViStrings.logoutCleanSubtitle),
            if (_errorText != null) ...<Widget>[
              const SizedBox(height: 10),
              LifeOsErrorBanner(message: _errorText!),
            ],
          ],
        ),
        actions: <Widget>[
          TextButton(
            onPressed: _loading ? null : () => Navigator.of(context).pop(),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_clean_logout'),
            onPressed: _loading ? null : () => _executeSafeLogout(),
            child: Text(_loading ? 'Đang đăng xuất...' : 'Đăng xuất'),
          ),
        ],
      );
    }

    return AlertDialog(
      title: Text(ViStrings.logoutDirtyTitle(outbox.length)),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            if (_errorText != null) ...<Widget>[
              LifeOsErrorBanner(message: _errorText!),
              const SizedBox(height: 10),
            ],
            FilledButton(
              key: const Key('btn_logout_sync_first'),
              onPressed: _loading
                  ? null
                  : () => _executeSafeLogout(
                        choiceWhenDirty: DirtyLogoutChoice.syncThenLogout,
                      ),
              child: const Text(ViStrings.logoutOptionSyncFirst),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              key: const Key('btn_logout_export_backup'),
              onPressed: _loading
                  ? null
                  : () => _executeSafeLogout(
                        choiceWhenDirty:
                            DirtyLogoutChoice.exportBackupThenLogout,
                      ),
              child: const Text(ViStrings.logoutOptionExportBackup),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_force_logout_word'),
              controller: _confirmWordController,
              decoration: const InputDecoration(
                labelText: 'Nhập "XÓA" nếu muốn buộc đăng xuất',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
            TextButton(
              key: const Key('btn_logout_force_discard'),
              style: TextButton.styleFrom(foregroundColor: tokens.danger),
              onPressed: _loading
                  ? null
                  : () {
                      final String word = _confirmWordController.text.trim();
                      if (word.toUpperCase() !=
                          ViStrings.logoutForceConfirmWord) {
                        setState(() {
                          _errorText =
                              'Vui lòng nhập chính xác "${ViStrings.logoutForceConfirmWord}" trước khi buộc đăng xuất.';
                        });
                        return;
                      }
                      _executeSafeLogout(
                        choiceWhenDirty:
                            DirtyLogoutChoice.forceDiscardAndLogout,
                        confirmationWord: word,
                      );
                    },
              child: const Text(ViStrings.logoutOptionForceDiscard),
            ),
          ],
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: _loading ? null : () => Navigator.of(context).pop(),
          child: const Text(ViStrings.cancelAction),
        ),
      ],
    );
  }
}
