import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/database_records.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/theme/app_tokens.dart';

/// Bottom sheet tạo mới hoặc chỉnh sửa Thói quen (`habit`).
class HabitEditorSheet extends ConsumerStatefulWidget {
  const HabitEditorSheet({
    super.key,
    this.habit,
  });

  final LocalEntityRow? habit;

  @override
  ConsumerState<HabitEditorSheet> createState() => _HabitEditorSheetState();
}

class _HabitEditorSheetState extends ConsumerState<HabitEditorSheet> {
  static final RegExp _isoDatePattern = RegExp(r'^\d{4}-\d{2}-\d{2}$');
  static final RegExp _timePattern = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');

  late final TextEditingController _nameController;
  late final TextEditingController _descController;
  late final TextEditingController _startOnController;
  late final TextEditingController _reminderController;

  late String _frequencyType;
  late Set<int> _selectedDays;
  late int _timesPerWeek;
  late int _intervalDays;
  String? _validationError;

  bool get _isEditing => widget.habit != null;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic> p =
        widget.habit?.payload ?? const <String, dynamic>{};
    _nameController =
        TextEditingController(text: (p['name'] as String?) ?? '');
    _descController =
        TextEditingController(text: (p['description'] as String?) ?? '');
    _startOnController = TextEditingController(
      text: (p['startOn'] as String?) ??
          ref.read(localRepositoryProvider).getUserDayClock(
                ref.read(currentUserIdProvider),
              ).businessDateForUtc(ref.read(referenceUtcProvider)),
    );
    final String rawRem = (p['reminderTime'] as String?) ?? '';
    _reminderController = TextEditingController(
      text: rawRem.length >= 5 ? rawRem.substring(0, 5) : rawRem,
    );

    final String rawFreq = ((p['scheduleType'] ?? p['frequencyType']) as String? ?? 'DAILY')
        .toUpperCase();
    _frequencyType = const <String>{
      'DAILY',
      'SPECIFIC_DAYS',
      'TIMES_PER_WEEK',
      'INTERVAL',
    }.contains(rawFreq)
        ? rawFreq
        : 'DAILY';

    final List<int> rawDays = (p['daysOfWeek'] as List<dynamic>?)
            ?.whereType<num>()
            .map((num n) => n.toInt())
            .toList() ??
        const <int>[];
    if (rawDays.isNotEmpty) {
      _selectedDays = rawDays.toSet();
    } else if (p['weekdays'] is num) {
      final int mask = (p['weekdays'] as num).toInt();
      _selectedDays = <int>{
        for (int d = 1; d <= 7; d++)
          if ((mask & (1 << (d - 1))) != 0) d,
      };
    } else {
      _selectedDays = <int>{1, 2, 3, 4, 5, 6, 7};
    }

