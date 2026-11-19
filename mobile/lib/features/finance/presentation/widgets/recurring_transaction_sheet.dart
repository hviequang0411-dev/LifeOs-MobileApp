import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/money/decimal10k.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Biểu mẫu tạo/sửa Giao dịch định kỳ (`recurring_transaction` — `MWP-C`).
/// Dùng đúng `frequency` (`DAILY|WEEKLY|MONTHLY|YEARLY`), `intervalCount`, `startOn`, `endOn`, `status` (không RRULE)
/// và hiển thị **Xem trước 3 kỳ kế tiếp**.
class RecurringTransactionSheet extends ConsumerStatefulWidget {
  const RecurringTransactionSheet({
    super.key,
    this.existing,
  });

  final LocalEntityRow? existing;

  @override
  ConsumerState<RecurringTransactionSheet> createState() =>
      _RecurringTransactionSheetState();
}

class _RecurringTransactionSheetState
    extends ConsumerState<RecurringTransactionSheet> {
  final TextEditingController _amountController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();
  final TextEditingController _intervalController = TextEditingController();
  final TextEditingController _startOnController = TextEditingController();
  final TextEditingController _endOnController = TextEditingController();

  final FocusNode _amountFocus = FocusNode();
  final FocusNode _intervalFocus = FocusNode();

  String _kind = 'EXPENSE';
  String _frequency = 'MONTHLY';
  String _status = 'ACTIVE';
  String? _accountId;
  String? _categoryId;
  String? _amountError;
  String? _intervalError;
  String? _formError;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);
    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(refUtc);

    final Map<String, dynamic>? p = widget.existing?.payload;
    if (p != null) {
      _kind = (p['kind'] as String? ?? 'EXPENSE').toUpperCase();
      _frequency = (p['frequency'] as String? ?? 'MONTHLY').toUpperCase();
      _status = (p['status'] as String? ?? 'ACTIVE').toUpperCase();
      _accountId = p['accountId'] as String?;
      _categoryId = p['categoryId'] as String?;
      _amountController.text =
          Decimal10k.parseJsonString((p['amount'] ?? '0').toString())
              .toJsonString();
      _noteController.text = (p['note'] as String?) ?? '';
      _intervalController.text =
          ((p['intervalCount'] as num?)?.toInt() ?? 1).toString();
      _startOnController.text = (p['startOn'] as String?) ?? todayIso;
      _endOnController.text = (p['endOn'] as String?) ?? '';
    } else {
      _amountController.text = '150000';
      _intervalController.text = '1';
      _startOnController.text = todayIso;
    }

    _amountFocus.addListener(() {
      if (!_amountFocus.hasFocus && mounted) {
        setState(() => _amountError = _validateAmount());
      }
    });
    _intervalFocus.addListener(() {
      if (!_intervalFocus.hasFocus && mounted) {
        setState(() => _intervalError = _validateInterval());
      }
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _noteController.dispose();
    _intervalController.dispose();
    _startOnController.dispose();
    _endOnController.dispose();
    _amountFocus.dispose();
    _intervalFocus.dispose();
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
      _parseAmount() == null ? 'Số tiền phải lớn hơn 0.' : null;

  int? _parseInterval() {
    final int? n = int.tryParse(_intervalController.text.trim());
    if (n == null || n < 1 || n > 365) return null;
    return n;
  }

  String? _validateInterval() =>
      _parseInterval() == null ? 'Chu kỳ lặp phải từ 1 đến 365.' : null;

  void _save(List<LocalEntityRow> accounts) {
    final String? amtErr = _validateAmount();
    final String? intErr = _validateInterval();
    String? resolvedAccount = _accountId;
    if (resolvedAccount == null && accounts.isNotEmpty) {
      resolvedAccount = accounts.first.id;
    }
    setState(() {
      _amountError = amtErr;
      _intervalError = intErr;
      _formError =
          resolvedAccount == null ? 'Vui lòng tạo ít nhất 1 ví trước.' : null;
    });
    if (amtErr != null || intErr != null || resolvedAccount == null) return;
    final String startIso = _startOnController.text.trim();
    final String endIso = _endOnController.text.trim();
    final RegExp dateRe = RegExp(r'^\d{4}-\d{2}-\d{2}$');
    if (!dateRe.hasMatch(startIso) || (endIso.isNotEmpty && !dateRe.hasMatch(endIso))) {
      setState(
        () => _formError = 'Ngày bắt đầu / kết thúc cần đúng định dạng YYYY-MM-DD.',
      );
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final Decimal10k amount = _parseAmount()!;
    final int interval = _parseInterval()!;

    try {
      repo.finance.saveRecurringTransaction(
        userId: userId,
        recurringId: widget.existing?.id,
        accountId: resolvedAccount,
        categoryId: _categoryId,
        kind: _kind,
        amount: amount,
        note: _noteController.text,
        frequency: _frequency,
        intervalCount: interval,
        startOnIso: startIso,
        endOnIso: endIso.isEmpty ? null : endIso,
        status: _status,
        nowUtc: ref.read(referenceUtcProvider),
      );
      Navigator.of(context).pop(true);
    } catch (e) {
      final String code = e is StateError ? e.message : e.toString();
      setState(() => _formError = ViStrings.mapBackendErrorCode(code));
    }
  }

  Future<void> _pickRecurringDate(TextEditingController ctrl) async {
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

  Future<void> _confirmDelete() async {
    if (widget.existing == null) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa giao dịch định kỳ?'),
        content: const Text('Lịch định kỳ này sẽ ngừng tạo giao dịch mới.'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_recurring'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      ref.read(localRepositoryProvider).deleteRecurringTransaction(
            userId: ref.read(currentUserIdProvider),
            recurringId: widget.existing!.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> accounts = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeAccount))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> categories = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeCategory))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final String? effectiveAccountId = accounts.any((e) => e.id == _accountId)
        ? _accountId
        : (accounts.isNotEmpty ? accounts.first.id : null);

    final int previewInterval = _parseInterval() ?? 1;
    final List<String> previewDates =
        FinanceLocalRepository.computeRecurringOccurrencesPreview(
      frequency: _frequency,
      intervalCount: previewInterval,
      startOnIso: _startOnController.text.trim(),
      endOnIso: _endOnController.text.trim(),
      count: 3,
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
            Row(
              children: <Widget>[
                Expanded(
                  child: Text(
                    widget.existing == null
                        ? 'Thêm giao dịch định kỳ'
                        : 'Cập nhật giao dịch định kỳ',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                if (widget.existing != null)
                  IconButton(
                    key: const Key('btn_delete_recurring'),
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
            if (_formError != null) ...<Widget>[
              Text(
                _formError!,
                style: TextStyle(
                  color: tokens.danger,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 8),
            ],
            SegmentedButton<String>(
              segments: const <ButtonSegment<String>>[
                ButtonSegment<String>(value: 'EXPENSE', label: Text('Chi tiêu')),
                ButtonSegment<String>(value: 'INCOME', label: Text('Thu nhập')),
              ],
              selected: <String>{_kind},
              onSelectionChanged: (Set<String> s) =>
                  setState(() => _kind = s.first),
            ),
            const SizedBox(height: 12),
            if (accounts.isNotEmpty)
              DropdownButtonFormField<String>(
                key: const Key('dropdown_recurring_account'),
                initialValue: effectiveAccountId,
                decoration: const InputDecoration(
                  labelText: 'Tài khoản / Ví *',
                  border: OutlineInputBorder(),
                ),
                items: accounts
                    .map(
                      (LocalEntityRow a) => DropdownMenuItem<String>(
                        value: a.id,
                        child: Text((a.payload['name'] as String?) ?? 'Ví'),
                      ),
                    )
                    .toList(),
                onChanged: (String? v) => setState(() => _accountId = v),
              ),
            const SizedBox(height: 12),
            if (categories.isNotEmpty) ...<Widget>[
              DropdownButtonFormField<String?>(
                initialValue: categories.any((c) => c.id == _categoryId)
                    ? _categoryId
                    : null,
                decoration: const InputDecoration(
                  labelText: 'Danh mục (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
                items: <DropdownMenuItem<String?>>[
                  const DropdownMenuItem<String?>(
                    value: null,
                    child: Text('Không chọn danh mục'),
                  ),
                  ...categories.map(
                    (LocalEntityRow c) => DropdownMenuItem<String?>(
                      value: c.id,
                      child:
                          Text((c.payload['name'] as String?) ?? 'Danh mục'),
                    ),
                  ),
                ],
                onChanged: (String? v) => setState(() => _categoryId = v),
              ),
              const SizedBox(height: 12),
            ],
            TextField(
              key: const Key('input_recurring_amount'),
              controller: _amountController,
              focusNode: _amountFocus,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: 'Số tiền *',
                suffixText: 'VND',
                errorText: _amountError,
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_recurring_note'),
              controller: _noteController,
              decoration: const InputDecoration(
                labelText: 'Ghi chú (ví dụ: Tiền thuê nhà, Lương tháng)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  flex: 2,
                  child: DropdownButtonFormField<String>(
                    key: const Key('dropdown_recurring_frequency'),
                    initialValue: _frequency,
                    decoration: const InputDecoration(
                      labelText: 'Tần suất *',
                      border: OutlineInputBorder(),
                    ),
                    items: const <DropdownMenuItem<String>>[
                      DropdownMenuItem<String>(
                        value: 'DAILY',
                        child: Text('Hằng ngày'),
                      ),
                      DropdownMenuItem<String>(
                        value: 'WEEKLY',
                        child: Text('Hằng tuần'),
                      ),
                      DropdownMenuItem<String>(
                        value: 'MONTHLY',
                        child: Text('Hằng tháng'),
                      ),
                      DropdownMenuItem<String>(
                        value: 'YEARLY',
                        child: Text('Hằng năm'),
                      ),
                    ],
                    onChanged: (String? v) {
                      if (v != null) setState(() => _frequency = v);
                    },
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: TextField(
                    key: const Key('input_recurring_interval'),
                    controller: _intervalController,
                    focusNode: _intervalFocus,
                    keyboardType: TextInputType.number,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      labelText: 'Mỗi (lần) *',
                      errorText: _intervalError,
                      border: const OutlineInputBorder(),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  child: TextField(
                    key: const Key('input_recurring_start_on'),
                    controller: _startOnController,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      labelText: 'Ngày bắt đầu *',
                      hintText: 'YYYY-MM-DD',
                      border: const OutlineInputBorder(),
                      suffixIcon: IconButton(
                        tooltip: 'Chọn ngày',
                        icon: const Icon(Icons.calendar_today_rounded, size: 18),
                        onPressed: () => _pickRecurringDate(_startOnController),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: TextField(
                    key: const Key('input_recurring_end_on'),
                    controller: _endOnController,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      labelText: 'Kết thúc (tùy chọn)',
                      hintText: 'YYYY-MM-DD',
                      border: const OutlineInputBorder(),
                      suffixIcon: IconButton(
                        tooltip: 'Chọn ngày',
                        icon: const Icon(Icons.calendar_today_rounded, size: 18),
                        onPressed: () => _pickRecurringDate(_endOnController),
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Đang hoạt động'),
              subtitle: Text(
                _status == 'ACTIVE'
                    ? 'Tự động tính kỳ sắp tới'
                    : 'Đã tạm dừng (PAUSED)',
              ),
              value: _status == 'ACTIVE',
              onChanged: (bool active) =>
                  setState(() => _status = active ? 'ACTIVE' : 'PAUSED'),
            ),
            const SizedBox(height: 6),
            Container(
              key: const Key('recurring_preview_box'),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: tokens.domainFinance.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                border: Border.all(
                  color: tokens.domainFinance.withValues(alpha: 0.3),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    'Xem trước 3 lần lặp kế tiếp',
                    style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                      color: tokens.domainFinance,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    previewDates.isEmpty
                        ? 'Kiểm tra lại định dạng ngày YYYY-MM-DD'
                        : previewDates.join('  •  '),
                    style: TextStyle(
                      fontSize: 13.5,
                      fontWeight: FontWeight.w600,
                      color: tokens.textPrimary,
                      fontFeatures: const <FontFeature>[
                        FontFeature.tabularFigures(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
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
                    key: const Key('btn_save_recurring_tx'),
                    style: FilledButton.styleFrom(
                      minimumSize:
                          const Size.fromHeight(LifeOsTokens.minTouchTarget),
                    ),
                    onPressed: () => _save(accounts),
                    child: const Text('Lưu định kỳ'),
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
