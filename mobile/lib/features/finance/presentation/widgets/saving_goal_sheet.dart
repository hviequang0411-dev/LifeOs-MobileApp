import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/money/decimal10k.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Biểu mẫu Tạo / Sửa Mục tiêu Tiết kiệm (`saving_goal` — dùng `Decimal10k`).
class SavingGoalSheet extends ConsumerStatefulWidget {
  const SavingGoalSheet({super.key, this.existing});

  final LocalEntityRow? existing;

  @override
  ConsumerState<SavingGoalSheet> createState() => _SavingGoalSheetState();
}

class _SavingGoalSheetState extends ConsumerState<SavingGoalSheet> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _targetAmountController = TextEditingController();
  final TextEditingController _targetOnController = TextEditingController();
  final FocusNode _nameFocus = FocusNode();
  final FocusNode _amountFocus = FocusNode();

  String _status = 'ACTIVE';
  String? _nameError;
  String? _amountError;
  String? _dateError;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic>? p = widget.existing?.payload;
    if (p != null) {
      _nameController.text = (p['name'] as String?) ?? '';
      _targetAmountController.text =
          Decimal10k.parseJsonString((p['targetAmount'] ?? '0').toString())
              .toJsonString();
      _targetOnController.text = (p['targetOn'] as String?) ?? '';
      _status = (p['status'] as String? ?? 'ACTIVE').toUpperCase();
    } else {
      _targetAmountController.text = '10000000';
      _targetOnController.text = '2026-12-31';
    }

    _nameFocus.addListener(() {
      if (!_nameFocus.hasFocus && mounted) {
        setState(() => _nameError = _validateName());
      }
    });
    _amountFocus.addListener(() {
      if (!_amountFocus.hasFocus && mounted) {
        setState(() => _amountError = _validateAmount());
      }
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _targetAmountController.dispose();
    _targetOnController.dispose();
    _nameFocus.dispose();
    _amountFocus.dispose();
    super.dispose();
  }

  String? _validateName() => _nameController.text.trim().isEmpty
      ? 'Vui lòng nhập tên mục tiêu tiết kiệm.'
      : null;

  Decimal10k? _parseAmount() {
    final String digits =
        _targetAmountController.text.replaceAll(RegExp(r'[^0-9.]'), '');
    if (digits.isEmpty) return null;
    try {
      final Decimal10k v = Decimal10k.parseJsonString(digits);
      return v.scaledValue > 0 ? v : null;
    } catch (_) {
      return null;
    }
  }

  String? _validateAmount() =>
      _parseAmount() == null ? 'Số tiền mục tiêu phải lớn hơn 0.' : null;

  void _save() {
    final String rawTargetOn = _targetOnController.text.trim();
    final String? dateErr = rawTargetOn.isNotEmpty &&
            !RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(rawTargetOn)
        ? 'Ngày dự kiến cần đúng định dạng YYYY-MM-DD.'
        : null;
    setState(() {
      _nameError = _validateName();
      _amountError = _validateAmount();
      _dateError = dateErr;
    });
    if (_nameError != null || _amountError != null || _dateError != null) {
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final Map<String, dynamic> settings = repo.getUserSettings(userId);
    final String currency =
        (settings['defaultCurrency'] as String?) ?? 'VND';

    repo.finance.saveSavingGoal(
      userId: userId,
      goalId: widget.existing?.id,
      name: _nameController.text,
      currency: currency,
      targetAmount: _parseAmount()!,
      targetOnIso: _targetOnController.text.trim(),
      status: _status,
      nowUtc: ref.read(referenceUtcProvider),
    );
    Navigator.of(context).pop(true);
  }

  Future<void> _pickTargetDate() async {
    final DateTime base = DateTime.tryParse(_targetOnController.text.trim()) ??
        ref.read(referenceUtcProvider).toLocal();
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: base,
      firstDate: DateTime(2020),
      lastDate: DateTime(2045),
    );
    if (picked != null && mounted) {
      final String y = picked.year.toString().padLeft(4, '0');
      final String m = picked.month.toString().padLeft(2, '0');
      final String d = picked.day.toString().padLeft(2, '0');
      setState(() {
        _targetOnController.text = '$y-$m-$d';
        _dateError = null;
      });
    }
  }

  Future<void> _confirmDelete() async {
    if (widget.existing == null) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa mục tiêu tiết kiệm?'),
        content: Text(
          'Mục tiêu "${_nameController.text.trim()}" và lịch sử góp sẽ bị xóa.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_saving_goal'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      ref.read(localRepositoryProvider).deleteSavingGoal(
            userId: ref.read(currentUserIdProvider),
            goalId: widget.existing!.id,
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
                    widget.existing == null
                        ? 'Tạo mục tiêu tiết kiệm'
                        : 'Cập nhật mục tiêu tiết kiệm',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                if (widget.existing != null)
                  IconButton(
                    key: const Key('btn_delete_saving_goal'),
                    tooltip: ViStrings.deleteAction,
                    onPressed: _confirmDelete,
                    icon: Icon(
                      Icons.delete_outline_rounded,
                      color: tokens.danger,
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_saving_goal_name'),
              controller: _nameController,
              focusNode: _nameFocus,
              decoration: InputDecoration(
                labelText: 'Tên mục tiêu (ví dụ: Quỹ dự phòng) *',
                errorText: _nameError,
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_saving_goal_target_amount'),
              controller: _targetAmountController,
              focusNode: _amountFocus,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: 'Số tiền mục tiêu *',
                suffixText: 'VND',
                errorText: _amountError,
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_saving_goal_target_on'),
              controller: _targetOnController,
              onChanged: (_) {
                if (_dateError != null) setState(() => _dateError = null);
              },
              decoration: InputDecoration(
                labelText: 'Ngày dự kiến đạt (tùy chọn)',
                hintText: 'YYYY-MM-DD',
                border: const OutlineInputBorder(),
                errorText: _dateError,
                suffixIcon: IconButton(
                  tooltip: 'Chọn ngày',
                  icon: const Icon(Icons.calendar_today_rounded, size: 18),
                  onPressed: _pickTargetDate,
                ),
              ),
            ),
            const SizedBox(height: 8),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Đang theo dõi'),
              value: _status == 'ACTIVE',
              onChanged: (bool v) =>
                  setState(() => _status = v ? 'ACTIVE' : 'ARCHIVED'),
            ),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      minimumSize:
                          const Size.fromHeight(LifeOsTokens.minTouchTarget),
                    ),
                    onPressed: () => Navigator.of(context).pop(false),
                    child: const Text(ViStrings.cancelAction),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton(
                    key: const Key('btn_save_saving_goal'),
                    style: FilledButton.styleFrom(
                      minimumSize:
                          const Size.fromHeight(LifeOsTokens.minTouchTarget),
                    ),
                    onPressed: _save,
                    child: const Text('Lưu mục tiêu'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      ),
    );
  }
}

/// Hộp thoại Thêm khoản đóng góp vào Mục tiêu tiết kiệm (`saving_contribution` — `Decimal10k`).
class SavingContributionDialog extends ConsumerStatefulWidget {
  const SavingContributionDialog({
    super.key,
    required this.goalId,
    required this.goalName,
  });

  final String goalId;
  final String goalName;

  @override
  ConsumerState<SavingContributionDialog> createState() =>
      _SavingContributionDialogState();
}

class _SavingContributionDialogState
    extends ConsumerState<SavingContributionDialog> {
  final TextEditingController _amountController =
      TextEditingController(text: '500000');
  final TextEditingController _noteController = TextEditingController();
  final FocusNode _amountFocus = FocusNode();
  String? _amountError;

  @override
  void initState() {
    super.initState();
    _amountFocus.addListener(() {
      if (!_amountFocus.hasFocus && mounted) {
        setState(() => _amountError = _validateAmount());
      }
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _noteController.dispose();
    _amountFocus.dispose();
    super.dispose();
  }

  Decimal10k? _parseAmount() {
    final String digits =
        _amountController.text.replaceAll(RegExp(r'[^0-9.]'), '');
    if (digits.isEmpty) return null;
    try {
      final Decimal10k val = Decimal10k.parseJsonString(digits);
      return val.scaledValue > 0 ? val : null;
    } catch (_) {
      return null;
    }
  }

  String? _validateAmount() =>
      _parseAmount() == null ? 'Số tiền đóng góp phải lớn hơn 0.' : null;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('Góp tiền vào "${widget.goalName}"'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          TextField(
            key: const Key('input_saving_contribution_amount'),
            controller: _amountController,
            focusNode: _amountFocus,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              labelText: 'Số tiền đóng góp *',
              suffixText: 'VND',
              errorText: _amountError,
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            key: const Key('input_saving_contribution_note'),
            controller: _noteController,
            decoration: const InputDecoration(
              labelText: 'Ghi chú (tùy chọn)',
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text(ViStrings.cancelAction),
        ),
        FilledButton(
          key: const Key('btn_save_saving_contribution'),
          style: FilledButton.styleFrom(
            minimumSize: const Size(120, LifeOsTokens.minTouchTarget),
          ),
          onPressed: () {
            final String? err = _validateAmount();
            if (err != null) {
              setState(() => _amountError = err);
              return;
            }
            final String userId = ref.read(currentUserIdProvider);
            final LifeOsLocalRepository repo =
                ref.read(localRepositoryProvider);
            final DateTime refUtc = ref.read(referenceUtcProvider);
            final String todayIso =
                repo.getUserDayClock(userId).businessDateForUtc(refUtc);

            repo.finance.addSavingContribution(
              userId: userId,
              goalId: widget.goalId,
              amount: _parseAmount()!,
              occurredOnIso: todayIso,
              note: _noteController.text,
              nowUtc: refUtc,
            );
            Navigator.of(context).pop(true);
          },
          child: const Text('Ghi nhận đóng góp'),
        ),
      ],
    );
  }
}
