import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/database_records.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';

/// Sheet tạo mới hoặc chỉnh sửa / lưu trữ / xóa Dự án (`project`).
class ProjectEditorSheet extends ConsumerStatefulWidget {
  const ProjectEditorSheet({
    super.key,
    this.project,
    this.taskCount = 0,
  });

  final LocalEntityRow? project;
  final int taskCount;

  @override
  ConsumerState<ProjectEditorSheet> createState() => _ProjectEditorSheetState();
}

class _ProjectEditorSheetState extends ConsumerState<ProjectEditorSheet> {
  late final TextEditingController _nameController;
  late final TextEditingController _descController;
  late bool _archived;
  String? _errorText;

  bool get _isEditing => widget.project != null;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic> p =
        widget.project?.payload ?? const <String, dynamic>{};
    _nameController =
        TextEditingController(text: (p['name'] as String?) ?? '');
    _descController =
        TextEditingController(text: (p['description'] as String?) ?? '');
    _archived = p['archived'] == true || p['archivedAt'] != null;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descController.dispose();
    super.dispose();
  }

  void _save() {
    final String name = _nameController.text.trim();
    if (name.isEmpty) {
      setState(() => _errorText = 'Vui lòng nhập tên dự án.');
      return;
    }
    final String userId = ref.read(currentUserIdProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final repo = ref.read(localRepositoryProvider);
    if (_isEditing) {
      repo.updateProject(
        userId: userId,
        projectId: widget.project!.id,
        name: name,
        description: _descController.text,
        archived: _archived,
        nowUtc: nowUtc,
      );
    } else {
      repo.createProject(
        userId: userId,
        name: name,
        description: _descController.text.trim().isEmpty
            ? null
            : _descController.text.trim(),
        nowUtc: nowUtc,
      );
    }
    if (mounted) Navigator.of(context).pop(true);
  }

  Future<void> _confirmDelete() async {
    if (!_isEditing) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text(ViStrings.confirmDeleteTitle),
        content: Text(
          ViStrings.confirmDeleteProjectMessage(
            _nameController.text.trim(),
            widget.taskCount,
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_project'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      ref.read(localRepositoryProvider).deleteProject(
            userId: ref.read(currentUserIdProvider),
            projectId: widget.project!.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
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
              Row(
                children: <Widget>[
                  Expanded(
                    child: Text(
                      _isEditing ? 'Chỉnh sửa dự án' : 'Tạo dự án mới',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  if (_isEditing)
                    IconButton(
                      key: const Key('btn_delete_project'),
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
                key: const Key('input_project_name'),
                controller: _nameController,
                onChanged: (_) {
                  if (_errorText != null) setState(() => _errorText = null);
                },
                decoration: InputDecoration(
                  labelText: 'Tên dự án *',
                  border: const OutlineInputBorder(),
                  errorText: _errorText,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_project_desc'),
                controller: _descController,
                minLines: 2,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Mô tả (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              if (_isEditing) ...<Widget>[
                const SizedBox(height: 8),
                SwitchListTile(
                  key: const Key('switch_archive_project'),
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Lưu trữ dự án'),
                  value: _archived,
                  onChanged: (bool val) => setState(() => _archived = val),
                ),
              ],
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_save_project'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _save,
                child: Text(_isEditing ? 'Lưu thay đổi' : 'Tạo dự án'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet tạo mới hoặc sửa Sự kiện lịch (`calendar_event`) kèm xử lý lặp & ngoại lệ từng buổi.
class CalendarEventSheet extends ConsumerStatefulWidget {
  const CalendarEventSheet({
    super.key,
    this.event,
    this.initialDateIso,
    this.occurrenceStartLocal,
  });

  final LocalEntityRow? event;
  final String? initialDateIso;
  final String? occurrenceStartLocal;

  @override
  ConsumerState<CalendarEventSheet> createState() =>
      _CalendarEventSheetState();
}

class _CalendarEventSheetState extends ConsumerState<CalendarEventSheet> {
  static final RegExp _isoDatePattern = RegExp(r'^\d{4}-\d{2}-\d{2}$');
  static final RegExp _timePattern = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');

  late final TextEditingController _titleController;
  late final TextEditingController _locationController;
  late final TextEditingController _descController;
  late final TextEditingController _dateController;
  late final TextEditingController _startTimeController;
  late final TextEditingController _endTimeController;
  late final TextEditingController _untilController;

  late bool _allDay;
  late String _freq; // NONE | DAILY | WEEKLY | MONTHLY
  String _editScope = 'SERIES'; // SERIES | THIS_OCCURRENCE | FROM_THIS
  String? _errorText;

  bool get _isEditing => widget.event != null;
  bool get _isRecurringOccurrence =>
      _isEditing &&
      widget.occurrenceStartLocal != null &&
      widget.event!.payload['recurrence'] is Map;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final String fallbackDate = widget.initialDateIso ??
        ref
            .read(localRepositoryProvider)
            .getUserDayClock(userId)
            .businessDateForUtc(ref.read(referenceUtcProvider));

    if (_isEditing) {
      final Map<String, dynamic> p = widget.event!.payload;
      _titleController =
          TextEditingController(text: (p['title'] as String?) ?? '');
      _locationController =
          TextEditingController(text: (p['location'] as String?) ?? '');
      _descController =
          TextEditingController(text: (p['description'] as String?) ?? '');
      _allDay = p['allDay'] == true;

      final String rawStart = (widget.occurrenceStartLocal ??
              p['startLocal'] as String? ??
              '${fallbackDate}T09:00:00')
          .toString();
      final String rawEnd =
          (p['endLocal'] as String? ?? '${fallbackDate}T10:00:00').toString();
      final String datePart = rawStart.length >= 10
          ? rawStart.substring(0, 10)
          : fallbackDate;
      _dateController = TextEditingController(
        text: (p['startDate'] as String?) ?? datePart,
      );
      _startTimeController = TextEditingController(
        text: rawStart.contains('T') && rawStart.length >= 16
            ? rawStart.split('T').last.substring(0, 5)
            : '09:00',
      );
      _endTimeController = TextEditingController(
        text: rawEnd.contains('T') && rawEnd.length >= 16
            ? rawEnd.split('T').last.substring(0, 5)
            : '10:00',
      );

      final Object? rec = p['recurrence'];
      final String rawFreq =
          rec is Map ? ((rec['freq'] as String?) ?? 'NONE').toUpperCase() : 'NONE';
      _freq = const <String>{'DAILY', 'WEEKLY', 'MONTHLY'}.contains(rawFreq)
          ? rawFreq
          : 'NONE';
      _untilController = TextEditingController(
        text: (p['recurrenceUntil'] as String?) ??
            (rec is Map ? (rec['until'] as String? ?? '') : ''),
      );
      if (_isRecurringOccurrence) {
        _editScope = 'THIS_OCCURRENCE';
      }
    } else {
      _titleController = TextEditingController();
      _locationController = TextEditingController();
      _descController = TextEditingController();
      _allDay = false;
      _dateController = TextEditingController(text: fallbackDate);
      _startTimeController = TextEditingController(text: '09:00');
      _endTimeController = TextEditingController(text: '10:00');
      _freq = 'NONE';
      _untilController = TextEditingController();
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _locationController.dispose();
    _descController.dispose();
    _dateController.dispose();
    _startTimeController.dispose();
    _endTimeController.dispose();
    _untilController.dispose();
    super.dispose();
  }

  Future<void> _pickDate(TextEditingController ctrl) async {
    final DateTime base = DateTime.tryParse(ctrl.text.trim()) ??
        ref.read(referenceUtcProvider).toLocal();
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: base,
      firstDate: DateTime(2020),
      lastDate: DateTime(2040),
    );
    if (picked != null && mounted) {
      final String y = picked.year.toString().padLeft(4, '0');
      final String m = picked.month.toString().padLeft(2, '0');
      final String d = picked.day.toString().padLeft(2, '0');
      setState(() => ctrl.text = '$y-$m-$d');
    }
  }

  Future<void> _pickTime(TextEditingController ctrl) async {
    TimeOfDay initial = const TimeOfDay(hour: 9, minute: 0);
    if (_timePattern.hasMatch(ctrl.text.trim())) {
      final List<String> parts = ctrl.text.trim().split(':');
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
      setState(() => ctrl.text = '$hh:$mm');
    }
  }

  void _save() {
    final String title = _titleController.text.trim();
    if (title.isEmpty) {
      setState(() => _errorText = 'Vui lòng nhập tiêu đề sự kiện.');
      return;
    }
    final String dateIso = _dateController.text.trim();
    if (!_isoDatePattern.hasMatch(dateIso)) {
      setState(() => _errorText = 'Ngày cần đúng định dạng YYYY-MM-DD.');
      return;
    }
    final String startHm = _startTimeController.text.trim();
    final String endHm = _endTimeController.text.trim();
    if (!_allDay) {
      if (!_timePattern.hasMatch(startHm) || !_timePattern.hasMatch(endHm)) {
        setState(() => _errorText = 'Giờ cần đúng định dạng HH:mm.');
        return;
      }
      if (endHm.compareTo(startHm) <= 0) {
        setState(() => _errorText = 'Giờ kết thúc phải sau giờ bắt đầu.');
        return;
      }
    }
    final String until = _untilController.text.trim();
    if (until.isNotEmpty && !_isoDatePattern.hasMatch(until)) {
      setState(
        () => _errorText = 'Ngày kết thúc lặp cần đúng định dạng YYYY-MM-DD.',
      );
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final repo = ref.read(localRepositoryProvider);
    final String startLocal =
        _allDay ? '${dateIso}T00:00:00' : '${dateIso}T$startHm:00';
    final String endLocal =
        _allDay ? '${dateIso}T23:59:00' : '${dateIso}T$endHm:00';

    Map<String, dynamic>? recurrenceMap;
    if (_freq != 'NONE') {
      recurrenceMap = <String, dynamic>{
        'freq': _freq,
        'interval': 1,
        if (_freq == 'WEEKLY')
          'byWeekday': <int>[DateTime.parse(dateIso).weekday],
        if (until.isNotEmpty) 'until': '${until}T23:59:59Z',
      };
    }

    if (!_isEditing) {
      repo.createCalendarEvent(
        userId: userId,
        title: title,
        description: _descController.text.trim().isEmpty
            ? null
            : _descController.text.trim(),
        location: _locationController.text.trim().isEmpty
            ? null
            : _locationController.text.trim(),
        allDay: _allDay,
        startLocal: startLocal,
        endLocal: endLocal,
        startDate: _allDay ? dateIso : null,
        endDate: _allDay ? dateIso : null,
        recurrence: recurrenceMap,
        nowUtc: nowUtc,
      );
    } else if (_isRecurringOccurrence && _editScope == 'THIS_OCCURRENCE') {
      repo.recordEventOccurrenceException(
        userId: userId,
        eventId: widget.event!.id,
        originalStartLocal: widget.occurrenceStartLocal!,
        kind: 'MODIFIED',
        title: title,
        description: _descController.text.trim(),
        location: _locationController.text.trim(),
        startLocal: startLocal,
        endLocal: endLocal,
        nowUtc: nowUtc,
      );
    } else if (_isRecurringOccurrence && _editScope == 'FROM_THIS') {
      repo.splitRecurringEventFrom(
        userId: userId,
        originalEventId: widget.event!.id,
        originalStartLocal: widget.occurrenceStartLocal!,
        newTitle: title,
        newStartLocal: startLocal,
        newEndLocal: endLocal,
        newRecurrence: recurrenceMap,
        nowUtc: nowUtc,
      );
    } else {
      repo.updateCalendarEvent(
        userId: userId,
        eventId: widget.event!.id,
        title: title,
        description: _descController.text,
        location: _locationController.text,
        allDay: _allDay,
        startLocal: startLocal,
        endLocal: endLocal,
        startDate: _allDay ? dateIso : '',
        endDate: _allDay ? dateIso : '',
        recurrence: recurrenceMap,
        clearRecurrence: _freq == 'NONE',
        recurrenceUntil: until,
        nowUtc: nowUtc,
      );
    }
    if (mounted) Navigator.of(context).pop(true);
  }

  Future<void> _confirmDelete() async {
    if (!_isEditing) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool cancelThisOnly =
        _isRecurringOccurrence && _editScope == 'THIS_OCCURRENCE';
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: Text(
          cancelThisOnly ? 'Hủy riêng buổi này?' : 'Xóa sự kiện lịch?',
        ),
        content: Text(
          cancelThisOnly
              ? 'Chỉ lần diễn ra ngày ${_dateController.text} sẽ bị hủy, các buổi khác được giữ nguyên.'
              : 'Sự kiện "${_titleController.text.trim()}" sẽ bị xóa khỏi lịch của bạn.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_event'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: Text(cancelThisOnly ? 'Hủy buổi này' : ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      final String userId = ref.read(currentUserIdProvider);
      final DateTime nowUtc = ref.read(referenceUtcProvider);
      final repo = ref.read(localRepositoryProvider);
      if (cancelThisOnly) {
        repo.recordEventOccurrenceException(
          userId: userId,
          eventId: widget.event!.id,
          originalStartLocal: widget.occurrenceStartLocal!,
          kind: 'CANCELLED',
          nowUtc: nowUtc,
        );
      } else {
        repo.deleteCalendarEvent(
          userId: userId,
          eventId: widget.event!.id,
          nowUtc: nowUtc,
        );
      }
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
              Row(
                children: <Widget>[
                  Expanded(
                    child: Text(
                      _isEditing ? 'Chi tiết sự kiện' : 'Thêm sự kiện lịch',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  if (_isEditing)
                    IconButton(
                      key: const Key('btn_delete_event'),
                      tooltip: ViStrings.deleteAction,
                      onPressed: _confirmDelete,
                      icon: Icon(
                        Icons.delete_outline_rounded,
                        color: tokens.danger,
                      ),
                    ),
                ],
              ),
              if (_isRecurringOccurrence) ...<Widget>[
                const SizedBox(height: 8),
                Text(
                  'Phạm vi áp dụng cho sự kiện lặp',
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: tokens.textSecondary,
                  ),
                ),
                const SizedBox(height: 6),
                Wrap(
                  spacing: 6,
                  children: const <MapEntry<String, String>>[
                    MapEntry<String, String>('THIS_OCCURRENCE', 'Chỉ buổi này'),
                    MapEntry<String, String>('FROM_THIS', 'Từ buổi này trở đi'),
                    MapEntry<String, String>('SERIES', 'Toàn bộ chuỗi'),
                  ].map((MapEntry<String, String> e) {
                    return ChoiceChip(
                      label: Text(e.value),
                      selected: _editScope == e.key,
                      onSelected: (_) => setState(() => _editScope = e.key),
                    );
                  }).toList(),
                ),
              ],
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_event_title'),
                controller: _titleController,
                onChanged: (_) {
                  if (_errorText != null) setState(() => _errorText = null);
                },
                decoration: InputDecoration(
                  labelText: 'Tiêu đề sự kiện *',
                  border: const OutlineInputBorder(),
                  errorText: _errorText,
                ),
              ),
              const SizedBox(height: 8),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Cả ngày'),
                value: _allDay,
                onChanged: (bool val) => setState(() => _allDay = val),
              ),
              const SizedBox(height: 6),
              TextField(
                key: const Key('input_event_date'),
                controller: _dateController,
                decoration: InputDecoration(
                  labelText: 'Ngày *',
                  hintText: 'YYYY-MM-DD',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    tooltip: 'Chọn ngày',
                    icon: const Icon(Icons.calendar_today_rounded, size: 18),
                    onPressed: () => _pickDate(_dateController),
                  ),
                ),
              ),
              if (!_allDay) ...<Widget>[
                const SizedBox(height: 10),
                Row(
                  children: <Widget>[
                    Expanded(
                      child: TextField(
                        key: const Key('input_event_start_time'),
                        controller: _startTimeController,
                        decoration: InputDecoration(
                          labelText: 'Bắt đầu',
                          hintText: 'HH:mm',
                          border: const OutlineInputBorder(),
                          suffixIcon: IconButton(
                            tooltip: 'Chọn giờ',
                            icon: const Icon(Icons.access_time_rounded, size: 18),
                            onPressed: () => _pickTime(_startTimeController),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        key: const Key('input_event_end_time'),
                        controller: _endTimeController,
                        decoration: InputDecoration(
                          labelText: 'Kết thúc',
                          hintText: 'HH:mm',
                          border: const OutlineInputBorder(),
                          suffixIcon: IconButton(
                            tooltip: 'Chọn giờ',
                            icon: const Icon(Icons.access_time_rounded, size: 18),
                            onPressed: () => _pickTime(_endTimeController),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_event_location'),
                controller: _locationController,
                decoration: const InputDecoration(
                  labelText: 'Địa điểm (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_event_desc'),
                controller: _descController,
                minLines: 2,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Mô tả (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Lặp lại',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: tokens.textSecondary,
                ),
              ),
              const SizedBox(height: 6),
              Wrap(
                spacing: 6,
                children: const <MapEntry<String, String>>[
                  MapEntry<String, String>('NONE', 'Không lặp'),
                  MapEntry<String, String>('DAILY', 'Hằng ngày'),
                  MapEntry<String, String>('WEEKLY', 'Hằng tuần'),
                  MapEntry<String, String>('MONTHLY', 'Hằng tháng'),
                ].map((MapEntry<String, String> item) {
                  return ChoiceChip(
                    label: Text(item.value),
                    selected: _freq == item.key,
                    onSelected: (_) => setState(() => _freq = item.key),
                  );
                }).toList(),
              ),
              if (_freq != 'NONE') ...<Widget>[
                const SizedBox(height: 10),
                TextField(
                  key: const Key('input_event_until'),
                  controller: _untilController,
                  decoration: InputDecoration(
                    labelText: 'Lặp đến ngày (tùy chọn)',
                    hintText: 'YYYY-MM-DD',
                    border: const OutlineInputBorder(),
                    suffixIcon: IconButton(
                      tooltip: 'Chọn ngày',
                      icon: const Icon(Icons.calendar_today_rounded, size: 18),
                      onPressed: () => _pickDate(_untilController),
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_save_event'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _save,
                child: Text(_isEditing ? 'Lưu thay đổi' : 'Tạo sự kiện'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet tạo hoặc sửa Khối tập trung thời gian (`time_block`).
class TimeBlockSheet extends ConsumerStatefulWidget {
  const TimeBlockSheet({
    super.key,
    this.timeBlock,
    this.initialDateIso,
  });

  final LocalEntityRow? timeBlock;
  final String? initialDateIso;

  @override
  ConsumerState<TimeBlockSheet> createState() => _TimeBlockSheetState();
}

class _TimeBlockSheetState extends ConsumerState<TimeBlockSheet> {
  static final RegExp _isoDatePattern = RegExp(r'^\d{4}-\d{2}-\d{2}$');
  static final RegExp _timePattern = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');

  late final TextEditingController _dateController;
  late final TextEditingController _startController;
  late final TextEditingController _endController;
  late final TextEditingController _noteController;
  String? _taskId;
  String? _errorText;

  bool get _isEditing => widget.timeBlock != null;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final String fallbackDate = widget.initialDateIso ??
        ref
            .read(localRepositoryProvider)
            .getUserDayClock(userId)
            .businessDateForUtc(ref.read(referenceUtcProvider));

    if (_isEditing) {
      final Map<String, dynamic> p = widget.timeBlock!.payload;
      _taskId = p['taskId'] as String?;
      _noteController = TextEditingController(
        text: ((p['note'] ?? p['title']) as String?) ?? '',
      );
      final String rawStart =
          ((p['startLocal'] ?? p['startAt']) as String?) ?? '${fallbackDate}T14:00:00Z';
      final String rawEnd =
          ((p['endLocal'] ?? p['endAt']) as String?) ?? '${fallbackDate}T15:00:00Z';
      final DateTime? parsedStart = DateTime.tryParse(rawStart)?.toLocal();
      final DateTime? parsedEnd = DateTime.tryParse(rawEnd)?.toLocal();
      _dateController = TextEditingController(
        text: parsedStart != null
            ? '${parsedStart.year.toString().padLeft(4, '0')}-${parsedStart.month.toString().padLeft(2, '0')}-${parsedStart.day.toString().padLeft(2, '0')}'
            : fallbackDate,
      );
      _startController = TextEditingController(
        text: parsedStart != null
            ? '${parsedStart.hour.toString().padLeft(2, '0')}:${parsedStart.minute.toString().padLeft(2, '0')}'
            : '14:00',
      );
      _endController = TextEditingController(
        text: parsedEnd != null
            ? '${parsedEnd.hour.toString().padLeft(2, '0')}:${parsedEnd.minute.toString().padLeft(2, '0')}'
            : '15:00',
      );
    } else {
      _dateController = TextEditingController(text: fallbackDate);
      _startController = TextEditingController(text: '14:00');
      _endController = TextEditingController(text: '15:00');
      _noteController = TextEditingController();
    }
  }

  @override
  void dispose() {
    _dateController.dispose();
    _startController.dispose();
    _endController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final DateTime base = DateTime.tryParse(_dateController.text.trim()) ??
        ref.read(referenceUtcProvider).toLocal();
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: base,
      firstDate: DateTime(2020),
      lastDate: DateTime(2040),
    );
    if (picked != null && mounted) {
      final String y = picked.year.toString().padLeft(4, '0');
      final String m = picked.month.toString().padLeft(2, '0');
      final String d = picked.day.toString().padLeft(2, '0');
      setState(() => _dateController.text = '$y-$m-$d');
    }
  }

  Future<void> _pickTime(TextEditingController ctrl) async {
    TimeOfDay initial = const TimeOfDay(hour: 14, minute: 0);
    if (_timePattern.hasMatch(ctrl.text.trim())) {
      final List<String> parts = ctrl.text.trim().split(':');
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
      setState(() => ctrl.text = '$hh:$mm');
    }
  }

  void _save() {
    final String dateIso = _dateController.text.trim();
    final String startHm = _startController.text.trim();
    final String endHm = _endController.text.trim();
    if (!_isoDatePattern.hasMatch(dateIso)) {
      setState(() => _errorText = 'Ngày cần đúng định dạng YYYY-MM-DD.');
      return;
    }
    if (!_timePattern.hasMatch(startHm) || !_timePattern.hasMatch(endHm)) {
      setState(() => _errorText = 'Giờ cần đúng định dạng HH:mm.');
      return;
    }
    final DateTime startLocal = DateTime.parse('${dateIso}T$startHm:00');
    final DateTime endLocal = DateTime.parse('${dateIso}T$endHm:00');
    if (!endLocal.isAfter(startLocal)) {
      setState(() => _errorText = 'Giờ kết thúc phải sau giờ bắt đầu.');
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final repo = ref.read(localRepositoryProvider);
    final String startUtcIso = startLocal.toUtc().toIso8601String();
    final String endUtcIso = endLocal.toUtc().toIso8601String();

    if (_isEditing) {
      repo.updateTimeBlock(
        userId: userId,
        timeBlockId: widget.timeBlock!.id,
        taskId: _taskId,
        clearTask: _taskId == null,
        startAtIso: startUtcIso,
        endAtIso: endUtcIso,
        note: _noteController.text,
        nowUtc: nowUtc,
      );
    } else {
      repo.createTimeBlock(
        userId: userId,
        taskId: _taskId,
        startAtIso: startUtcIso,
        endAtIso: endUtcIso,
        note: _noteController.text,
        nowUtc: nowUtc,
      );
    }
    if (mounted) Navigator.of(context).pop(true);
  }

  Future<void> _confirmDelete() async {
    if (!_isEditing) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa khối thời gian?'),
        content: const Text('Khối tập trung này sẽ bị xóa khỏi lịch trong ngày.'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_timeblock'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      ref.read(localRepositoryProvider).deleteTimeBlock(
            userId: ref.read(currentUserIdProvider),
            timeBlockId: widget.timeBlock!.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> tasks = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.task))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final String? validTaskId =
        tasks.any((LocalEntityRow t) => t.id == _taskId) ? _taskId : null;
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
                      _isEditing
                          ? 'Sửa khối tập trung'
                          : 'Tạo khối tập trung (Time Block)',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  if (_isEditing)
                    IconButton(
                      key: const Key('btn_delete_timeblock'),
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
              if (tasks.isNotEmpty) ...<Widget>[
                DropdownButtonFormField<String?>(
                  initialValue: validTaskId,
                  decoration: const InputDecoration(
                    labelText: 'Gắn với công việc (tùy chọn)',
                    border: OutlineInputBorder(),
                  ),
                  items: <DropdownMenuItem<String?>>[
                    const DropdownMenuItem<String?>(
                      value: null,
                      child: Text('Khối tập trung tự do'),
                    ),
                    ...tasks.map(
                      (LocalEntityRow t) => DropdownMenuItem<String?>(
                        value: t.id,
                        child: Text(
                          (t.payload['title'] as String?) ?? 'Công việc',
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                  ],
                  onChanged: (String? v) => setState(() => _taskId = v),
                ),
                const SizedBox(height: 10),
              ],
              TextField(
                key: const Key('input_timeblock_note'),
                controller: _noteController,
                onChanged: (_) {
                  if (_errorText != null) setState(() => _errorText = null);
                },
                decoration: InputDecoration(
                  labelText: 'Ghi chú / Mục tiêu khối thời gian',
                  border: const OutlineInputBorder(),
                  errorText: _errorText,
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_timeblock_date'),
                controller: _dateController,
                decoration: InputDecoration(
                  labelText: 'Ngày *',
                  hintText: 'YYYY-MM-DD',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    tooltip: 'Chọn ngày',
                    icon: const Icon(Icons.calendar_today_rounded, size: 18),
                    onPressed: _pickDate,
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      key: const Key('input_timeblock_start'),
                      controller: _startController,
                      decoration: InputDecoration(
                        labelText: 'Bắt đầu',
                        hintText: 'HH:mm',
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          tooltip: 'Chọn giờ',
                          icon: const Icon(Icons.access_time_rounded, size: 18),
                          onPressed: () => _pickTime(_startController),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      key: const Key('input_timeblock_end'),
                      controller: _endController,
                      decoration: InputDecoration(
                        labelText: 'Kết thúc',
                        hintText: 'HH:mm',
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          tooltip: 'Chọn giờ',
                          icon: const Icon(Icons.access_time_rounded, size: 18),
                          onPressed: () => _pickTime(_endController),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_save_timeblock'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _save,
                child: Text(_isEditing ? 'Lưu thay đổi' : 'Tạo khối thời gian'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
