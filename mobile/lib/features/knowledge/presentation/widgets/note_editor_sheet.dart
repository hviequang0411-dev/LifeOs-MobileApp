import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../workspace/data/lifeos_local_repository.dart';
import '../../../ai/data/ai_api.dart';
import '../../../ai/data/ai_models.dart';
import '../../../ai/data/ai_providers.dart';
import '../../../ai/presentation/widgets/ai_consent_sheet.dart';
import '../../../ai/presentation/widgets/ai_plans_and_insights_sheets.dart';

/// Biểu mẫu Tạo mới hoặc Chỉnh sửa Ghi chú cá nhân (`note`) kèm liên kết (`note_link`).
class NoteEditorSheet extends ConsumerStatefulWidget {
  const NoteEditorSheet({
    super.key,
    this.note,
  });

  final LocalEntityRow? note;

  bool get isEditing => note != null;

  @override
  ConsumerState<NoteEditorSheet> createState() => _NoteEditorSheetState();
}

class _NoteEditorSheetState extends ConsumerState<NoteEditorSheet> {
  late final TextEditingController _titleController;
  late final TextEditingController _contentController;
  final FocusNode _titleFocus = FocusNode();

  bool _pinned = false;
  bool _archived = false;
  bool _summarizing = false;
  final Set<String> _pendingTagIds = <String>{};
  String? _titleError;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic> p = widget.note?.payload ?? const <String, dynamic>{};
    _titleController = TextEditingController(text: (p['title'] as String?) ?? '');
    _contentController = TextEditingController(text: (p['content'] as String?) ?? '');
    _pinned = p['pinned'] == true;
    _archived = p['archived'] == true;

