import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/database_records.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../ai/data/ai_models.dart';
import '../../../ai/data/ai_providers.dart';
import '../../../ai/presentation/widgets/ai_plans_and_insights_sheets.dart';

/// Bottom sheet chỉnh sửa chi tiết, xóa mềm và gọi AI chia nhỏ một Công việc.
class TaskEditorSheet extends ConsumerStatefulWidget {
  const TaskEditorSheet({
    super.key,
    required this.task,
  });

  final LocalEntityRow task;

  @override
  ConsumerState<TaskEditorSheet> createState() => _TaskEditorSheetState();
}

class _TaskEditorSheetState extends ConsumerState<TaskEditorSheet> {
  static final RegExp _isoDatePattern = RegExp(r'^\d{4}-\d{2}-\d{2}$');
  static final RegExp _timePattern = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');

  late final TextEditingController _titleController;
  late final TextEditingController _descController;
  late final TextEditingController _dueDateController;
  late final TextEditingController _dueTimeController;

  late String _priority;
  late String _status;
  String? _projectId;
  String? _goalId;
  String? _validationError;
  bool _breakingDown = false;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic> p = widget.task.payload;
    _titleController = TextEditingController(text: (p['title'] as String?) ?? '');
    _descController =
        TextEditingController(text: (p['description'] as String?) ?? '');
    _dueDateController =
        TextEditingController(text: (p['dueDate'] as String?) ?? '');
    final String rawDueTime = (p['dueTime'] as String?) ?? '';
    _dueTimeController = TextEditingController(
      text: rawDueTime.length >= 5 ? rawDueTime.substring(0, 5) : rawDueTime,
    );

    final String rawPrio = ((p['priority'] as String?) ?? 'MEDIUM').toUpperCase();
    _priority = const <String>{'LOW', 'MEDIUM', 'HIGH', 'URGENT'}.contains(rawPrio)
        ? rawPrio
        : 'MEDIUM';

    final String rawStatus = ((p['status'] as String?) ?? 'OPEN').toUpperCase();
    if (rawStatus == 'COMPLETED' || rawStatus == 'DONE') {
      _status = 'DONE';
    } else if (rawStatus == 'IN_PROGRESS') {
      _status = 'IN_PROGRESS';
    } else {
      _status = 'OPEN';
    }

