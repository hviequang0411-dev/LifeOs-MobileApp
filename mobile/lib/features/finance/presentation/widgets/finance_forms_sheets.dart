import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/database_records.dart';
import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/money/decimal10k.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/sync/entity_types.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

Decimal10k? _tryParseDecimal(String raw) {
  final String cleaned = raw.trim().replaceAll(',', '').replaceAll(' ', '');
  if (cleaned.isEmpty) return null;
  try {
    return Decimal10k.parseJsonString(cleaned);
  } on FormatException {
    return null;
  }
}

/// Sheet tạo mới, chỉnh sửa, đối chiếu số dư, lưu trữ (F6) hoặc xóa ví (`financial_account`).
class FinanceAccountSheet extends ConsumerStatefulWidget {
  const FinanceAccountSheet({
    super.key,
    this.account,
  });

  final LocalEntityRow? account;

  @override
  ConsumerState<FinanceAccountSheet> createState() =>
      _FinanceAccountSheetState();
}

class _FinanceAccountSheetState extends ConsumerState<FinanceAccountSheet> {
  late final TextEditingController _nameController;
  late final TextEditingController _openingController;
  late String _accountType;
  late String _currency;
  late bool _archived;
  String? _errorText;

  bool get _isEditing => widget.account != null;

  @override
  void initState() {
    super.initState();
    final Map<String, dynamic> p =
        widget.account?.payload ?? const <String, dynamic>{};
    _nameController =
        TextEditingController(text: (p['name'] as String?) ?? '');
    _openingController = TextEditingController(text: '0');
    final String rawType =
        ((p['accountType'] ?? p['type']) as String? ?? 'BANK').toUpperCase();
    _accountType = const <String>{'CASH', 'BANK', 'CREDIT', 'INVESTMENT'}
            .contains(rawType)
        ? rawType
        : 'BANK';
    _currency = ((p['currency'] as String?) ?? 'VND').toUpperCase();
    _archived = p['archived'] == true || p['archivedAt'] != null;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _openingController.dispose();
    super.dispose();
  }

  String _formatAccountType(String type) {
    return switch (type.toUpperCase()) {
      'CASH' => 'Tiền mặt',
      'BANK' => 'Ngân hàng',
      'CREDIT' => 'Thẻ tín dụng',
      'INVESTMENT' => 'Đầu tư / Tiết kiệm',
      _ => type,
    };
  }

  String _extractErrorCode(Object e) {
    if (e is StateError) return e.message;
    return e.toString();
  }

  void _save() {
    final String name = _nameController.text.trim();
    if (name.isEmpty) {
      setState(() => _errorText = 'Vui lòng nhập tên ví hoặc tài khoản.');
      return;
    }
    final String userId = ref.read(currentUserIdProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);

    try {
      if (_isEditing) {
        repo.updateFinanceAccount(
          userId: userId,
          accountId: widget.account!.id,
          name: name,
          accountType: _accountType,
          currency: _currency,
          archived: _archived,
          nowUtc: nowUtc,
        );
      } else {
        final Decimal10k? opening =
            _tryParseDecimal(_openingController.text.trim());
        if (opening == null) {
          setState(() => _errorText = 'Số dư ban đầu không hợp lệ.');
          return;
        }
        repo.createFinanceAccount(
          userId: userId,
          name: name,
          accountType: _accountType,
          currency: _currency,
          openingBalance: opening,
          nowUtc: nowUtc,
        );
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      setState(
        () => _errorText = ViStrings.mapBackendErrorCode(_extractErrorCode(e)),
      );
    }
  }

  Future<void> _reconcileBalance() async {
    if (!_isEditing) return;
    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final Decimal10k currentBal = repo.computeAccountBalance(
      userId: userId,
      accountId: widget.account!.id,
    );
    final TextEditingController ctrl = TextEditingController(
      text: currentBal.toJsonString(),
    );
    String? dialogErr;

    final Decimal10k? targetBal = await showDialog<Decimal10k>(
      context: context,
      builder: (BuildContext ctx) => StatefulBuilder(
        builder: (BuildContext ctx, StateSetter setLocalState) => AlertDialog(
          title: const Text('Đối chiếu số dư thực tế'),
          content: TextField(
            key: const Key('input_reconcile_balance'),
            controller: ctrl,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: 'Số dư hiện có thực tế ($_currency)',
              border: const OutlineInputBorder(),
              errorText: dialogErr,
            ),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text(ViStrings.cancelAction),
            ),
            FilledButton(
              key: const Key('btn_confirm_reconcile'),
              onPressed: () {
                final Decimal10k? parsed =
                    _tryParseDecimal(ctrl.text.trim());
                if (parsed == null) {
                  setLocalState(() => dialogErr = 'Số tiền không hợp lệ.');
                  return;
                }
                Navigator.of(ctx).pop(parsed);
              },
              child: const Text('Điều chỉnh'),
            ),
          ],
        ),
      ),
    );
    ctrl.dispose();