    _titleFocus.addListener(() {
      if (!_titleFocus.hasFocus && mounted) {
        setState(() {
          _titleError = _titleController.text.trim().isEmpty
              ? 'Vui lòng nhập tiêu đề ghi chú.'
              : null;
        });
      }
    });
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    _titleFocus.dispose();
    super.dispose();
  }

  Future<void> _confirmDelete() async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa ghi chú?'),
        content: const Text(
          'Hành động này sẽ xóa ghi chú và toàn bộ liên kết đính kèm. Không thể hoàn tác.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: LifeOsTokens.of(ctx).danger,
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Xác nhận xóa'),
          ),
        ],
      ),
    );

    if (ok == true && mounted && widget.note != null) {
      final String userId = ref.read(currentUserIdProvider);
      ref.read(localRepositoryProvider).deleteNote(
            userId: userId,
            noteId: widget.note!.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      if (mounted && Navigator.canPop(context)) {
        Navigator.of(context).pop(true);
      }
    }
  }

  Future<void> _handleAiSummarize() async {
    if (widget.note == null || _summarizing) return;
    if (!ref.read(aiIsOnlineProvider)) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Tính năng AI cần kết nối mạng để thực hiện.'),
          ),
        );
      }
      return;
    }
    final AiPreferencesResponse prefs =
        ref.read(aiPreferencesProvider).valueOrNull ??
            AiPreferencesResponse.initialDefault();

    if (!prefs.notes) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Tính năng tóm tắt cần cấp quyền miền "Ghi chú" trong cài đặt quyền riêng tư AI.',
            ),
          ),
        );
        unawaited(
          showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            backgroundColor: Colors.transparent,
            builder: (_) => const AiConsentSheet(),
          ),
        );
      }
      return;
    }

    setState(() => _summarizing = true);
    try {
      final AiApiClient api = ref.read(aiApiClientProvider);
      final AiNoteSummaryResponse summary = await api.summarizeNote(
        widget.note!.id,
        style: 'BULLETS',
        contextDomains: <String>[AiContextDomain.notes.code],
      );
      if (mounted) {
        unawaited(
          showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            backgroundColor: Colors.transparent,
            builder: (_) => AiNoteSummarySheet(summary: summary),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        final AiErrorDescriptor err =
            ViStrings.mapAiError(e is LifeOsApiException ? e.errorCode : null);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(err.message)),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _summarizing = false);
      }
    }
  }

  Future<void> _openCreateTagDialog() async {
    final String? name = await showDialog<String>(
      context: context,
      builder: (BuildContext ctx) => const _CreateTagDialog(),
    );
    if (name == null || name.isEmpty || !mounted) return;

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);
    final String tagId = repo.createTag(
      userId: userId,
      name: name,
      nowUtc: refUtc,
    );
    if (widget.note != null) {
      repo.attachTagToNote(
        userId: userId,
        noteId: widget.note!.id,
        tagId: tagId,
        nowUtc: refUtc,
      );
    } else {
      setState(() => _pendingTagIds.add(tagId));
    }
  }

  Future<void> _openAddLinkDialog() async {
    if (widget.note == null) return;
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);

    final List<LocalEntityRow> tasks = repo.db.listEntities(
      userId: userId,
      entityType: SyncEntityType.task,
    );
    final List<LocalEntityRow> goals = repo.db.listEntities(
      userId: userId,
      entityType: SyncEntityType.goal,
    );

    String targetType = 'TASK';
    String? selectedTargetId = tasks.isNotEmpty ? tasks.first.id : (goals.isNotEmpty ? goals.first.id : null);

    final bool? added = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => StatefulBuilder(
        builder: (BuildContext context, StateSetter setDialogState) {
          final List<LocalEntityRow> currentOptions = targetType == 'TASK' ? tasks : goals;
          if (selectedTargetId == null && currentOptions.isNotEmpty) {
            selectedTargetId = currentOptions.first.id;
          }

          return AlertDialog(
            title: const Text('Thêm liên kết mới'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                SegmentedButton<String>(
                  segments: const <ButtonSegment<String>>[
                    ButtonSegment<String>(
                      value: 'TASK',
                      label: Text('Công việc'),
                      icon: Icon(Icons.check_box_outlined, size: 16),
                    ),
                    ButtonSegment<String>(
                      value: 'GOAL',
                      label: Text('Mục tiêu'),
                      icon: Icon(Icons.flag_rounded, size: 16),
                    ),
                  ],
                  selected: <String>{targetType},
                  onSelectionChanged: (Set<String> s) {
                    setDialogState(() {
                      targetType = s.first;
                      final List<LocalEntityRow> opts = targetType == 'TASK' ? tasks : goals;
                      selectedTargetId = opts.isNotEmpty ? opts.first.id : null;
                    });
                  },
                ),
                const SizedBox(height: 14),
                if (currentOptions.isEmpty)
                  Text(
                    targetType == 'TASK'
                        ? 'Chưa có công việc nào trong danh mục.'
                        : 'Chưa có mục tiêu nào trong danh mục.',
                    style: const TextStyle(fontSize: 13, color: Colors.grey),
                  )
                else
                  DropdownButtonFormField<String>(
                    key: const Key('dropdown_pick_link_target'),
                    initialValue: selectedTargetId,
                    decoration: InputDecoration(
                      labelText: targetType == 'TASK' ? 'Chọn công việc' : 'Chọn mục tiêu',
                      border: const OutlineInputBorder(),
                    ),
                    isExpanded: true,
                    items: currentOptions.map((LocalEntityRow item) {
                      final String t = (item.payload['title'] as String?) ?? item.id;
                      return DropdownMenuItem<String>(
                        value: item.id,
                        child: Text(t, maxLines: 1, overflow: TextOverflow.ellipsis),
                      );
                    }).toList(),
                    onChanged: (String? val) {
                      setDialogState(() => selectedTargetId = val);
                    },
                  ),
              ],
            ),
            actions: <Widget>[
              TextButton(
                onPressed: () => Navigator.of(ctx).pop(false),
                child: const Text(ViStrings.cancelAction),
              ),
              FilledButton(
                key: const Key('btn_confirm_add_note_link'),
                onPressed: selectedTargetId == null
                    ? null
                    : () => Navigator.of(ctx).pop(true),
                child: const Text('Thêm liên kết'),
              ),
            ],
          );
        },
      ),
    );

    if (added == true && selectedTargetId != null && mounted) {
      repo.linkNoteToTarget(
        userId: userId,
        noteId: widget.note!.id,
        targetType: targetType,
        targetId: selectedTargetId!,
        nowUtc: ref.read(referenceUtcProvider),
      );
      setState(() {});
    }
  }

  void _saveNote() {
    final String title = _titleController.text.trim();
    if (title.isEmpty) {
      setState(() => _titleError = 'Vui lòng nhập tiêu đề ghi chú.');
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);

    final String savedNoteId = repo.saveNote(
      userId: userId,
      noteId: widget.note?.id,
      title: title,
      content: _contentController.text,
      pinned: _pinned,
      archived: _archived,
      nowUtc: refUtc,
    );
    if (widget.note == null && _pendingTagIds.isNotEmpty) {
      for (final String tagId in _pendingTagIds) {
        repo.attachTagToNote(
          userId: userId,
          noteId: savedNoteId,
          tagId: tagId,
          nowUtc: refUtc,
        );
      }
    }

    if (mounted && Navigator.canPop(context)) {
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    final List<LocalEntityRow> links = widget.note != null
        ? repo.listLinksForNote(userId: userId, noteId: widget.note!.id)
        : const <LocalEntityRow>[];
    final List<LocalEntityRow> allTags =
        ref.watch(entitiesByTypeStreamProvider(SyncEntityType.tag)).valueOrNull ??
            const <LocalEntityRow>[];
    ref.watch(entitiesByTypeStreamProvider(SyncEntityType.noteTag));
    final Set<String> activeTagIds = widget.note != null
        ? repo.listTagIdsForNote(userId: userId, noteId: widget.note!.id)
        : _pendingTagIds;
    final bool isOnline = ref.watch(aiIsOnlineProvider);

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 16, 20, 20 + bottomInset),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Flexible(
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Text(
                    widget.isEditing ? 'Chỉnh sửa ghi chú' : 'Ghi chú mới',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  if (widget.isEditing)
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        if (ref.watch(aiPreferencesProvider).valueOrNull?.notes == true)
                          TextButton.icon(
                            key: const Key('btn_ai_summarize_note'),
                            onPressed: _summarizing ? null : _handleAiSummarize,
                            icon: _summarizing
                                ? SizedBox(
                                    width: 14,
                                    height: 14,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: tokens.accentPrimary,
                                    ),
                                  )
                                : Icon(
                                    Icons.auto_awesome_rounded,
                                    size: 16,
                                    color: isOnline
                                        ? tokens.accentPrimary
                                        : tokens.textMuted,
                                  ),
                            label: Text(
                              _summarizing ? 'Đang tóm tắt...' : 'Tóm tắt AI',
                              style: TextStyle(
                                fontSize: 12,
                                color: _summarizing
                                    ? tokens.textSecondary
                                    : (isOnline
                                        ? tokens.accentPrimary
                                        : tokens.textMuted),
                              ),
                            ),
                          ),
                        IconButton(
                          key: const Key('btn_delete_note'),
                          tooltip: 'Xóa ghi chú',
                          onPressed: _confirmDelete,
                          icon: Icon(
                            Icons.delete_outline_rounded,
                            color: tokens.danger,
                          ),
                        ),
                      ],
                    ),
                ],
              ),
              const SizedBox(height: 12),

              // Title
              TextField(
                key: const Key('input_note_title'),
                controller: _titleController,
                focusNode: _titleFocus,
                maxLength: 200,
                decoration: InputDecoration(
                  labelText: 'Tiêu đề ghi chú *',
                  hintText: 'Ý tưởng, tài liệu, kiến thức...',
                  errorText: _titleError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),

              // Content
              TextField(
                key: const Key('input_note_content'),
                controller: _contentController,
                minLines: 4,
                maxLines: 10,
                decoration: const InputDecoration(
                  labelText: 'Nội dung ghi chú',
                  hintText: 'Nhập nội dung ghi chú (tự động đồng bộ và lưu trên máy)...',
                  border: OutlineInputBorder(),
                  alignLabelWithHint: true,
                ),
              ),
              const SizedBox(height: 10),

              // Pinned Switch
              SwitchListTile(
                key: const Key('switch_note_pinned'),
                contentPadding: EdgeInsets.zero,
                secondary: Icon(
                  _pinned ? Icons.push_pin : Icons.push_pin_outlined,
                  color: _pinned ? tokens.domainKnowledge : tokens.textSecondary,
                ),
                title: const Text('Ghim lên đầu trang'),
                subtitle: const Text(
                  'Ưu tiên hiển thị trên cùng danh sách',
                  style: TextStyle(fontSize: 12),
                ),
                value: _pinned,
                onChanged: (bool val) => setState(() => _pinned = val),
              ),

              // Archived Switch (if editing)
              if (widget.isEditing)
                SwitchListTile(
                  key: const Key('switch_note_archived'),
                  contentPadding: EdgeInsets.zero,
                  secondary: Icon(
                    _archived ? Icons.archive : Icons.archive_outlined,
                    color: _archived ? tokens.warning : tokens.textSecondary,
                  ),
                  title: const Text('Lưu trữ ghi chú'),
                  subtitle: const Text(
                    'Ẩn khỏi danh sách chính nhưng vẫn bảo lưu dữ liệu',
                    style: TextStyle(fontSize: 12),
                  ),
                  value: _archived,
                  onChanged: (bool val) => setState(() => _archived = val),
                ),

              // Tags Section
              const Divider(),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Text(
                    'Nhãn',
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                      fontSize: 14,
                    ),
                  ),
                  TextButton.icon(
                    key: const Key('btn_add_note_tag'),
                    onPressed: _openCreateTagDialog,
                    icon: const Icon(Icons.label_outline_rounded, size: 18),
                    label: const Text('+ Nhãn'),
                  ),
                ],
              ),
              if (allTags.isEmpty)
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Text(
                    'Chưa có nhãn nào. Bấm "+ Nhãn" để phân loại ghi chú.',
                    style: TextStyle(
                      fontSize: 12.5,
                      color: tokens.textSecondary,
                    ),
                  ),
                )
              else
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: allTags.map((LocalEntityRow tag) {
                      final String tagName =
                          (tag.payload['name'] as String?) ?? 'Nhãn';
                      final bool selected = activeTagIds.contains(tag.id);
                      return FilterChip(
                        key: Key('chip_note_tag_${tag.id}'),
                        label: Text('#$tagName'),
                        selected: selected,
                        onSelected: (bool next) {
                          if (widget.note == null) {
                            setState(() {
                              if (next) {
                                _pendingTagIds.add(tag.id);
                              } else {
                                _pendingTagIds.remove(tag.id);
                              }
                            });
                            return;
                          }
                          final DateTime nowUtc = ref.read(referenceUtcProvider);
                          if (next) {
                            repo.attachTagToNote(
                              userId: userId,
                              noteId: widget.note!.id,
                              tagId: tag.id,
                              nowUtc: nowUtc,
                            );
                          } else {
                            repo.detachTagFromNote(
                              userId: userId,
                              noteId: widget.note!.id,
                              tagId: tag.id,
                              nowUtc: nowUtc,
                            );
                          }
                        },
                      );
                    }).toList(),
                  ),
                ),

              // Links Section (if editing)
              if (widget.isEditing) ...<Widget>[
                const Divider(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      'Liên kết (${links.length})',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                        fontSize: 14,
                      ),
                    ),
                    TextButton.icon(
                      key: const Key('btn_add_note_link'),
                      onPressed: _openAddLinkDialog,
                      icon: const Icon(Icons.add_link_rounded, size: 18),
                      label: const Text('Thêm liên kết'),
                    ),
                  ],
                ),
                if (links.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 4),
                    child: Text(
                      'Chưa gắn liên kết nào tới Công việc hay Mục tiêu.',
                      style: TextStyle(fontSize: 12.5, color: Colors.grey),
                    ),
                  )
                else
                  ...links.map((LocalEntityRow l) {
                    final String type = (l.payload['targetType'] as String?)?.toUpperCase() ?? 'TASK';
                    final String targetId = (l.payload['targetId'] as String?) ?? '';
                    final LocalEntityRow? targetEntity = repo.db.getEntity(
                      userId: userId,
                      entityType: type == 'TASK' ? SyncEntityType.task : SyncEntityType.goal,
                      entityId: targetId,
                    );
                    final String targetTitle =
                        (targetEntity?.payload['title'] as String?) ?? 'ID: ${targetId.substring(0, targetId.length.clamp(0, 8))}...';

                    return ListTile(
                      contentPadding: EdgeInsets.zero,
                      dense: true,
                      leading: Icon(
                        type == 'TASK' ? Icons.check_box_outlined : Icons.flag_rounded,
                        color: type == 'TASK' ? tokens.domainHabitButtonBg : tokens.domainGoal,
                        size: 20,
                      ),
                      title: Text(targetTitle, maxLines: 1, overflow: TextOverflow.ellipsis),
                      subtitle: Text(
                        type == 'TASK' ? 'Công việc' : 'Mục tiêu dài hạn',
                        style: const TextStyle(fontSize: 11),
                      ),
                      trailing: IconButton(
                        tooltip: 'Gỡ liên kết',
                        icon: const Icon(Icons.close_rounded, size: 18),
                        onPressed: () {
                          repo.deleteNoteLink(
                            userId: userId,
                            linkId: l.id,
                            nowUtc: ref.read(referenceUtcProvider),
                          );
                          setState(() {});
                        },
                      ),
                    );
                  }),
              ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Submit Button
            FilledButton(
              key: const Key('btn_save_note'),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
              ),
              onPressed: _saveNote,
              child: Text(widget.isEditing ? 'Lưu thay đổi' : 'Tạo ghi chú'),
            ),
          ],
        ),
      ),
    );
  }
}

class _CreateTagDialog extends StatefulWidget {
  const _CreateTagDialog();

  @override
  State<_CreateTagDialog> createState() => _CreateTagDialogState();
}

class _CreateTagDialogState extends State<_CreateTagDialog> {
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Tạo nhãn mới'),
      content: TextField(
        key: const Key('input_new_tag_name'),
        controller: _controller,
        autofocus: true,
        decoration: const InputDecoration(
          labelText: 'Tên nhãn *',
          hintText: 'Ví dụ: Công việc, Học tập',
          border: OutlineInputBorder(),
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text(ViStrings.cancelAction),
        ),
        FilledButton(
          key: const Key('btn_confirm_create_tag'),
          onPressed: () {
            final String v = _controller.text.trim();
            if (v.isEmpty) return;
            Navigator.of(context).pop(v);
          },
          child: const Text('Tạo nhãn'),
        ),
      ],
    );
  }
}
