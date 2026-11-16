import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Biểu mẫu Tạo Mục tiêu dài hạn mới (`goal` + `milestone`) hỗ trợ nhiều cột mốc đồng thời.
class CreateGoalSheet extends ConsumerStatefulWidget {
  const CreateGoalSheet({super.key});

  @override
  ConsumerState<CreateGoalSheet> createState() => _CreateGoalSheetState();
}

class _CreateGoalSheetState extends ConsumerState<CreateGoalSheet> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _descController = TextEditingController();
  final TextEditingController _startDateController = TextEditingController();
  final TextEditingController _targetDateController = TextEditingController();

  // Ô nhập cột mốc đầu tiên (giữ key cho tương thích ngược)
  final TextEditingController _firstMilestoneController =
      TextEditingController();

  // Danh sách các cột mốc bổ sung cho mục tiêu dài hạn
  final List<GoalMilestoneDraft> _milestoneDrafts = <GoalMilestoneDraft>[];
  final TextEditingController _extraMilestoneTitleController =
      TextEditingController();
  final TextEditingController _extraMilestoneDueController =
      TextEditingController();

  final FocusNode _titleFocus = FocusNode();
  String? _titleError;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);
    final String todayIso =
        ref.read(localRepositoryProvider).getUserDayClock(userId).businessDateForUtc(refUtc);
    _startDateController.text = todayIso;
    final DateTime plus90 = DateTime.parse(todayIso).add(const Duration(days: 90));
    _targetDateController.text =
        '${plus90.year.toString().padLeft(4, '0')}-${plus90.month.toString().padLeft(2, '0')}-${plus90.day.toString().padLeft(2, '0')}';

    _titleFocus.addListener(() {
      if (!_titleFocus.hasFocus && mounted) {
        setState(() => _titleError = _titleController.text.trim().isEmpty
            ? 'Vui lòng nhập tên mục tiêu.'
            : null);
      }
    });
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _startDateController.dispose();
    _targetDateController.dispose();
    _firstMilestoneController.dispose();
    _extraMilestoneTitleController.dispose();
    _extraMilestoneDueController.dispose();
    _titleFocus.dispose();
    super.dispose();
  }

  void _addExtraMilestone() {
    final String title = _extraMilestoneTitleController.text.trim();
    if (title.isEmpty) return;
    setState(() {
      _milestoneDrafts.add(
        GoalMilestoneDraft(
          title: title,
          dueOnIso: _extraMilestoneDueController.text.trim().isEmpty
              ? null
              : _extraMilestoneDueController.text.trim(),
        ),
      );
      _extraMilestoneTitleController.clear();
      _extraMilestoneDueController.clear();
    });
  }

  void _submitCreateGoal() {
    if (_titleController.text.trim().isEmpty) {
      setState(() => _titleError = 'Vui lòng nhập tên mục tiêu.');
      return;
    }
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);

    final String firstMs = _firstMilestoneController.text.trim();
    final List<GoalMilestoneDraft> allMilestones = <GoalMilestoneDraft>[
      if (firstMs.isNotEmpty)
        GoalMilestoneDraft(
          title: firstMs,
          dueOnIso: _targetDateController.text.trim(),
        ),
      ..._milestoneDrafts,
    ];

    repo.createGoal(
      userId: userId,
      title: _titleController.text.trim(),
      description: _descController.text.trim().isEmpty ? null : _descController.text.trim(),
      startDateIso: _startDateController.text.trim().isEmpty
          ? repo.getUserDayClock(userId).businessDateForUtc(refUtc)
          : _startDateController.text.trim(),
      targetDateIso: _targetDateController.text.trim(),
      milestoneDrafts: allMilestones,
      nowUtc: refUtc,
    );
    if (mounted && Navigator.canPop(context)) {
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
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
              Text(
                'Thiết lập mục tiêu mới',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_create_goal_title'),
                controller: _titleController,
                focusNode: _titleFocus,
                decoration: InputDecoration(
                  labelText: 'Tên mục tiêu *',
                  hintText: 'Ví dụ: Đạt chứng chỉ JLPT N2, Mua căn hộ...',
                  errorText: _titleError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_create_goal_description'),
                controller: _descController,
                maxLines: 2,
                decoration: const InputDecoration(
                  labelText: 'Mô tả / Ý nghĩa mục tiêu (tùy chọn)',
                  hintText: 'Lý do thực hiện và kết quả kỳ vọng...',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      key: const Key('input_create_goal_start_date'),
                      controller: _startDateController,
                      decoration: const InputDecoration(
                        labelText: 'Ngày bắt đầu *',
                        hintText: 'YYYY-MM-DD',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextField(
                      key: const Key('input_create_goal_target_date'),
                      controller: _targetDateController,
                      decoration: const InputDecoration(
                        labelText: 'Ngày hoàn thành *',
                        hintText: 'YYYY-MM-DD',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Phần Cột mốc: "mục tiêu dài hạn phải có thêm cột mốc"
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: tokens.domainGoal.withValues(alpha: 0.05),
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  border: Border.all(color: tokens.domainGoal.withValues(alpha: 0.2)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Icon(Icons.flag_rounded, size: 18, color: tokens.domainGoal),
                        const SizedBox(width: 8),
                        Text(
                          'Cột mốc mục tiêu dài hạn (${_milestoneDrafts.length + (_firstMilestoneController.text.trim().isNotEmpty ? 1 : 0)})',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            color: tokens.textPrimary,
                            fontSize: 13.5,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    // Cột mốc đầu tiên
                    TextField(
                      key: const Key('input_create_goal_first_milestone'),
                      controller: _firstMilestoneController,
                      onChanged: (_) => setState(() {}),
                      decoration: const InputDecoration(
                        labelText: 'Cột mốc 1 (cột mốc khởi đầu)',
                        hintText: 'Ví dụ: Hoàn thành giáo trình cơ bản...',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                    // Danh sách các cột mốc đã thêm
                    if (_milestoneDrafts.isNotEmpty) ...<Widget>[
                      const SizedBox(height: 8),
                      ..._milestoneDrafts.asMap().entries.map((MapEntry<int, GoalMilestoneDraft> entry) {
                        final int idx = entry.key;
                        final GoalMilestoneDraft ms = entry.value;
                        return Container(
                          margin: const EdgeInsets.only(top: 6),
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: tokens.bgSurface,
                            borderRadius: BorderRadius.circular(LifeOsTokens.radiusSm),
                            border: Border.all(color: tokens.borderSubtle),
                          ),
                          child: Row(
                            children: <Widget>[
                              Text(
                                '${idx + 2}.',
                                style: TextStyle(
                                  fontWeight: FontWeight.w700,
                                  color: tokens.domainGoal,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  ms.title,
                                  style: TextStyle(color: tokens.textPrimary, fontSize: 13),
                                ),
                              ),
                              if (ms.dueOnIso != null) ...<Widget>[
                                Text(
                                  ms.dueOnIso!,
                                  style: TextStyle(color: tokens.textSecondary, fontSize: 11),
                                ),
                                const SizedBox(width: 6),
                              ],
                              InkWell(
                                onTap: () => setState(() => _milestoneDrafts.removeAt(idx)),
                                child: Icon(Icons.close_rounded, size: 16, color: tokens.danger),
                              ),
                            ],
                          ),
                        );
                      }),
                    ],
                    const SizedBox(height: 10),
                    // Nhập thêm cột mốc bổ sung
                    Row(
                      children: <Widget>[
                        Expanded(
                          flex: 3,
                          child: TextField(
                            key: const Key('input_create_goal_extra_milestone_title'),
                            controller: _extraMilestoneTitleController,
                            decoration: const InputDecoration(
                              labelText: 'Tên cột mốc tiếp theo',
                              hintText: 'Cột mốc 2, 3...',
                              border: OutlineInputBorder(),
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          flex: 2,
                          child: TextField(
                            key: const Key('input_create_goal_extra_milestone_due'),
                            controller: _extraMilestoneDueController,
                            decoration: const InputDecoration(
                              labelText: 'Hạn (tùy chọn)',
                              hintText: 'YYYY-MM-DD',
                              border: OutlineInputBorder(),
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        IconButton.filledTonal(
                          key: const Key('btn_add_draft_milestone'),
                          tooltip: 'Thêm cột mốc vào danh sách',
                          onPressed: _addExtraMilestone,
                          icon: const Icon(Icons.add_rounded, size: 20),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_submit_create_goal'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _submitCreateGoal,
                child: const Text('Tạo mục tiêu'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Biểu mẫu Chỉnh sửa Mục tiêu dài hạn (`goal`) kèm Quản lý Cột mốc (`milestone`).
class GoalEditorSheet extends ConsumerStatefulWidget {
  const GoalEditorSheet({
    super.key,
    required this.goal,
  });

  final LocalEntityRow goal;

  @override
  ConsumerState<GoalEditorSheet> createState() => _GoalEditorSheetState();
}

class _GoalEditorSheetState extends ConsumerState<GoalEditorSheet> {
  late final TextEditingController _titleController;
  late final TextEditingController _descController;
  late final TextEditingController _startDateController;
  late final TextEditingController _targetDateController;
  late String _status;

  final TextEditingController _newMilestoneTitleController =
      TextEditingController();
  final TextEditingController _newMilestoneDueController =
      TextEditingController();

  final FocusNode _titleFocus = FocusNode();
  String? _titleError;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic> p = widget.goal.payload;
    _titleController = TextEditingController(text: (p['title'] as String?) ?? '');
    _descController = TextEditingController(text: (p['description'] as String?) ?? '');
    _startDateController = TextEditingController(
      text: ((p['startOn'] ?? p['startDate']) as String?) ?? '',
    );
    _targetDateController = TextEditingController(
      text: ((p['targetOn'] ?? p['targetDate']) as String?) ?? '',
    );
    _status = ((p['status'] as String?) ?? 'ACTIVE').toUpperCase();

    _titleFocus.addListener(() {
      if (!_titleFocus.hasFocus && mounted) {
        setState(() {
          _titleError = _titleController.text.trim().isEmpty
              ? 'Vui lòng nhập tên mục tiêu.'
              : null;
        });
      }
    });
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _startDateController.dispose();
    _targetDateController.dispose();
    _newMilestoneTitleController.dispose();
    _newMilestoneDueController.dispose();
    _titleFocus.dispose();
    super.dispose();
  }

  Future<void> _confirmDeleteGoal() async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa mục tiêu dài hạn?'),
        content: const Text(
          'Hành động này sẽ xóa mục tiêu và toàn bộ cột mốc kèm theo khỏi hệ thống. Không thể hoàn tác.',
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

    if (ok == true && mounted) {
      final String userId = ref.read(currentUserIdProvider);
      ref.read(localRepositoryProvider).deleteGoal(
            userId: userId,
            goalId: widget.goal.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      if (mounted && Navigator.canPop(context)) {
      Navigator.of(context).pop(true);
    }
    }
  }

  void _saveGoal() {
    final String title = _titleController.text.trim();
    if (title.isEmpty) {
      setState(() => _titleError = 'Vui lòng nhập tên mục tiêu.');
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);

    repo.updateGoal(
      userId: userId,
      goalId: widget.goal.id,
      title: title,
      description: _descController.text.trim().isEmpty ? null : _descController.text.trim(),
      category: widget.goal.payload['category'] as String?,
      startDateIso: _startDateController.text.trim(),
      targetDateIso: _targetDateController.text.trim(),
      status: _status,
      nowUtc: ref.read(referenceUtcProvider),
    );

    if (mounted && Navigator.canPop(context)) {
      Navigator.of(context).pop(true);
    }
  }

  Future<void> _editMilestoneDialog(LocalEntityRow ms) async {
    final TextEditingController titleCtrl = TextEditingController(
      text: (ms.payload['title'] as String?) ?? '',
    );
    final TextEditingController dueCtrl = TextEditingController(
      text: (ms.payload['dueOn'] as String?) ?? '',
    );
    final bool? saved = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Sửa cột mốc'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            TextField(
              controller: titleCtrl,
              decoration: const InputDecoration(
                labelText: 'Tên cột mốc *',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: dueCtrl,
              decoration: const InputDecoration(
                labelText: 'Hạn hoàn thành (tùy chọn)',
                hintText: 'YYYY-MM-DD',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            onPressed: () {
              if (titleCtrl.text.trim().isEmpty) return;
              Navigator.of(ctx).pop(true);
            },
            child: const Text(ViStrings.saveAction),
          ),
        ],
      ),
    );
    if (saved == true && mounted) {
      final String userId = ref.read(currentUserIdProvider);
      ref.read(localRepositoryProvider).knowledge.updateMilestone(
            userId: userId,
            milestoneId: ms.id,
            title: titleCtrl.text,
            dueOnIso: dueCtrl.text,
            nowUtc: ref.read(referenceUtcProvider),
          );
    }
    titleCtrl.dispose();
    dueCtrl.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    final List<LocalEntityRow> allMilestones = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.goalMilestone))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> milestones = allMilestones
        .where((LocalEntityRow m) => m.payload['goalId'] == widget.goal.id)
        .toList()
      ..sort(
        (LocalEntityRow a, LocalEntityRow b) =>
            ((a.payload['position'] as num?)?.toInt() ?? 0)
                .compareTo((b.payload['position'] as num?)?.toInt() ?? 0),
      );

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
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Text(
                    'Chỉnh sửa mục tiêu',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  IconButton(
                    key: const Key('btn_delete_goal'),
                    tooltip: 'Xóa mục tiêu',
                    onPressed: _confirmDeleteGoal,
                    icon: Icon(
                      Icons.delete_outline_rounded,
                      color: tokens.danger,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),

              // Title
              TextField(
                key: const Key('input_edit_goal_title'),
                controller: _titleController,
                focusNode: _titleFocus,
                decoration: InputDecoration(
                  labelText: 'Tên mục tiêu *',
                  errorText: _titleError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),

              // Description
              TextField(
                key: const Key('input_edit_goal_desc'),
                controller: _descController,
                maxLines: 2,
                decoration: const InputDecoration(
                  labelText: 'Mô tả mục tiêu (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),

              // Dates
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      key: const Key('input_edit_goal_start_date'),
                      controller: _startDateController,
                      decoration: const InputDecoration(
                        labelText: 'Ngày bắt đầu *',
                        hintText: 'YYYY-MM-DD',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextField(
                      key: const Key('input_edit_goal_target_date'),
                      controller: _targetDateController,
                      decoration: const InputDecoration(
                        labelText: 'Ngày hoàn thành *',
                        hintText: 'YYYY-MM-DD',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Status Segmented Button
              Text(
                'Trạng thái mục tiêu',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: tokens.textSecondary,
                ),
              ),
              const SizedBox(height: 6),
              SegmentedButton<String>(
                key: const Key('segmented_goal_status'),
                segments: const <ButtonSegment<String>>[
                  ButtonSegment<String>(
                    value: 'ACTIVE',
                    label: Text('Đang chạy'),
                    icon: Icon(Icons.play_arrow_rounded, size: 16),
                  ),
                  ButtonSegment<String>(
                    value: 'COMPLETED',
                    label: Text('Hoàn thành'),
                    icon: Icon(Icons.check_circle_outline_rounded, size: 16),
                  ),
                  ButtonSegment<String>(
                    value: 'ABANDONED',
                    label: Text('Tạm ngưng'),
                    icon: Icon(Icons.pause_circle_outline_rounded, size: 16),
                  ),
                ],
                selected: <String>{_status},
                onSelectionChanged: (Set<String> s) => setState(() => _status = s.first),
              ),

              const SizedBox(height: 16),
              const Divider(),

              // Milestones Section embedded
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Icon(Icons.checklist_rounded, size: 18, color: tokens.domainGoal),
                      const SizedBox(width: 8),
                      Text(
                        'Cột mốc mục tiêu (${milestones.length})',
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: tokens.textPrimary,
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 8),

              if (milestones.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 4),
                  child: Text(
                    'Chưa có cột mốc nào. Thêm cột mốc bên dưới để theo dõi tiến độ.',
                    style: TextStyle(fontSize: 12.5, color: Colors.grey),
                  ),
                )
              else
                ...milestones.map((LocalEntityRow m) {
                  final bool isDone = (m.payload['completed'] == true) ||
                      (m.payload['status']?.toString().toUpperCase() == 'DONE');
                  final String? dueOn = m.payload['dueOn'] as String?;
                  return ListTile(
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                    leading: Checkbox(
                      value: isDone,
                      onChanged: (_) => repo.knowledge.toggleMilestoneCompletion(
                        userId: userId,
                        milestoneId: m.id,
                        nowUtc: ref.read(referenceUtcProvider),
                      ),
                    ),
                    title: Text(
                      (m.payload['title'] as String?) ?? 'Cột mốc',
                      style: TextStyle(
                        decoration: isDone ? TextDecoration.lineThrough : null,
                        color: tokens.textPrimary,
                        fontSize: 13.5,
                      ),
                    ),
                    subtitle: dueOn != null && dueOn.isNotEmpty
                        ? Text('Hạn: $dueOn', style: const TextStyle(fontSize: 11))
                        : null,
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        IconButton(
                          tooltip: 'Sửa cột mốc',
                          icon: const Icon(Icons.edit_outlined, size: 18),
                          onPressed: () => _editMilestoneDialog(m),
                        ),
                        IconButton(
                          tooltip: 'Xóa cột mốc',
                          icon: Icon(
                            Icons.delete_outline_rounded,
                            size: 18,
                            color: tokens.danger,
                          ),
                          onPressed: () => repo.knowledge.deleteMilestone(
                            userId: userId,
                            milestoneId: m.id,
                            nowUtc: ref.read(referenceUtcProvider),
                          ),
                        ),
                      ],
                    ),
                  );
                }),

              const SizedBox(height: 10),

              // Inline add milestone
              Row(
                children: <Widget>[
                  Expanded(
                    flex: 3,
                    child: TextField(
                      key: const Key('input_edit_goal_new_milestone_title'),
                      controller: _newMilestoneTitleController,
                      decoration: const InputDecoration(
                        labelText: 'Thêm cột mốc mới',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    flex: 2,
                    child: TextField(
                      key: const Key('input_edit_goal_new_milestone_due'),
                      controller: _newMilestoneDueController,
                      decoration: const InputDecoration(
                        labelText: 'Hạn (tùy chọn)',
                        hintText: 'YYYY-MM-DD',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  IconButton.filledTonal(
                    key: const Key('btn_edit_goal_add_milestone'),
                    tooltip: 'Thêm cột mốc',
                    icon: const Icon(Icons.add_rounded),
                    onPressed: () {
                      final String t = _newMilestoneTitleController.text.trim();
                      if (t.isEmpty) return;
                      repo.knowledge.addMilestone(
                        userId: userId,
                        goalId: widget.goal.id,
                        title: t,
                        dueOnIso: _newMilestoneDueController.text.trim(),
                        nowUtc: ref.read(referenceUtcProvider),
                      );
                      _newMilestoneTitleController.clear();
                      _newMilestoneDueController.clear();
                    },
                  ),
                ],
              ),

              const SizedBox(height: 20),

              FilledButton(
                key: const Key('btn_save_goal_changes'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _saveGoal,
                child: const Text('Lưu thay đổi'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet quản lý Cột mốc của một Mục tiêu (`milestone`: thêm / sửa / đánh dấu / xóa) (`MWP-C`).
class GoalMilestonesSheet extends ConsumerStatefulWidget {
  const GoalMilestonesSheet({
    super.key,
    required this.goal,
  });

  final LocalEntityRow goal;

  @override
  ConsumerState<GoalMilestonesSheet> createState() =>
      _GoalMilestonesSheetState();
}

class _GoalMilestonesSheetState extends ConsumerState<GoalMilestonesSheet> {
  final TextEditingController _newTitleController = TextEditingController();
  final TextEditingController _newDueController = TextEditingController();

  @override
  void dispose() {
    _newTitleController.dispose();
    _newDueController.dispose();
    super.dispose();
  }

  Future<void> _editMilestoneDialog(LocalEntityRow ms) async {
    final TextEditingController titleCtrl = TextEditingController(
      text: (ms.payload['title'] as String?) ?? '',
    );
    final TextEditingController dueCtrl = TextEditingController(
      text: (ms.payload['dueOn'] as String?) ?? '',
    );
    final bool? saved = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Sửa cột mốc'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            TextField(
              key: const Key('input_edit_milestone_title'),
              controller: titleCtrl,
              decoration: const InputDecoration(
                labelText: 'Tên cột mốc *',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_edit_milestone_due'),
              controller: dueCtrl,
              decoration: const InputDecoration(
                labelText: 'Hạn hoàn thành (tùy chọn)',
                hintText: 'YYYY-MM-DD',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_edit_milestone'),
            onPressed: () {
              if (titleCtrl.text.trim().isEmpty) return;
              Navigator.of(ctx).pop(true);
            },
            child: const Text(ViStrings.saveAction),
          ),
        ],
      ),
    );
    if (saved == true && mounted) {
      final String userId = ref.read(currentUserIdProvider);
      ref.read(localRepositoryProvider).knowledge.updateMilestone(
            userId: userId,
            milestoneId: ms.id,
            title: titleCtrl.text,
            dueOnIso: dueCtrl.text,
            nowUtc: ref.read(referenceUtcProvider),
          );
    }
    titleCtrl.dispose();
    dueCtrl.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final List<LocalEntityRow> allMilestones = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.goalMilestone))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> milestones = allMilestones
        .where((LocalEntityRow m) => m.payload['goalId'] == widget.goal.id)
        .toList()
      ..sort(
        (LocalEntityRow a, LocalEntityRow b) =>
            ((a.payload['position'] as num?)?.toInt() ?? 0)
                .compareTo((b.payload['position'] as num?)?.toInt() ?? 0),
      );
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
            Text(
              'Cột mốc · ${(widget.goal.payload['title'] as String?) ?? 'Mục tiêu'}',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 10),
            if (milestones.isEmpty)
              const LifeOsEmptyState(
                icon: Icons.flag_outlined,
                title: 'Chưa có cột mốc nào',
                subtitle: 'Thêm cột mốc cụ thể bên dưới để theo dõi tiến độ.',
              )
            else
              ...milestones.map((LocalEntityRow m) {
                final bool isDone = (m.payload['completed'] == true) ||
                    (m.payload['status']?.toString().toUpperCase() == 'DONE');
                final String? dueOn = m.payload['dueOn'] as String?;
                return ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Checkbox(
                    value: isDone,
                    onChanged: (_) => repo.knowledge.toggleMilestoneCompletion(
                      userId: userId,
                      milestoneId: m.id,
                      nowUtc: ref.read(referenceUtcProvider),
                    ),
                  ),
                  title: Text(
                    (m.payload['title'] as String?) ?? 'Cột mốc',
                    style: TextStyle(
                      decoration: isDone ? TextDecoration.lineThrough : null,
                      color: tokens.textPrimary,
                    ),
                  ),
                  subtitle: dueOn != null && dueOn.isNotEmpty
                      ? Text('Hạn: $dueOn')
                      : null,
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      IconButton(
                        tooltip: 'Sửa cột mốc',
                        onPressed: () => _editMilestoneDialog(m),
                        icon: const Icon(Icons.edit_outlined, size: 20),
                      ),
                      IconButton(
                        tooltip: 'Xóa cột mốc',
                        onPressed: () => repo.knowledge.deleteMilestone(
                          userId: userId,
                          milestoneId: m.id,
                          nowUtc: ref.read(referenceUtcProvider),
                        ),
                        icon: Icon(
                          Icons.delete_outline_rounded,
                          size: 20,
                          color: tokens.danger,
                        ),
                      ),
                    ],
                  ),
                );
              }),
            const Divider(),
            Text(
              'Thêm cột mốc mới',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: tokens.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              key: const Key('input_new_milestone_title'),
              controller: _newTitleController,
              decoration: const InputDecoration(
                labelText: 'Tên cột mốc *',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              key: const Key('input_new_milestone_due'),
              controller: _newDueController,
              decoration: const InputDecoration(
                labelText: 'Ngày hạn (tùy chọn)',
                hintText: 'YYYY-MM-DD',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            FilledButton.icon(
              key: const Key('btn_add_milestone'),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
              ),
              onPressed: () {
                if (_newTitleController.text.trim().isEmpty) return;
                repo.knowledge.addMilestone(
                  userId: userId,
                  goalId: widget.goal.id,
                  title: _newTitleController.text,
                  dueOnIso: _newDueController.text,
                  nowUtc: ref.read(referenceUtcProvider),
                );
                _newTitleController.clear();
                _newDueController.clear();
              },
              icon: const Icon(Icons.add_rounded),
              label: const Text('Thêm cột mốc'),
            ),
          ],
        ),
      ),
      ),
    );
  }
}