    if (targetBal != null && mounted) {
      final DateTime nowUtc = ref.read(referenceUtcProvider);
      final String todayIso = repo.getUserDayClock(userId).businessDateForUtc(nowUtc);
      repo.reconcileAccountBalance(
        userId: userId,
        accountId: widget.account!.id,
        actualBalance: targetBal,
        occurredOnIso: todayIso,
        nowUtc: nowUtc,
      );
      Navigator.of(context).pop(true);
    }
  }

  Future<void> _confirmDelete() async {
    if (!_isEditing) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa tài khoản?'),
        content: Text(
          'Bạn muốn xóa "${_nameController.text.trim()}"? Nếu tài khoản đã có giao dịch, hãy dùng tùy chọn Lưu trữ thay thế.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_account'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      try {
        ref.read(localRepositoryProvider).deleteFinanceAccount(
              userId: ref.read(currentUserIdProvider),
              accountId: widget.account!.id,
              nowUtc: ref.read(referenceUtcProvider),
            );
        Navigator.of(context).pop(true);
      } catch (e) {
        setState(
          () => _errorText = ViStrings.mapBackendErrorCode(_extractErrorCode(e)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final bool masked = ref.watch(privacyMaskAmountsProvider);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    final int activeRecurringCount = _isEditing
        ? (ref
                    .watch(
                      entitiesByTypeStreamProvider(
                        SyncEntityType.recurringTransaction,
                      ),
                    )
                    .valueOrNull ??
                const <LocalEntityRow>[])
            .where(
              (LocalEntityRow r) =>
                  r.payload['accountId'] == widget.account!.id &&
                  (r.payload['status'] as String?)?.toUpperCase() == 'ACTIVE',
            )
            .length
        : 0;

    final Decimal10k? currentBal = _isEditing
        ? repo.computeAccountBalance(userId: userId, accountId: widget.account!.id)
        : null;

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
                      _isEditing ? 'Quản lý ví & tài khoản' : 'Thêm ví mới',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  if (_isEditing)
                    IconButton(
                      key: const Key('btn_delete_account'),
                      tooltip: ViStrings.deleteAction,
                      onPressed: _confirmDelete,
                      icon: Icon(
                        Icons.delete_outline_rounded,
                        color: tokens.danger,
                      ),
                    ),
                ],
              ),
              if (currentBal != null) ...<Widget>[
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: tokens.bgElevated,
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  ),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              'Số dư hiện tại',
                              style: TextStyle(
                                fontSize: 12,
                                color: tokens.textSecondary,
                              ),
                            ),
                            Text(
                              '${currentBal.formatDisplay(masked: masked)} ($_currency)',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w800,
                                color: tokens.domainFinance,
                              ),
                            ),
                          ],
                        ),
                      ),
                      OutlinedButton.icon(
                        key: const Key('btn_reconcile_account'),
                        onPressed: _reconcileBalance,
                        icon: const Icon(Icons.tune_rounded, size: 16),
                        label: const Text('Đối chiếu'),
                      ),
                    ],
                  ),
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_account_name'),
                controller: _nameController,
                onChanged: (_) {
                  if (_errorText != null) setState(() => _errorText = null);
                },
                decoration: InputDecoration(
                  labelText: 'Tên ví / tài khoản *',
                  hintText: 'Ví dụ: Ví tiền mặt, Techcombank...',
                  border: const OutlineInputBorder(),
                  errorText: _errorText,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Loại tài khoản',
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
                children: const <String>['CASH', 'BANK', 'CREDIT', 'INVESTMENT']
                    .map(
                  (String t) => ChoiceChip(
                    label: Text(_formatAccountType(t)),
                    selected: _accountType == t,
                    onSelected: (_) => setState(() => _accountType = t),
                  ),
                ).toList(),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _currency,
                decoration: const InputDecoration(
                  labelText: 'Đơn vị tiền tệ',
                  border: OutlineInputBorder(),
                ),
                items: const <String>['VND', 'USD', 'EUR']
                    .map(
                      (String c) => DropdownMenuItem<String>(
                        value: c,
                        child: Text(c),
                      ),
                    )
                    .toList(),
                onChanged: (String? v) {
                  if (v != null) setState(() => _currency = v);
                },
              ),
              if (!_isEditing) ...<Widget>[
                const SizedBox(height: 12),
                TextField(
                  key: const Key('input_account_opening_balance'),
                  controller: _openingController,
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  decoration: InputDecoration(
                    labelText: 'Số dư ban đầu ($_currency)',
                    border: const OutlineInputBorder(),
                  ),
                ),
              ],
              if (_isEditing) ...<Widget>[
                const SizedBox(height: 10),
                SwitchListTile(
                  key: const Key('switch_archive_account'),
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Lưu trữ tài khoản'),
                  subtitle: Text(
                    _archived
                        ? 'Tài khoản đã ẩn khỏi lựa chọn giao dịch mới.'
                        : 'Bật nếu không còn dùng ví này hàng ngày.',
                  ),
                  value: _archived,
                  onChanged: (bool val) => setState(() => _archived = val),
                ),
                if (_archived && activeRecurringCount > 0)
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: tokens.warning.withValues(alpha: 0.14),
                      borderRadius:
                          BorderRadius.circular(LifeOsTokens.radiusMd),
                    ),
                    child: Text(
                      'Lưu trữ tài khoản sẽ tự động tạm dừng $activeRecurringCount giao dịch định kỳ đang hoạt động trên ví này.',
                      style: TextStyle(fontSize: 12.5, color: tokens.warning),
                    ),
                  ),
              ],
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_save_account'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _save,
                child: Text(_isEditing ? 'Lưu thay đổi' : 'Tạo ví'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet tạo mới hoặc sửa Giao dịch Thu / Chi / Chuyển ví.
class FinanceTransactionSheet extends ConsumerStatefulWidget {
  const FinanceTransactionSheet({
    super.key,
    this.transaction,
    this.initialKind = 'EXPENSE',
  });

  final LocalEntityRow? transaction;
  final String initialKind;

  @override
  ConsumerState<FinanceTransactionSheet> createState() =>
      _FinanceTransactionSheetState();
}

class _FinanceTransactionSheetState
    extends ConsumerState<FinanceTransactionSheet> {
  static final RegExp _isoDatePattern = RegExp(r'^\d{4}-\d{2}-\d{2}$');

  late final TextEditingController _amountController;
  late final TextEditingController _dateController;
  late final TextEditingController _noteController;

  late String _mode; // 'EXPENSE' | 'INCOME' | 'TRANSFER'
  String? _accountId;
  String? _toAccountId;
  String? _categoryId;
  String? _errorText;

  bool get _isEditing => widget.transaction != null;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final String todayIso = ref
        .read(localRepositoryProvider)
        .getUserDayClock(userId)
        .businessDateForUtc(ref.read(referenceUtcProvider));

    if (_isEditing) {
      final Map<String, dynamic> p = widget.transaction!.payload;
      final String rawKind =
          ((p['kind'] ?? p['type']) as String? ?? 'EXPENSE').toUpperCase();
      _mode = rawKind == 'INCOME' ? 'INCOME' : 'EXPENSE';
      _amountController = TextEditingController(
        text: (p['amount'] ?? '').toString(),
      );
      _dateController = TextEditingController(
        text: ((p['occurredOn'] ?? p['transactionDate']) as String?) ?? todayIso,
      );
      _noteController =
          TextEditingController(text: (p['note'] as String?) ?? '');
      _accountId = p['accountId'] as String?;
      _categoryId = p['categoryId'] as String?;
    } else {
      _mode = widget.initialKind.toUpperCase();
      _amountController = TextEditingController();
      _dateController = TextEditingController(text: todayIso);
      _noteController = TextEditingController();
    }
  }

  @override
  void dispose() {
    _amountController.dispose();
    _dateController.dispose();
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

  Future<void> _quickCreateCategory() async {
    final TextEditingController ctrl = TextEditingController();
    final String? createdName = await showDialog<String>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: Text(
          _mode == 'INCOME'
              ? 'Thêm danh mục thu nhập'
              : 'Thêm danh mục chi tiêu',
        ),
        content: TextField(
          key: const Key('input_quick_category_name'),
          controller: ctrl,
          autofocus: true,
          decoration: const InputDecoration(
            labelText: 'Tên danh mục *',
            border: OutlineInputBorder(),
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_quick_category'),
            onPressed: () {
              final String v = ctrl.text.trim();
              if (v.isEmpty) return;
              Navigator.of(ctx).pop(v);
            },
            child: const Text('Tạo'),
          ),
        ],
      ),
    );
    ctrl.dispose();

    if (createdName != null && mounted) {
      final String newCatId =
          ref.read(localRepositoryProvider).finance.createFinanceCategory(
                userId: ref.read(currentUserIdProvider),
                name: createdName,
                kind: _mode == 'INCOME' ? 'INCOME' : 'EXPENSE',
                nowUtc: ref.read(referenceUtcProvider),
              );
      setState(() => _categoryId = newCatId);
    }
  }

  String _extractErrorCode(Object e) {
    if (e is StateError) return e.message;
    return e.toString();
  }

  void _save(List<LocalEntityRow> activeAccounts) {
    final Decimal10k? amt =
        _tryParseDecimal(_amountController.text.trim());
    if (amt == null || amt.scaledValue <= 0) {
      setState(() => _errorText = 'Vui lòng nhập số tiền hợp lệ (> 0).');
      return;
    }
    final String dateIso = _dateController.text.trim();
    if (!_isoDatePattern.hasMatch(dateIso)) {
      setState(() => _errorText = 'Ngày giao dịch cần đúng định dạng YYYY-MM-DD.');
      return;
    }

    final String effectiveAccId = _accountId ??
        (activeAccounts.isNotEmpty ? activeAccounts.first.id : '');
    if (effectiveAccId.isEmpty) {
      setState(() => _errorText = 'Vui lòng tạo ít nhất 1 ví trước.');
      return;
    }

    final String userId = ref.read(currentUserIdProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);

    try {
      if (_mode == 'TRANSFER') {
        final String destId = _toAccountId ?? '';
        if (destId.isEmpty || destId == effectiveAccId) {
          setState(
            () => _errorText = 'Vui lòng chọn ví đích khác ví nguồn.',
          );
          return;
        }
        repo.createAccountTransfer(
          userId: userId,
          fromAccountId: effectiveAccId,
          toAccountId: destId,
          amount: amt,
          occurredOnIso: dateIso,
          note: _noteController.text.trim(),
          nowUtc: nowUtc,
        );
      } else if (_isEditing) {
        repo.updateFinanceTransaction(
          userId: userId,
          transactionId: widget.transaction!.id,
          accountId: effectiveAccId,
          categoryId: _categoryId,
          clearCategory: _categoryId == null,
          kind: _mode,
          amount: amt,
          occurredOnIso: dateIso,
          note: _noteController.text,
          nowUtc: nowUtc,
        );
      } else {
        repo.createFinanceTransaction(
          userId: userId,
          accountId: effectiveAccId,
          kind: _mode,
          amount: amt,
          occurredOnIso: dateIso,
          categoryId: _categoryId,
          note: _noteController.text.trim().isEmpty
              ? null
              : _noteController.text.trim(),
          nowUtc: nowUtc,
        );
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      setState(
        () => _errorText = ViStrings.mapBackendErrorCode(_extractErrorCode(e)),
      );
    }
  }

  Future<void> _confirmDelete() async {
    if (!_isEditing) return;
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa giao dịch?'),
        content: const Text('Giao dịch này sẽ bị xóa khỏi sổ thu chi của bạn.'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            key: const Key('btn_confirm_delete_transaction'),
            style: FilledButton.styleFrom(backgroundColor: tokens.danger),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(ViStrings.deleteAction),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      ref.read(localRepositoryProvider).deleteFinanceTransaction(
            userId: ref.read(currentUserIdProvider),
            transactionId: widget.transaction!.id,
            nowUtc: ref.read(referenceUtcProvider),
          );
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<LocalEntityRow> accounts = (ref
                .watch(
                  entitiesByTypeStreamProvider(SyncEntityType.financeAccount),
                )
                .valueOrNull ??
            const <LocalEntityRow>[])
        .where(
          (LocalEntityRow r) =>
              r.payload['archived'] != true && r.payload['archivedAt'] == null,
        )
        .toList();

    final List<LocalEntityRow> allCategories = ref
            .watch(
              entitiesByTypeStreamProvider(SyncEntityType.financeCategory),
            )
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> categories = allCategories
        .where(
          (LocalEntityRow c) =>
              (c.payload['kind'] as String?)?.toUpperCase() == _mode,
        )
        .toList();

    final String? effectiveAccId =
        accounts.any((LocalEntityRow a) => a.id == _accountId)
            ? _accountId
            : (accounts.isNotEmpty ? accounts.first.id : null);
    final String? effectiveToAccId =
        accounts.any((LocalEntityRow a) => a.id == _toAccountId && a.id != effectiveAccId)
            ? _toAccountId
            : (accounts
                    .where((LocalEntityRow a) => a.id != effectiveAccId)
                    .map((LocalEntityRow a) => a.id)
                    .firstOrNull);
    if (_mode == 'TRANSFER' && _toAccountId == null && effectiveToAccId != null) {
      _toAccountId = effectiveToAccId;
    }

    final String? validCategoryId =
        categories.any((LocalEntityRow c) => c.id == _categoryId)
            ? _categoryId
            : null;
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
                      _isEditing ? 'Chỉnh sửa giao dịch' : 'Ghi nhận giao dịch',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  if (_isEditing)
                    IconButton(
                      key: const Key('btn_delete_transaction'),
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
              if (!_isEditing) ...<Widget>[
                SegmentedButton<String>(
                  segments: const <ButtonSegment<String>>[
                    ButtonSegment<String>(
                      value: 'EXPENSE',
                      label: Text('Chi tiêu'),
                    ),
                    ButtonSegment<String>(
                      value: 'INCOME',
                      label: Text('Thu nhập'),
                    ),
                    ButtonSegment<String>(
                      value: 'TRANSFER',
                      label: Text('Chuyển ví'),
                    ),
                  ],
                  selected: <String>{_mode},
                  onSelectionChanged: (Set<String> s) {
                    setState(() {
                      _mode = s.first;
                      _categoryId = null;
                      _errorText = null;
                    });
                  },
                ),
                const SizedBox(height: 12),
              ],
              TextField(
                key: const Key('input_tx_amount'),
                controller: _amountController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                onChanged: (_) {
                  if (_errorText != null) setState(() => _errorText = null);
                },
                decoration: InputDecoration(
                  labelText: 'Số tiền *',
                  border: const OutlineInputBorder(),
                  errorText: _errorText,
                ),
              ),
              const SizedBox(height: 12),
              if (accounts.isEmpty)
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: tokens.warning.withValues(alpha: 0.14),
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  ),
                  child: const Text(
                    'Bạn chưa có ví đang hoạt động. Hãy tạo một ví trước khi ghi giao dịch.',
                  ),
                )
              else ...<Widget>[
                DropdownButtonFormField<String>(
                  key: const Key('dropdown_tx_account'),
                  initialValue: effectiveAccId,
                  decoration: InputDecoration(
                    labelText: _mode == 'TRANSFER' ? 'Từ ví *' : 'Ví / Tài khoản *',
                    border: const OutlineInputBorder(),
                  ),
                  items: accounts.map((LocalEntityRow a) {
                    final String curr =
                        ((a.payload['currency'] as String?) ?? 'VND').toUpperCase();
                    return DropdownMenuItem<String>(
                      value: a.id,
                      child: Text('${a.payload['name'] ?? 'Ví'} ($curr)'),
                    );
                  }).toList(),
                  onChanged: (String? v) => setState(() => _accountId = v),
                ),
                if (_mode == 'TRANSFER') ...<Widget>[
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    key: const Key('dropdown_tx_to_account'),
                    initialValue: effectiveToAccId,
                    decoration: const InputDecoration(
                      labelText: 'Đến ví *',
                      border: OutlineInputBorder(),
                    ),
                    items: accounts
                        .where((LocalEntityRow a) => a.id != effectiveAccId)
                        .map((LocalEntityRow a) {
                      final String curr =
                          ((a.payload['currency'] as String?) ?? 'VND')
                              .toUpperCase();
                      return DropdownMenuItem<String>(
                        value: a.id,
                        child: Text('${a.payload['name'] ?? 'Ví'} ($curr)'),
                      );
                    }).toList(),
                    onChanged: (String? v) => setState(() => _toAccountId = v),
                  ),
                ],
              ],
              if (_mode != 'TRANSFER') ...<Widget>[
                const SizedBox(height: 12),
                Row(
                  children: <Widget>[
                    Expanded(
                      child: DropdownButtonFormField<String?>(
                        initialValue: validCategoryId,
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
                              child: Text((c.payload['name'] as String?) ?? ''),
                            ),
                          ),
                        ],
                        onChanged: (String? v) =>
                            setState(() => _categoryId = v),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton(
                      key: const Key('btn_quick_add_category'),
                      tooltip: 'Thêm danh mục nhanh',
                      onPressed: _quickCreateCategory,
                      icon: const Icon(Icons.add_circle_outline_rounded),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_tx_date'),
                controller: _dateController,
                decoration: InputDecoration(
                  labelText: 'Ngày phát sinh *',
                  hintText: 'YYYY-MM-DD',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    tooltip: 'Chọn ngày',
                    icon: const Icon(Icons.calendar_today_rounded, size: 18),
                    onPressed: _pickDate,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_tx_note'),
                controller: _noteController,
                decoration: const InputDecoration(
                  labelText: 'Ghi chú (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_save_transaction'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: accounts.isEmpty ? null : () => _save(accounts),
                child: Text(_isEditing ? 'Lưu thay đổi' : 'Lưu giao dịch'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet quản lý Ngân sách tháng (`budget`): liệt kê, thêm mới, chỉnh sửa và xóa.
class BudgetManagementSheet extends ConsumerStatefulWidget {
  const BudgetManagementSheet({super.key});

  @override
  ConsumerState<BudgetManagementSheet> createState() =>
      _BudgetManagementSheetState();
}

class _BudgetManagementSheetState extends ConsumerState<BudgetManagementSheet> {
  final TextEditingController _amountController = TextEditingController();
  String? _selectedCategoryId;
  String? _errorText;

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  void _addBudget(String defaultCurrency) {
    final Decimal10k? amt =
        _tryParseDecimal(_amountController.text.trim());
    if (amt == null || amt.scaledValue <= 0) {
      setState(() => _errorText = 'Vui lòng nhập hạn mức > 0.');
      return;
    }
    final String userId = ref.read(currentUserIdProvider);
    ref.read(localRepositoryProvider).createBudget(
          userId: userId,
          categoryId: _selectedCategoryId,
          currency: defaultCurrency,
          amount: amt,
          nowUtc: ref.read(referenceUtcProvider),
        );
    _amountController.clear();
    setState(() => _errorText = null);
  }

  Future<void> _editBudgetDialog(LocalEntityRow budget) async {
    final TextEditingController ctrl = TextEditingController(
      text: (budget.payload['amount'] ?? '').toString(),
    );
    String? err;
    final Decimal10k? next = await showDialog<Decimal10k>(
      context: context,
      builder: (BuildContext ctx) => StatefulBuilder(
        builder: (BuildContext ctx, StateSetter setLocal) => AlertDialog(
          title: const Text('Sửa hạn mức ngân sách'),
          content: TextField(
            controller: ctrl,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: 'Hạn mức tháng',
              border: const OutlineInputBorder(),
              errorText: err,
            ),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text(ViStrings.cancelAction),
            ),
            FilledButton(
              onPressed: () {
                final Decimal10k? p = _tryParseDecimal(ctrl.text.trim());
                if (p == null || p.scaledValue <= 0) {
                  setLocal(() => err = 'Số tiền phải lớn hơn 0.');
                  return;
                }
                Navigator.of(ctx).pop(p);
              },
              child: const Text(ViStrings.saveAction),
            ),
          ],
        ),
      ),
    );
    ctrl.dispose();
    if (next != null && mounted) {
      ref.read(localRepositoryProvider).updateBudget(
            userId: ref.read(currentUserIdProvider),
            budgetId: budget.id,
            amount: next,
            nowUtc: ref.read(referenceUtcProvider),
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final bool masked = ref.watch(privacyMaskAmountsProvider);
    final String defaultCurrency =
        ((repo.getUserSettings(userId)['defaultCurrency'] as String?) ?? 'VND')
            .toUpperCase();

    final List<LocalEntityRow> budgets = (ref
                .watch(entitiesByTypeStreamProvider(SyncEntityType.budget))
                .valueOrNull ??
            const <LocalEntityRow>[])
        .where(
          (LocalEntityRow r) =>
              r.payload['archived'] != true && r.payload['archivedAt'] == null,
        )
        .toList();
    final List<LocalEntityRow> categories = (ref
                .watch(
                  entitiesByTypeStreamProvider(SyncEntityType.financeCategory),
                )
                .valueOrNull ??
            const <LocalEntityRow>[])
        .where(
          (LocalEntityRow c) =>
              (c.payload['kind'] as String?)?.toUpperCase() != 'INCOME',
        )
        .toList();

    final Map<String, String> catNames = <String, String>{
      for (final LocalEntityRow c in categories)
        c.id: (c.payload['name'] as String?) ?? 'Danh mục',
    };
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
                'Ngân sách chi tiêu tháng',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 10),
              if (budgets.isEmpty)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Text(
                    'Chưa có hạn mức nào. Hãy thêm hạn mức bên dưới để theo dõi tốc độ chi tiêu.',
                    style: TextStyle(color: tokens.textSecondary),
                  ),
                )
              else
                ...budgets.map((LocalEntityRow b) {
                  final String? catId = b.payload['categoryId'] as String?;
                  final String title = catId != null && catNames.containsKey(catId)
                      ? catNames[catId]!
                      : 'Ngân sách chung';
                  final Decimal10k amt = Decimal10k.parseJsonString(
                    (b.payload['amount'] ?? '0').toString(),
                  );
                  return ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(title),
                    subtitle: Text(
                      '${amt.formatDisplay(masked: masked)} (${(b.payload['currency'] as String?) ?? defaultCurrency})',
                    ),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        IconButton(
                          tooltip: 'Sửa',
                          icon: const Icon(Icons.edit_outlined, size: 20),
                          onPressed: () => _editBudgetDialog(b),
                        ),
                        IconButton(
                          tooltip: ViStrings.deleteAction,
                          icon: Icon(
                            Icons.delete_outline_rounded,
                            size: 20,
                            color: tokens.danger,
                          ),
                          onPressed: () => repo.deleteBudget(
                            userId: userId,
                            budgetId: b.id,
                            nowUtc: ref.read(referenceUtcProvider),
                          ),
                        ),
                      ],
                    ),
                  );
                }),
              const Divider(height: 24),
              DropdownButtonFormField<String?>(
                initialValue: categories.any(
                  (LocalEntityRow c) => c.id == _selectedCategoryId,
                )
                    ? _selectedCategoryId
                    : null,
                decoration: const InputDecoration(
                  labelText: 'Danh mục áp dụng',
                  border: OutlineInputBorder(),
                ),
                items: <DropdownMenuItem<String?>>[
                  const DropdownMenuItem<String?>(
                    value: null,
                    child: Text('Toàn bộ chi tiêu tháng'),
                  ),
                  ...categories.map(
                    (LocalEntityRow c) => DropdownMenuItem<String?>(
                      value: c.id,
                      child: Text((c.payload['name'] as String?) ?? ''),
                    ),
                  ),
                ],
                onChanged: (String? v) =>
                    setState(() => _selectedCategoryId = v),
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('input_budget_amount'),
                controller: _amountController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: 'Hạn mức tháng ($defaultCurrency) *',
                  hintText: 'Ví dụ: 5000000',
                  border: const OutlineInputBorder(),
                  errorText: _errorText,
                ),
              ),
              const SizedBox(height: 12),
              FilledButton.icon(
                key: const Key('btn_save_budget'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: () => _addBudget(defaultCurrency),
                icon: const Icon(Icons.add_rounded),
                label: const Text('Thêm ngân sách'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet xem lịch sử & tìm kiếm toàn bộ giao dịch thu chi + chuyển ví.
class TransactionHistorySheet extends ConsumerStatefulWidget {
  const TransactionHistorySheet({super.key});

  @override
  ConsumerState<TransactionHistorySheet> createState() =>
      _TransactionHistorySheetState();
}

class _TransactionHistorySheetState
    extends ConsumerState<TransactionHistorySheet> {
  String _query = '';
  String _kindFilter = 'ALL'; // ALL | EXPENSE | INCOME | TRANSFER

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final bool masked = ref.watch(privacyMaskAmountsProvider);

    final List<LocalEntityRow> txs = ref
            .watch(
              entitiesByTypeStreamProvider(SyncEntityType.financeTransaction),
            )
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> transfers = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.accountTransfer))
            .valueOrNull ??
        const <LocalEntityRow>[];

    final Map<String, String> accountNames = <String, String>{
      for (final LocalEntityRow a in ref
              .watch(
                entitiesByTypeStreamProvider(SyncEntityType.financeAccount),
              )
              .valueOrNull ??
          const <LocalEntityRow>[])
        a.id: (a.payload['name'] as String?) ?? 'Ví',
    };

    final List<({LocalEntityRow row, bool isTransfer, String occurredOn})>
        entries =
        <({LocalEntityRow row, bool isTransfer, String occurredOn})>[];

    for (final LocalEntityRow r in txs) {
      final String kind =
          ((r.payload['kind'] ?? r.payload['type']) as String? ?? 'EXPENSE')
              .toUpperCase();
      if (_kindFilter != 'ALL' && _kindFilter != kind) continue;
      final String date =
          ((r.payload['occurredOn'] ?? r.payload['transactionDate']) as String?) ??
              '';
      final String note = (r.payload['note'] as String?) ?? '';
      final String acc = accountNames[r.payload['accountId']] ?? '';
      if (_query.isNotEmpty &&
          !note.toLowerCase().contains(_query) &&
          !acc.toLowerCase().contains(_query) &&
          !date.contains(_query)) {
        continue;
      }
      entries.add((row: r, isTransfer: false, occurredOn: date));
    }
    for (final LocalEntityRow tr in transfers) {
      if (_kindFilter != 'ALL' && _kindFilter != 'TRANSFER') continue;
      final String date = (tr.payload['occurredOn'] as String?) ?? '';
      final String from = accountNames[tr.payload['fromAccountId']] ?? 'Ví';
      final String to = accountNames[tr.payload['toAccountId']] ?? 'Ví';
      final String note = (tr.payload['note'] as String?) ?? '';
      if (_query.isNotEmpty &&
          !from.toLowerCase().contains(_query) &&
          !to.toLowerCase().contains(_query) &&
          !note.toLowerCase().contains(_query)) {
        continue;
      }
      entries.add((row: tr, isTransfer: true, occurredOn: date));
    }
    entries.sort((a, b) => b.occurredOn.compareTo(a.occurredOn));

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.78,
        minChildSize: 0.5,
        maxChildSize: 0.94,
        builder: (BuildContext context, ScrollController scrollCtrl) {
          return Column(
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    Text(
                      'Lịch sử giao dịch',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      key: const Key('input_search_transactions'),
                      onChanged: (String v) =>
                          setState(() => _query = v.trim().toLowerCase()),
                      decoration: const InputDecoration(
                        hintText: 'Tìm theo ghi chú, ví, ngày...',
                        prefixIcon: Icon(Icons.search_rounded),
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      children: const <MapEntry<String, String>>[
                        MapEntry<String, String>('ALL', 'Tất cả'),
                        MapEntry<String, String>('EXPENSE', 'Chi'),
                        MapEntry<String, String>('INCOME', 'Thu'),
                        MapEntry<String, String>('TRANSFER', 'Chuyển ví'),
                      ].map((MapEntry<String, String> item) {
                        return ChoiceChip(
                          label: Text(item.value),
                          selected: _kindFilter == item.key,
                          onSelected: (_) =>
                              setState(() => _kindFilter = item.key),
                        );
                      }).toList(),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: entries.isEmpty
                    ? Center(
                        child: Text(
                          'Không có giao dịch nào phù hợp.',
                          style: TextStyle(color: tokens.textSecondary),
                        ),
                      )
                    : ListView.separated(
                        controller: scrollCtrl,
                        padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
                        itemCount: entries.length,
                        separatorBuilder: (BuildContext ctx, int idx) =>
                            const Divider(height: 1),
                        itemBuilder: (BuildContext ctx, int idx) {
                          final item = entries[idx];
                          final LocalEntityRow row = item.row;
                          final Decimal10k amt = Decimal10k.parseJsonString(
                            (row.payload['amount'] ?? '0').toString(),
                          );
                          if (item.isTransfer) {
                            final String from =
                                accountNames[row.payload['fromAccountId']] ?? 'Ví';
                            final String to =
                                accountNames[row.payload['toAccountId']] ?? 'Ví';
                            return ListTile(
                              contentPadding: EdgeInsets.zero,
                              leading: const Icon(Icons.swap_horiz_rounded),
                              title: Text('$from → $to'),
                              subtitle: Text(
                                '${item.occurredOn}${(row.payload['note'] as String?)?.isNotEmpty == true ? ' · ${row.payload['note']}' : ''}',
                              ),
                              trailing: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: <Widget>[
                                  Text(
                                    amt.formatDisplay(masked: masked),
                                    style: const TextStyle(
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  IconButton(
                                    tooltip: ViStrings.deleteAction,
                                    icon: Icon(
                                      Icons.delete_outline_rounded,
                                      size: 20,
                                      color: tokens.danger,
                                    ),
                                    onPressed: () => repo.deleteAccountTransfer(
                                      userId: userId,
                                      transferId: row.id,
                                      nowUtc: ref.read(referenceUtcProvider),
                                    ),
                                  ),
                                ],
                              ),
                            );
                          }
                          final String kind =
                              ((row.payload['kind'] ?? row.payload['type'])
                                          as String? ??
                                      'EXPENSE')
                                  .toUpperCase();
                          final bool isIncome = kind == 'INCOME';
                          final String acc =
                              accountNames[row.payload['accountId']] ?? 'Ví';
                          final String note =
                              (row.payload['note'] as String?) ?? '';
                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(
                              isIncome
                                  ? Icons.arrow_downward_rounded
                                  : Icons.arrow_upward_rounded,
                              color: isIncome ? tokens.success : tokens.danger,
                            ),
                            title: Text(
                              note.isNotEmpty
                                  ? note
                                  : (isIncome ? 'Khoản thu' : 'Khoản chi'),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            subtitle: Text('${item.occurredOn} · $acc'),
                            trailing: Text(
                              '${isIncome ? '+' : '-'}${amt.formatDisplay(masked: masked)}',
                              style: TextStyle(
                                fontWeight: FontWeight.w700,
                                color: isIncome ? tokens.success : tokens.danger,
                              ),
                            ),
                            onTap: () {
                              showModalBottomSheet<void>(
                                context: context,
                                isScrollControlled: true,
                                backgroundColor: Colors.transparent,
                                builder: (_) =>
                                    FinanceTransactionSheet(transaction: row),
                              );
                            },
                          );
                        },
                      ),
              ),
            ],
          );
        },
      ),
    );
  }
}