    _timesPerWeek = ((p['timesPerWeek'] as num?)?.toInt() ?? 3).clamp(1, 7);
    _intervalDays = ((p['intervalDays'] as num?)?.toInt() ?? 2).clamp(2, 30);
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descController.dispose();
    _startOnController.dispose();
    _reminderController.dispose();
    super.dispose();
  }

  Future<void> _pickStartDate() async {
    final DateTime base = DateTime.tryParse(_startOnController.text.trim()) ??
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
      setState(() => _startOnController.text = '$y-$m-$d');
    }
  }

  Future<void> _pickReminderTime() async {
    TimeOfDay initial = const TimeOfDay(hour: 8, minute: 0);
    final String cur = _reminderController.text.trim();
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
      setState(() => _reminderController.text = '$hh:$mm');
    }
  }

  void _save() {
    final String name = _nameController.text.trim();
    if (name.isEmpty) {
      setState(() => _validationError = 'Vui lòng nhập tên thói quen.');
      return;
    }
    final String startOn = _startOnController.text.trim();
    if (startOn.isNotEmpty && !_isoDatePattern.hasMatch(startOn)) {
      setState(
        () => _validationError = 'Ngày bắt đầu cần đúng định dạng YYYY-MM-DD.',
      );
      return;
    }
    final String reminder = _reminderController.text.trim();
    if (reminder.isNotEmpty && !_timePattern.hasMatch(reminder)) {
      setState(() => _validationError = 'Giờ nhắc cần đúng định dạng HH:mm.');
      return;
    }
    if (_frequencyType == 'SPECIFIC_DAYS' && _selectedDays.isEmpty) {
      setState(() => _validationError = 'Vui lòng chọn ít nhất một ngày trong tuần.');
      return;
    }

    final List<int> sortedDays = _selectedDays.toList()..sort();
    final String userId = ref.read(currentUserIdProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final repo = ref.read(localRepositoryProvider);

    if (_isEditing) {
      repo.updateHabit(
        userId: userId,
        habitId: widget.habit!.id,
        name: name,
        description: _descController.text,
        frequencyType: _frequencyType,
        daysOfWeek: _frequencyType == 'SPECIFIC_DAYS'
            ? sortedDays
            : const <int>[1, 2, 3, 4, 5, 6, 7],
        timesPerWeek: _frequencyType == 'TIMES_PER_WEEK' ? _timesPerWeek : null,
        intervalDays: _frequencyType == 'INTERVAL' ? _intervalDays : null,
        startOn: startOn,
        reminderTime: reminder,
        nowUtc: nowUtc,
      );
    } else {
      repo.createHabit(
        userId: userId,
        name: name,
        description: _descController.text,
        frequencyType: _frequencyType,
        daysOfWeek: _frequencyType == 'SPECIFIC_DAYS'
            ? sortedDays
            : const <int>[],
        timesPerWeek: _frequencyType == 'TIMES_PER_WEEK' ? _timesPerWeek : null,
        intervalDays: _frequencyType == 'INTERVAL' ? _intervalDays : null,
        startOn: startOn.isEmpty ? null : startOn,
        reminderTime: reminder.isEmpty ? null : reminder,
        nowUtc: nowUtc,
      );
    }

    if (mounted) {
      Navigator.of(context).pop(true);
    }
  }

  Future<void> _confirmDelete() async {
    if (!_isEditing) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa thói quen?'),
        content: Text(
          'Bạn chắc chắn muốn xóa thói quen "${_nameController.text.trim()}"?',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_habit'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      ref.read(localRepositoryProvider).deleteHabit(
            userId: ref.read(currentUserIdProvider),
            habitId: widget.habit!.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    const List<MapEntry<int, String>> dayLabels = <MapEntry<int, String>>[
      MapEntry<int, String>(1, 'T2'),
      MapEntry<int, String>(2, 'T3'),
      MapEntry<int, String>(3, 'T4'),
      MapEntry<int, String>(4, 'T5'),
      MapEntry<int, String>(5, 'T6'),
      MapEntry<int, String>(6, 'T7'),
      MapEntry<int, String>(7, 'CN'),
    ];

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
                      _isEditing ? 'Chỉnh sửa thói quen' : 'Thêm thói quen mới',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  if (_isEditing)
                    IconButton(
                      key: const Key('btn_delete_habit'),
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
                key: const Key('input_habit_name'),
                controller: _nameController,
                onChanged: (_) {
                  if (_validationError != null) {
                    setState(() => _validationError = null);
                  }
                },
                decoration: InputDecoration(
                  labelText: 'Tên thói quen *',
                  hintText: 'Ví dụ: Đọc sách 20 phút',
                  border: const OutlineInputBorder(),
                  errorText: _validationError,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_habit_desc'),
                controller: _descController,
                decoration: const InputDecoration(
                  labelText: 'Mô tả / động lực (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Tần suất',
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
                children: const <MapEntry<String, String>>[
                  MapEntry<String, String>('DAILY', 'Hằng ngày'),
                  MapEntry<String, String>('SPECIFIC_DAYS', 'Chọn thứ'),
                  MapEntry<String, String>('TIMES_PER_WEEK', 'Số lần/tuần'),
                  MapEntry<String, String>('INTERVAL', 'Cách ngày'),
                ].map((MapEntry<String, String> item) {
                  return ChoiceChip(
                    label: Text(item.value),
                    selected: _frequencyType == item.key,
                    onSelected: (_) =>
                        setState(() => _frequencyType = item.key),
                  );
                }).toList(),
              ),
              if (_frequencyType == 'SPECIFIC_DAYS') ...<Widget>[
                const SizedBox(height: 10),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: dayLabels.map((MapEntry<int, String> d) {
                    final bool selected = _selectedDays.contains(d.key);
                    return FilterChip(
                      label: Text(d.value),
                      selected: selected,
                      onSelected: (bool on) {
                        setState(() {
                          if (on) {
                            _selectedDays.add(d.key);
                          } else {
                            _selectedDays.remove(d.key);
                          }
                        });
                      },
                    );
                  }).toList(),
                ),
              ],
              if (_frequencyType == 'TIMES_PER_WEEK') ...<Widget>[
                const SizedBox(height: 10),
                Row(
                  children: <Widget>[
                    const Text('Mục tiêu mỗi tuần:'),
                    const Spacer(),
                    IconButton(
                      onPressed: _timesPerWeek > 1
                          ? () => setState(() => _timesPerWeek--)
                          : null,
                      icon: const Icon(Icons.remove_circle_outline_rounded),
                    ),
                    Text(
                      '$_timesPerWeek lần',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    IconButton(
                      onPressed: _timesPerWeek < 7
                          ? () => setState(() => _timesPerWeek++)
                          : null,
                      icon: const Icon(Icons.add_circle_outline_rounded),
                    ),
                  ],
                ),
              ],
              if (_frequencyType == 'INTERVAL') ...<Widget>[
                const SizedBox(height: 10),
                Row(
                  children: <Widget>[
                    const Text('Lặp lại mỗi:'),
                    const Spacer(),
                    IconButton(
                      onPressed: _intervalDays > 2
                          ? () => setState(() => _intervalDays--)
                          : null,
                      icon: const Icon(Icons.remove_circle_outline_rounded),
                    ),
                    Text(
                      '$_intervalDays ngày',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    IconButton(
                      onPressed: _intervalDays < 30
                          ? () => setState(() => _intervalDays++)
                          : null,
                      icon: const Icon(Icons.add_circle_outline_rounded),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: <Widget>[
                  Expanded(
                    flex: 3,
                    child: TextField(
                      key: const Key('input_habit_start_on'),
                      controller: _startOnController,
                      decoration: InputDecoration(
                        labelText: 'Ngày bắt đầu',
                        hintText: 'YYYY-MM-DD',
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          tooltip: 'Chọn ngày',
                          icon:
                              const Icon(Icons.calendar_today_rounded, size: 18),
                          onPressed: _pickStartDate,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    flex: 2,
                    child: TextField(
                      key: const Key('input_habit_reminder'),
                      controller: _reminderController,
                      decoration: InputDecoration(
                        labelText: 'Giờ nhắc',
                        hintText: 'HH:mm',
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          tooltip: 'Chọn giờ',
                          icon: const Icon(Icons.access_time_rounded, size: 18),
                          onPressed: _pickReminderTime,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              FilledButton(
                key: const Key('btn_save_habit'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _save,
                child: Text(
                  _isEditing ? 'Lưu thay đổi' : 'Tạo thói quen',
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
