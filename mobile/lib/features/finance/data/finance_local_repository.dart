import 'package:uuid/uuid.dart';

import '../../../core/database/app_database.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/sync/entity_types.dart';

/// Repository cục bộ cho miền **Personal Finance** (`docs/phases/PHASE_3_FINANCE.md` D100–D119 & `SYNC_PROTOCOL.md` §3.6).
///
/// Tuân thủ tuyệt đối:
/// - Không dùng `double` cho tiền tệ (`Decimal10k` số nguyên 64-bit × 10⁴).
/// - Tài khoản (`financial_account` D102): `{name, accountType, currency, openingBalance, archived}`.
/// - Giao dịch (`financial_transaction` §3.6 dòng 144): `{accountId, categoryId, kind ('INCOME'|'EXPENSE'), amount, occurredOn, note}`.
/// - Chuyển khoản (`account_transfer` §3.6 dòng 145, D102, D104, D118): thực thể riêng `account_transfer`
///   với `{fromAccountId, toAccountId, amount, occurredOn, note}`; cấm chuyển khoản khác `currency` (`FINANCE_CURRENCY_MISMATCH`).
/// - Công thức số dư (`D102` dòng 52): `openingBalance + Σ INCOME − Σ EXPENSE + Σ transfers_in − Σ transfers_out` (`occurredOn <= asOfDate`).
class FinanceLocalRepository {
  FinanceLocalRepository({
    required this.db,
    Uuid? uuid,
  }) : _uuid = uuid ?? const Uuid();

  final AppDatabase db;
  final Uuid _uuid;

