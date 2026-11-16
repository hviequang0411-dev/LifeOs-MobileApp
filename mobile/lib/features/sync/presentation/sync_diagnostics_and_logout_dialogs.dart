import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/sync_engine.dart';

/// Màn hình Chẩn đoán Đồng bộ & Giải quyết xung đột 3 bên (`PWP4`, `PWP15`).
class SyncDiagnosticsBottomSheet extends ConsumerWidget {
  const SyncDiagnosticsBottomSheet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final String userId = ref.watch(currentUserIdProvider);
    final AppDatabase db = ref.watch(appDatabaseProvider);
    final SyncEngine syncEngine = ref.watch(syncEngineProvider);
    final LocalSyncStateRecord state =
        ref.watch(syncStateStreamProvider).valueOrNull ??
            db.getSyncState(userId);
    final List<OutboxMutationRecord> outbox =
        ref.watch(outboxStreamProvider).valueOrNull ??
            const <OutboxMutationRecord>[];
    final List<OutboxMutationRecord> conflicts = outbox
        .where((OutboxMutationRecord m) => m.status == 'NEEDS_USER_ACTION')
        .toList();

    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Chẩn đoán đồng bộ & Hàng đợi',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 8),
          Text(
            'Mốc đồng bộ gần nhất: ${state.lastCursor} · Đang chờ gửi: ${outbox.length} mục · Toàn vẹn dữ liệu: ${db.runQuickIntegrityCheck() ? "Ổn định" : "Cần kiểm tra"}',
          ),
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
            ...conflicts.map((OutboxMutationRecord c) {
              return ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('${c.entityType.wireName} (${c.entityId})'),
                subtitle: Text(c.lastErrorMessage ?? 'Xung đột phiên bản'),
                trailing: Wrap(
                  spacing: 6,
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
                            (c.serverConflict?['version'] as num?)?.toInt() ??
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
              );
            }),
          ],
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: () async {
                await syncEngine.runSyncCycle(userId: userId);
              },
              icon: const Icon(Icons.sync_rounded),
              label: const Text('Đồng bộ ngay bây giờ'),
            ),
          ),
        ],
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

  @override
  void dispose() {
    _confirmWordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final String userId = ref.watch(currentUserIdProvider);
    final SyncEngine syncEngine = ref.watch(syncEngineProvider);
    final List<OutboxMutationRecord> outbox =
        ref.watch(outboxStreamProvider).valueOrNull ??
            const <OutboxMutationRecord>[];

    if (outbox.isEmpty) {
      return AlertDialog(
        title: const Text(ViStrings.logoutCleanTitle),
        content: const Text(ViStrings.logoutCleanSubtitle),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            onPressed: () async {
              await syncEngine.performSafeLogout(userId: userId);
              if (context.mounted) Navigator.of(context).pop();
            },
            child: const Text('Đăng xuất'),
          ),
        ],
      );
    }

    return AlertDialog(
      title: Text(ViStrings.logoutDirtyTitle(outbox.length)),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          FilledButton(
            onPressed: () async {
              await syncEngine.performSafeLogout(
                userId: userId,
                choiceWhenDirty: DirtyLogoutChoice.syncThenLogout,
              );
              if (context.mounted) Navigator.of(context).pop();
            },
            child: const Text(ViStrings.logoutOptionSyncFirst),
          ),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () async {
              await syncEngine.performSafeLogout(
                userId: userId,
                choiceWhenDirty: DirtyLogoutChoice.exportBackupThenLogout,
              );
              if (context.mounted) Navigator.of(context).pop();
            },
            child: const Text(ViStrings.logoutOptionExportBackup),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _confirmWordController,
            decoration: const InputDecoration(
              labelText: 'Nhập "XÓA" nếu muốn buộc đăng xuất',
            ),
          ),
          const SizedBox(height: 8),
          TextButton(
            onPressed: () async {
              if (_confirmWordController.text.trim().toUpperCase() ==
                  ViStrings.logoutForceConfirmWord) {
                await syncEngine.performSafeLogout(
                  userId: userId,
                  choiceWhenDirty: DirtyLogoutChoice.forceDiscardAndLogout,
                  confirmationWordForForceDiscard: _confirmWordController.text,
                );
                if (context.mounted) Navigator.of(context).pop();
              }
            },
            child: const Text(ViStrings.logoutOptionForceDiscard),
          ),
        ],
      ),
    );
  }
}