    _projectId = p['projectId'] as String?;
    _goalId = p['goalId'] as String?;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _dueDateController.dispose();
    _dueTimeController.dispose();
    super.dispose();
  }

  String _formatDateIso(DateTime d) {
    final String y = d.year.toString().padLeft(4, '0');
    final String m = d.month.toString().padLeft(2, '0');
    final String day = d.day.toString().padLeft(2, '0');
    return '$y-$m-$day';
  }

  Future<void> _pickDueDate() async {
    final DateTime base =
        DateTime.tryParse(_dueDateController.text.trim()) ??
            ref.read(referenceUtcProvider).toLocal();
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: base,
      firstDate: DateTime(2020),
      lastDate: DateTime(2040),
    );
    if (picked != null && mounted) {
      setState(() => _dueDateController.text = _formatDateIso(picked));
    }
  }

  Future<void> _pickDueTime() async {
    TimeOfDay initial = const TimeOfDay(hour: 9, minute: 0);
    final String cur = _dueTimeController.text.trim();
    if (_timePattern.hasMatch(cur)) {
      final List<String> parts = cur.split(':');
      initial = TimeOfDay(
        hour: int.parse(parts[0]),
        minute: int.parse(parts[1]),
      );
    }
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: initial,
    );
    if (picked != null && mounted) {
      final String hh = picked.hour.toString().padLeft(2, '0');
      final String mm = picked.minute.toString().padLeft(2, '0');
      setState(() => _dueTimeController.text = '$hh:$mm');
    }
  }

  Future<void> _handleAiBreakdown() async {
    if (_breakingDown) return;
    setState(() => _breakingDown = true);
    try {
      final AiTaskBreakdownResponse breakdown = await ref
          .read(aiApiClientProvider)
          .breakDownTask(
            widget.task.id,
            contextDomains: const <String>['TASK_CALENDAR'],
          );
      if (!mounted) return;
      await showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (_) => AiTaskBreakdownSheet(breakdown: breakdown),
      );
    } catch (e) {
      if (!mounted) return;
      final AiErrorDescriptor err =
          ViStrings.mapAiError(e is LifeOsApiException ? e.errorCode : null);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(err.message)),
      );
    } finally {
      if (mounted) {
        setState(() => _breakingDown = false);
      }
    }
  }

  void _save() {
    final String title = _titleController.text.trim();
    if (title.isEmpty) {
      setState(() => _validationError = 'Vui lòng nhập tiêu đề công việc.');
      return;
    }
    final String dueDate = _dueDateController.text.trim();
    if (dueDate.isNotEmpty && !_isoDatePattern.hasMatch(dueDate)) {
      setState(
        () => _validationError = 'Ngày hạn cần đúng định dạng YYYY-MM-DD.',
      );
      return;
    }
    final String dueTime = _dueTimeController.text.trim();
    if (dueTime.isNotEmpty && !_timePattern.hasMatch(dueTime)) {
      setState(() => _validationError = 'Giờ hạn cần đúng định dạng HH:mm.');
      return;
    }

    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final String userId = ref.read(currentUserIdProvider);
    final String desc = _descController.text.trim();
    final bool isDone = _status == 'DONE';
    ref.read(localRepositoryProvider).updateTask(
          userId: userId,
          taskId: widget.task.id,
          nowUtc: nowUtc,
          patch: <String, dynamic>{
            'title': title,
            'description': desc.isEmpty ? null : desc,
            'priority': _priority,
            'status': _status,
            'completedAt': isDone
                ? ((widget.task.payload['completedAt'] as String?) ??
                    nowUtc.toIso8601String())
                : null,
            'dueDate': dueDate.isEmpty ? null : dueDate,
            'dueTime': dueTime.isEmpty ? null : dueTime,
            'projectId': _projectId,
            'goalId': _goalId,
          },
        );
    if (mounted) {
      Navigator.of(context).pop(true);
    }
  }

  Future<void> _confirmDelete() async {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa công việc?'),
        content: Text(
          'Công việc "${_titleController.text.trim()}" sẽ được đưa vào danh sách đã xóa.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_task'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      final String userId = ref.read(currentUserIdProvider);
      ref.read(localRepositoryProvider).deleteTask(
            userId: userId,
            taskId: widget.task.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> projects = (ref
                .watch(entitiesByTypeStreamProvider(SyncEntityType.project))
                .valueOrNull ??
            const <LocalEntityRow>[])
        .where((LocalEntityRow r) => r.payload['archived'] != true)
        .toList();
    final List<LocalEntityRow> goals = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.goal))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final AiPreferencesResponse? aiPrefs =
        ref.watch(aiPreferencesProvider).valueOrNull;
    final bool allowBreakdown = aiPrefs?.taskCalendar == true;
    final bool isOnline = ref.watch(aiIsOnlineProvider);

    final String? validProjectId = projects.any((LocalEntityRow r) => r.id == _projectId)
        ? _projectId
        : null;
    final String? validGoalId =
        goals.any((LocalEntityRow r) => r.id == _goalId) ? _goalId : null;
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 16, 20, 20 + bottomInset),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Expanded(
                    child: Text(
                      'Chi tiết công việc',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    key: const Key('btn_delete_task'),
                    tooltip: ViStrings.deleteAction,
                    onPressed: _confirmDelete,
                    icon: Icon(
                      Icons.delete_outline_rounded,
                      color: tokens.danger,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_edit_task_title'),
                controller: _titleController,
                onChanged: (_) {
                  if (_validationError != null) {
                    setState(() => _validationError = null);
                  }
                },
                decoration: InputDecoration(
                  labelText: 'Tên công việc *',
                  border: const OutlineInputBorder(),
                  errorText: _validationError,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_edit_task_desc'),
                controller: _descController,
                minLines: 2,
                maxLines: 4,
                decoration: const InputDecoration(
                  labelText: 'Ghi chú chi tiết (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Trạng thái',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: tokens.textSecondary,
                ),
              ),
              const SizedBox(height: 6),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: <MapEntry<String, String>>[
                  const MapEntry<String, String>('OPEN', 'Cần làm'),
                  const MapEntry<String, String>('IN_PROGRESS', 'Đang làm'),
                  const MapEntry<String, String>('DONE', 'Hoàn thành'),
                ].map((MapEntry<String, String> item) {
                  return ChoiceChip(
                    label: Text(item.value),
                    selected: _status == item.key,
                    onSelected: (_) => setState(() => _status = item.key),
                  );
                }).toList(),
              ),
              const SizedBox(height: 12),
              Text(
                'Mức ưu tiên',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: tokens.textSecondary,
                ),
              ),
              const SizedBox(height: 6),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: const <String>['LOW', 'MEDIUM', 'HIGH', 'URGENT']
                    .map((String prio) {
                  return ChoiceChip(
                    label: Text(ViStrings.formatTaskPriority(prio)),
                    selected: _priority == prio,
                    onSelected: (_) => setState(() => _priority = prio),
                  );
                }).toList(),
              ),
              const SizedBox(height: 12),
              Row(
                children: <Widget>[
                  Expanded(
                    flex: 3,
                    child: TextField(
                      key: const Key('input_edit_task_due_date'),
                      controller: _dueDateController,
                      decoration: InputDecoration(
                        labelText: 'Ngày hạn',
                        hintText: 'YYYY-MM-DD',
                        border: const OutlineInputBorder(),
                        suffixIcon: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: <Widget>[
                            if (_dueDateController.text.isNotEmpty)
                              IconButton(
                                tooltip: 'Xóa ngày',
                                icon: const Icon(Icons.clear_rounded, size: 18),
                                onPressed: () =>
                                    setState(() => _dueDateController.clear()),
                              ),
                            IconButton(
                              tooltip: 'Chọn ngày',
                              icon: const Icon(Icons.calendar_today_rounded,
                                  size: 18),
                              onPressed: _pickDueDate,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    flex: 2,
                    child: TextField(
                      key: const Key('input_edit_task_due_time'),
                      controller: _dueTimeController,
                      decoration: InputDecoration(
                        labelText: 'Giờ',
                        hintText: 'HH:mm',
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          tooltip: 'Chọn giờ',
                          icon: const Icon(Icons.access_time_rounded, size: 18),
                          onPressed: _pickDueTime,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (projects.isNotEmpty) ...<Widget>[
                DropdownButtonFormField<String?>(
                  initialValue: validProjectId,
                  decoration: const InputDecoration(
                    labelText: 'Dự án',
                    border: OutlineInputBorder(),
                  ),
                  items: <DropdownMenuItem<String?>>[
                    const DropdownMenuItem<String?>(
                      value: null,
                      child: Text('Không thuộc dự án'),
                    ),
                    ...projects.map(
                      (LocalEntityRow p) => DropdownMenuItem<String?>(
                        value: p.id,
                        child: Text(
                          (p.payload['name'] as String?) ?? 'Dự án',
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                  ],
                  onChanged: (String? v) => setState(() => _projectId = v),
                ),
                const SizedBox(height: 12),
              ],
              if (goals.isNotEmpty) ...<Widget>[
                DropdownButtonFormField<String?>(
                  initialValue: validGoalId,
                  decoration: const InputDecoration(
                    labelText: 'Mục tiêu liên kết',
                    border: OutlineInputBorder(),
                  ),
                  items: <DropdownMenuItem<String?>>[
                    const DropdownMenuItem<String?>(
                      value: null,
                      child: Text('Không gắn mục tiêu'),
                    ),
                    ...goals.map(
                      (LocalEntityRow g) => DropdownMenuItem<String?>(
                        value: g.id,
                        child: Text(
                          (g.payload['title'] as String?) ?? 'Mục tiêu',
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                  ],
                  onChanged: (String? v) => setState(() => _goalId = v),
                ),
                const SizedBox(height: 12),
              ],
              if (allowBreakdown) ...<Widget>[
                OutlinedButton.icon(
                  key: const Key('btn_ai_task_breakdown'),
                  style: OutlinedButton.styleFrom(
                    minimumSize:
                        const Size.fromHeight(LifeOsTokens.minTouchTarget),
                  ),
                  onPressed: isOnline && !_breakingDown
                      ? _handleAiBreakdown
                      : null,
                  icon: _breakingDown
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.auto_awesome_rounded),
                  label: Text(
                    _breakingDown
                        ? 'AI đang gợi ý các bước nhỏ...'
                        : 'AI chia nhỏ việc',
                  ),
                ),
                if (!isOnline)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      'Cần kết nối mạng để dùng Trợ lý AI.',
                      style: TextStyle(fontSize: 12, color: tokens.textMuted),
                    ),
                  ),
                const SizedBox(height: 12),
              ],
              FilledButton(
                key: const Key('btn_save_task'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _save,
                child: const Text(ViStrings.saveAction),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