  String createFinanceAccount({
    required String userId,
    required String name,
    String accountType = 'BANK',
    String currency = 'VND',
    required Decimal10k openingBalance,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.financeAccount,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'name': name.trim(),
        'accountType': accountType.toUpperCase(),
        'currency': currency.toUpperCase(),
        'openingBalance': openingBalance.toJsonString(),
        'archived': false,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  String createFinanceCategory({
    required String userId,
    required String name,
    required String kind, // 'INCOME' | 'EXPENSE'
    String? parentId,
    String? systemKey,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.financeCategory,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'name': name.trim(),
        'kind': kind.toUpperCase(),
        if (parentId != null) 'parentId': parentId,
        if (systemKey != null) 'systemKey': systemKey,
        'archived': false,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tạo giao dịch Thu/Chi (`financial_transaction` — `SYNC_PROTOCOL.md` §3.6 dòng 144 & `openapi.yaml` dòng 6481–6506).
  String createFinanceTransaction({
    required String userId,
    required String accountId,
    required String kind, // 'INCOME' | 'EXPENSE'
    required Decimal10k amount,
    required String occurredOnIso, // YYYY-MM-DD
    String? categoryId,
    String? note,
    DateTime? nowUtc,
  }) {
    final String normalizedKind = kind.toUpperCase();
    if (normalizedKind != 'INCOME' && normalizedKind != 'EXPENSE') {
      throw ArgumentError('kind của financial_transaction phải là INCOME hoặc EXPENSE: "$kind"');
    }
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.financeTransaction,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'accountId': accountId,
        'categoryId': categoryId,
        'kind': normalizedKind,
        'amount': amount.abs().toJsonString(),
        'occurredOn': occurredOnIso,
        'note': note?.trim(),
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tạo thực thể Chuyển khoản (`account_transfer` — `PHASE_3_FINANCE.md` D102/D104/D118 & `SYNC_PROTOCOL.md` §3.6 dòng 145).
  ///
  /// Kiểm tra bắt buộc:
  /// - `fromAccountId != toAccountId`.
  /// - Hai tài khoản phải cùng `currency` (nếu khác ném lỗi `FINANCE_CURRENCY_MISMATCH` theo D104/D118).
  String createAccountTransfer({
    required String userId,
    required String fromAccountId,
    required String toAccountId,
    required Decimal10k amount,
    required String occurredOnIso,
    String? note,
    DateTime? nowUtc,
  }) {
    if (fromAccountId == toAccountId) {
      throw ArgumentError('FINANCE_SAME_ACCOUNT: Tài khoản nguồn và tài khoản đích phải khác nhau');
    }
    final LocalEntityRow? fromAcc = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.financeAccount,
      entityId: fromAccountId,
    );
    final LocalEntityRow? toAcc = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.financeAccount,
      entityId: toAccountId,
    );
    if (fromAcc != null && toAcc != null) {
      final String fromCur =
          (fromAcc.payload['currency'] as String? ?? 'VND').toUpperCase();
      final String toCur =
          (toAcc.payload['currency'] as String? ?? 'VND').toUpperCase();
      if (fromCur != toCur) {
        throw StateError(
          'FINANCE_CURRENCY_MISMATCH: Không hỗ trợ chuyển khoản khác tiền tệ ($fromCur -> $toCur) theo D104/D118',
        );
      }
    }

    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.accountTransfer,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'fromAccountId': fromAccountId,
        'toAccountId': toAccountId,
        'amount': amount.abs().toJsonString(),
        'occurredOn': occurredOnIso,
        'note': note?.trim(),
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tính số dư tài khoản (`PHASE_3_FINANCE.md` D102 dòng 52):
  /// `balance = openingBalance + Σ INCOME − Σ EXPENSE + Σ transfers_in − Σ transfers_out`
  /// (chỉ tính các bản ghi chưa xóa mềm và có `occurredOn <= asOfDateIso` nếu truyền `asOfDateIso`).
  Decimal10k computeAccountBalance({
    required String userId,
    required String accountId,
    String? asOfDateIso,
  }) {
    final LocalEntityRow? acc = db.getEntity(
      userId: userId,
      entityType: SyncEntityType.financeAccount,
      entityId: accountId,
    );
    if (acc == null) return Decimal10k.zero;

    final String rawOpening =
        (acc.payload['openingBalance'] ?? acc.payload['initialBalance'] ?? '0')
            .toString();
    Decimal10k balance = Decimal10k.parseJsonString(rawOpening);

    final List<LocalEntityRow> txs = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.financeTransaction,
    );
    for (final LocalEntityRow tx in txs) {
      if (tx.payload['accountId'] != accountId) continue;
      final String? occurredOn =
          (tx.payload['occurredOn'] ?? tx.payload['transactionDate']) as String?;
      if (asOfDateIso != null &&
          occurredOn != null &&
          occurredOn.compareTo(asOfDateIso) > 0) {
        continue;
      }
      final String kind =
          ((tx.payload['kind'] ?? tx.payload['type']) as String? ?? 'EXPENSE')
              .toUpperCase();
      final Decimal10k amt = Decimal10k.parseJsonString(
        (tx.payload['amount'] ?? '0').toString(),
      );
      if (kind == 'INCOME') {
        balance = balance + amt;
      } else if (kind == 'EXPENSE') {
        balance = balance - amt;
      }
    }

    final List<LocalEntityRow> transfers = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.accountTransfer,
    );
    for (final LocalEntityRow tr in transfers) {
      final String? occurredOn = tr.payload['occurredOn'] as String?;
      if (asOfDateIso != null &&
          occurredOn != null &&
          occurredOn.compareTo(asOfDateIso) > 0) {
        continue;
      }
      final Decimal10k amt = Decimal10k.parseJsonString(
        (tr.payload['amount'] ?? '0').toString(),
      );
      if (tr.payload['toAccountId'] == accountId) {
        balance = balance + amt;
      }
      if (tr.payload['fromAccountId'] == accountId) {
        balance = balance - amt;
      }
    }
    return balance;
  }

  /// Đối soát số dư tài khoản (`PHASE_3_FINANCE.md` D112 dòng 62 & D119 dòng 68):
  /// So sánh `actualBalance` với `computeAccountBalance(asOfDateIso: occurredOnIso)`;
  /// nếu lệch, tạo một giao dịch `INCOME` hoặc `EXPENSE` gắn vào danh mục hệ thống `systemKey = 'ADJUSTMENT'`.
  String? reconcileAccountBalance({
    required String userId,
    required String accountId,
    required Decimal10k actualBalance,
    required String occurredOnIso,
    DateTime? nowUtc,
  }) {
    final Decimal10k current = computeAccountBalance(
      userId: userId,
      accountId: accountId,
      asOfDateIso: occurredOnIso,
    );
    final Decimal10k delta = actualBalance - current;
    if (delta.isZero) return null;

    final String kind = delta.isPositive ? 'INCOME' : 'EXPENSE';
    final String adjustmentCategoryId = _ensureAdjustmentCategory(
      userId: userId,
      kind: kind,
      nowUtc: nowUtc,
    );

    return createFinanceTransaction(
      userId: userId,
      accountId: accountId,
      categoryId: adjustmentCategoryId,
      kind: kind,
      amount: delta.abs(),
      occurredOnIso: occurredOnIso,
      note: 'Điều chỉnh đối soát số dư (D112)',
      nowUtc: nowUtc,
    );
  }

  String _ensureAdjustmentCategory({
    required String userId,
    required String kind,
    DateTime? nowUtc,
  }) {
    final List<LocalEntityRow> cats = db.listEntities(
      userId: userId,
      entityType: SyncEntityType.financeCategory,
    );
    for (final LocalEntityRow c in cats) {
      if (c.payload['systemKey'] == 'ADJUSTMENT' &&
          (c.payload['kind'] as String?)?.toUpperCase() == kind) {
        return c.id;
      }
    }
    return createFinanceCategory(
      userId: userId,
      name: 'Điều chỉnh số dư',
      kind: kind,
      systemKey: 'ADJUSTMENT',
      nowUtc: nowUtc,
    );
  }

  String createBudget({
    required String userId,
    String? categoryId,
    required String currency,
    required Decimal10k amount,
    DateTime? nowUtc,
  }) {
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.budget,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'categoryId': categoryId,
        'currency': currency.toUpperCase(),
        'amount': amount.toJsonString(),
        'archived': false,
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  // ===========================================================================
  // GIAO DỊCH ĐỊNH KỲ (`recurring_transaction`) — khớp Recurrence.java (NO RRULE)
  // ===========================================================================

  /// Tạo hoặc cập nhật giao dịch định kỳ (`frequency` in {DAILY, WEEKLY, MONTHLY, YEARLY}).
  String saveRecurringTransaction({
    required String userId,
    String? recurringId,
    required String accountId,
    String? categoryId,
    required String kind, // 'INCOME' | 'EXPENSE'
    required Decimal10k amount,
    String? note,
    required String frequency, // 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'
    required int intervalCount,
    required String startOnIso, // YYYY-MM-DD
    String? endOnIso, // YYYY-MM-DD?
    String status = 'ACTIVE', // 'ACTIVE' | 'PAUSED'
    DateTime? nowUtc,
  }) {
    final String normalizedKind = kind.trim().toUpperCase();
    if (normalizedKind != 'INCOME' && normalizedKind != 'EXPENSE') {
      throw ArgumentError('kind phải là INCOME hoặc EXPENSE: "$kind"');
    }
    final String normalizedFreq = frequency.trim().toUpperCase();
    if (!const <String>{'DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'}
        .contains(normalizedFreq)) {
      throw ArgumentError('frequency không hợp lệ: "$frequency"');
    }
    if (intervalCount < 1 || intervalCount > 365) {
      throw RangeError('intervalCount phải trong khoảng 1..365');
    }
    if (amount.scaledValue <= 0) {
      throw ArgumentError('Số tiền định kỳ phải lớn hơn 0.');
    }

    final String id = recurringId ?? _uuid.v4();
    final bool isUpdate = recurringId != null &&
        db.getEntity(
              userId: userId,
              entityType: SyncEntityType.recurringTransaction,
              entityId: recurringId,
            ) !=
            null;

    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.recurringTransaction,
      entityId: id,
      op: isUpdate ? 'UPDATE' : 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'accountId': accountId,
        'categoryId': categoryId,
        'kind': normalizedKind,
        'amount': amount.abs().toJsonString(),
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
        'frequency': normalizedFreq,
        'intervalCount': intervalCount,
        'startOn': startOnIso,
        'endOn': endOnIso,
        'status': status.trim().toUpperCase() == 'PAUSED' ? 'PAUSED' : 'ACTIVE',
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tính chính xác các kỳ lặp kế tiếp theo đúng thuật toán `Recurrence.java`
  /// (`startOn + k * intervalCount`, kẹp cuối tháng cho MONTHLY/YEARLY).
  static List<String> computeRecurringOccurrencesPreview({
    required String frequency,
    required int intervalCount,
    required String startOnIso,
    String? endOnIso,
    int count = 3,
  }) {
    final DateTime? start = _tryParseIsoDate(startOnIso);
    if (start == null || intervalCount < 1 || intervalCount > 365) {
      return const <String>[];
    }
    final DateTime? end = endOnIso == null || endOnIso.trim().isEmpty
        ? null
        : _tryParseIsoDate(endOnIso);
    final String freq = frequency.trim().toUpperCase();
    final List<String> result = <String>[];

    for (int k = 0; k < count; k++) {
      final int step = k * intervalCount;
      final DateTime occ = switch (freq) {
        'DAILY' => DateTime.utc(start.year, start.month, start.day + step),
        'WEEKLY' =>
          DateTime.utc(start.year, start.month, start.day + step * 7),
        'MONTHLY' => _addMonthsClamped(start, step),
        'YEARLY' => _addYearsClamped(start, step),
        _ => start,
      };
      if (end != null && occ.isAfter(end)) break;
      result.add(
        '${occ.year.toString().padLeft(4, '0')}-${occ.month.toString().padLeft(2, '0')}-${occ.day.toString().padLeft(2, '0')}',
      );
    }
    return result;
  }

  static DateTime _addMonthsClamped(DateTime start, int monthsToAdd) {
    final int totalMonths = (start.year * 12 + (start.month - 1)) + monthsToAdd;
    final int targetYear = totalMonths ~/ 12;
    final int targetMonth = (totalMonths % 12) + 1;
    final int lastDayOfMonth = DateTime.utc(targetYear, targetMonth + 1, 0).day;
    final int clampedDay =
        start.day > lastDayOfMonth ? lastDayOfMonth : start.day;
    return DateTime.utc(targetYear, targetMonth, clampedDay);
  }

  static DateTime _addYearsClamped(DateTime start, int yearsToAdd) {
    final int targetYear = start.year + yearsToAdd;
    final int lastDayOfMonth = DateTime.utc(targetYear, start.month + 1, 0).day;
    final int clampedDay =
        start.day > lastDayOfMonth ? lastDayOfMonth : start.day;
    return DateTime.utc(targetYear, start.month, clampedDay);
  }

  static DateTime? _tryParseIsoDate(String raw) {
    final List<String> parts = raw.trim().split('-');
    if (parts.length != 3) return null;
    final int? y = int.tryParse(parts[0]);
    final int? m = int.tryParse(parts[1]);
    final int? d = int.tryParse(parts[2]);
    if (y == null || m == null || d == null) return null;
    return DateTime.utc(y, m, d);
  }

  // ===========================================================================
  // MỤC TIÊU TIẾT KIỆM (`saving_goal` & `saving_contribution`) — Decimal10k
  // ===========================================================================

  /// Tạo hoặc cập nhật mục tiêu tiết kiệm (`saving_goal`).
  String saveSavingGoal({
    required String userId,
    String? goalId,
    required String name,
    required String currency,
    required Decimal10k targetAmount,
    String? targetOnIso,
    String status = 'ACTIVE', // 'ACTIVE' | 'ARCHIVED'
    DateTime? nowUtc,
  }) {
    if (targetAmount.scaledValue <= 0) {
      throw ArgumentError('Số tiền mục tiêu tiết kiệm phải lớn hơn 0.');
    }
    final String id = goalId ?? _uuid.v4();
    final bool isUpdate = goalId != null &&
        db.getEntity(
              userId: userId,
              entityType: SyncEntityType.savingGoal,
              entityId: goalId,
            ) !=
            null;
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.savingGoal,
      entityId: id,
      op: isUpdate ? 'UPDATE' : 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'name': name.trim(),
        'currency': currency.trim().toUpperCase(),
        'targetAmount': targetAmount.abs().toJsonString(),
        if (targetOnIso != null && targetOnIso.trim().isNotEmpty)
          'targetOn': targetOnIso.trim(),
        'status':
            status.trim().toUpperCase() == 'ARCHIVED' ? 'ARCHIVED' : 'ACTIVE',
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Ghi nhận khoản đóng góp cho mục tiêu tiết kiệm (`saving_contribution`).
  String addSavingContribution({
    required String userId,
    required String goalId,
    required Decimal10k amount,
    required String occurredOnIso,
    String? note,
    DateTime? nowUtc,
  }) {
    if (amount.scaledValue <= 0) {
      throw ArgumentError('Số tiền đóng góp phải lớn hơn 0.');
    }
    final String id = _uuid.v4();
    db.mutateLocalAndEnqueueOutbox(
      userId: userId,
      entityType: SyncEntityType.savingContribution,
      entityId: id,
      op: 'CREATE',
      payload: <String, dynamic>{
        'id': id,
        'goalId': goalId,
        'amount': amount.abs().toJsonString(),
        'occurredOn': occurredOnIso,
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
      },
      nowUtc: nowUtc,
    );
    return id;
  }

  /// Tính tổng số tiền đã góp vào một `saving_goal` hoàn toàn bằng `Decimal10k`.
  Decimal10k computeSavingGoalSavedAmount({
    required String userId,
    required String goalId,
  }) {
    int sumScaled = 0;
    for (final LocalEntityRow row in db.listEntities(
      userId: userId,
      entityType: SyncEntityType.savingContribution,
    )) {
      if (row.payload['goalId'] == goalId) {
        final Decimal10k amt = Decimal10k.parseJsonString(
          (row.payload['amount'] ?? '0').toString(),
        );
        sumScaled += amt.scaledValue;
      }
    }
    return Decimal10k.fromScaled(sumScaled);
  }
}

