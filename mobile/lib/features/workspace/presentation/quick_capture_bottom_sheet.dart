import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/app_database.dart';
import '../../../core/l10n/vi_strings.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/sync/entity_types.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../data/lifeos_local_repository.dart';

/// Lưu nháp cục bộ trong phiên cho `QuickCaptureBottomSheet` để đóng mở lại không mất dữ liệu đang nhập.
@immutable
class QuickCaptureDraftState {
  const QuickCaptureDraftState({
    this.taskTitle = '',
    this.taskPriority = 'MEDIUM',
    this.financeDigits = '50000',
    this.financeKind = 'EXPENSE',
    this.financeNote = '',
    this.noteTitle = '',
    this.noteBody = '',
  });

  final String taskTitle;
  final String taskPriority;
  final String financeDigits;
  final String financeKind;
  final String financeNote;
  final String noteTitle;
  final String noteBody;

  QuickCaptureDraftState copyWith({
    String? taskTitle,
    String? taskPriority,
    String? financeDigits,
    String? financeKind,
    String? financeNote,
    String? noteTitle,
    String? noteBody,
  }) {
    return QuickCaptureDraftState(
      taskTitle: taskTitle ?? this.taskTitle,
      taskPriority: taskPriority ?? this.taskPriority,
      financeDigits: financeDigits ?? this.financeDigits,
      financeKind: financeKind ?? this.financeKind,
      financeNote: financeNote ?? this.financeNote,
      noteTitle: noteTitle ?? this.noteTitle,
      noteBody: noteBody ?? this.noteBody,
    );
  }
}

final StateProvider<QuickCaptureDraftState> quickCaptureDraftProvider =
    StateProvider<QuickCaptureDraftState>(
  (Ref ref) => const QuickCaptureDraftState(),
);

/// Sheet **Thêm nhanh 1 chạm (`Quick Capture`)**: Việc cần làm · Khoản thu/chi · Ghi chú nhanh.
class QuickCaptureBottomSheet extends ConsumerStatefulWidget {
  const QuickCaptureBottomSheet({super.key});

  @override
  ConsumerState<QuickCaptureBottomSheet> createState() =>
      _QuickCaptureBottomSheetState();
}

class _QuickCaptureBottomSheetState
    extends ConsumerState<QuickCaptureBottomSheet> {
  int _tabIndex = 0;
  late final TextEditingController _taskController;
  late final TextEditingController _financeNoteController;
  late final TextEditingController _noteTitleController;
  late final TextEditingController _noteBodyController;

  late String _taskPriority;
  late String _financeDigits;
  late String _financeKind;
  String? _selectedAccountId;
  String? _selectedCategoryId;
  String? _taskError;
  String? _noteError;

  @override
  void initState() {
    super.initState();
    final QuickCaptureDraftState draft = ref.read(quickCaptureDraftProvider);
    _taskController = TextEditingController(text: draft.taskTitle);
    _financeNoteController = TextEditingController(text: draft.financeNote);
    _noteTitleController = TextEditingController(text: draft.noteTitle);
    _noteBodyController = TextEditingController(text: draft.noteBody);
    _taskPriority = draft.taskPriority;
    _financeDigits = draft.financeDigits;
    _financeKind = draft.financeKind;
  }

  void _syncDraft() {
    ref.read(quickCaptureDraftProvider.notifier).state = QuickCaptureDraftState(
      taskTitle: _taskController.text,
      taskPriority: _taskPriority,
      financeDigits: _financeDigits,
      financeKind: _financeKind,
      financeNote: _financeNoteController.text,
      noteTitle: _noteTitleController.text,
      noteBody: _noteBodyController.text,
    );
  }

  @override
  void dispose() {
    _taskController.dispose();
    _financeNoteController.dispose();
    _noteTitleController.dispose();
    _noteBodyController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String userId = ref.watch(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.watch(localRepositoryProvider);
    final DateTime refUtc = ref.watch(referenceUtcProvider);
    final String todayIso =
        repo.getUserDayClock(userId).businessDateForUtc(refUtc);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    final List<LocalEntityRow> allAccounts = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeAccount))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> activeAccounts = allAccounts
        .where((LocalEntityRow a) => a.payload['archived'] != true)
        .toList();
    final String? effectiveAccountId =
        activeAccounts.any((LocalEntityRow a) => a.id == _selectedAccountId)
            ? _selectedAccountId
            : (activeAccounts.isNotEmpty ? activeAccounts.first.id : null);

    final List<LocalEntityRow> allCategories = ref
            .watch(entitiesByTypeStreamProvider(SyncEntityType.financeCategory))
            .valueOrNull ??
        const <LocalEntityRow>[];
    final List<LocalEntityRow> matchingCategories = allCategories
        .where(
          (LocalEntityRow c) =>
              c.payload['archived'] != true &&
              (c.payload['kind'] as String? ?? 'EXPENSE').toUpperCase() ==
                  _financeKind,
        )
        .toList();
    final String? effectiveCategoryId = matchingCategories
            .any((LocalEntityRow c) => c.id == _selectedCategoryId)
        ? _selectedCategoryId
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
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Wrap(
                spacing: 8,
                runSpacing: 4,
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: <Widget>[
                  Text(
                    ViStrings.quickCaptureTitle,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  Text(
                    ViStrings.draftAutoSaved,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: tokens.success,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: LifeOsTokens.space3),
              SegmentedButton<int>(
                segments: const <ButtonSegment<int>>[
                  ButtonSegment<int>(
                    value: 0,
                    label: Text(ViStrings.quickTabTask),
                    icon: Icon(Icons.check_box_outlined, size: 18),
                  ),
                  ButtonSegment<int>(
                    value: 1,
                    label: Text(ViStrings.quickTabFinance),
                    icon: Icon(Icons.payments_outlined, size: 18),
                  ),
                  ButtonSegment<int>(
                    value: 2,
                    label: Text(ViStrings.quickTabNote),
                    icon: Icon(Icons.edit_note_rounded, size: 18),
                  ),
                ],
                selected: <int>{_tabIndex},
                onSelectionChanged: (Set<int> s) =>
                    setState(() => _tabIndex = s.first),
              ),
              const SizedBox(height: LifeOsTokens.space4),
              if (_tabIndex == 0) ...<Widget>[
                TextField(
                  key: const Key('input_quick_task_title'),
                  controller: _taskController,
                  autofocus: true,
                  onChanged: (_) {
                    if (_taskError != null) setState(() => _taskError = null);
                    _syncDraft();
                  },
                  decoration: InputDecoration(
                    hintText: 'Nhập tên việc cần làm hôm nay...',
                    errorText: _taskError,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),
                Wrap(
                  spacing: 6,
                  runSpacing: 4,
                  children: const <String>['NONE', 'LOW', 'MEDIUM', 'HIGH']
                      .map((String p) {
                    return ChoiceChip(
                      label: Text(ViStrings.formatTaskPriority(p)),
                      selected: _taskPriority == p,
                      onSelected: (bool sel) {
                        if (!sel) return;
                        setState(() => _taskPriority = p);
                        _syncDraft();
                      },
                    );
                  }).toList(),
                ),
                const SizedBox(height: LifeOsTokens.space3),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    key: const Key('btn_submit_quick_task'),
                    onPressed: () {
                      final String title = _taskController.text.trim();
                      if (title.isEmpty) {
                        setState(
                          () => _taskError = 'Vui lòng nhập tên công việc.',
                        );
                        return;
                      }
                      repo.createTask(
                        userId: userId,
                        title: title,
                        priority: _taskPriority,
                        dueDate: todayIso,
                        isMyDay: true,
                        nowUtc: refUtc,
                      );
                      ref.read(quickCaptureDraftProvider.notifier).state =
                          ref.read(quickCaptureDraftProvider).copyWith(
                                taskTitle: '',
                                taskPriority: 'MEDIUM',
                              );
                      Navigator.of(context).pop();
                    },
                    child: const Text('Lưu việc cần làm'),
                  ),
                ),
              ] else if (_tabIndex == 1) ...<Widget>[
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
                  ],
                  selected: <String>{_financeKind},
                  onSelectionChanged: (Set<String> s) {
                    setState(() {
                      _financeKind = s.first;
                      _selectedCategoryId = null;
                    });
                    _syncDraft();
                  },
                ),
                const SizedBox(height: LifeOsTokens.space2),
                if (activeAccounts.isEmpty) ...<Widget>[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: tokens.warning.withValues(alpha: 0.12),
                      borderRadius:
                          BorderRadius.circular(LifeOsTokens.radiusMd),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: <Widget>[
                        Text(
                          'Bạn chưa có ví / tài khoản nào đang hoạt động. Hãy tạo nhanh ví đầu tiên để ghi giao dịch.',
                          style: TextStyle(
                            fontSize: 12.5,
                            color: tokens.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 8),
                        FilledButton.tonalIcon(
                          key: const Key('btn_quick_create_cash_wallet'),
                          onPressed: () {
                            final String newAccId = repo.createFinanceAccount(
                              userId: userId,
                              name: 'Ví tiền mặt',
                              accountType: 'CASH',
                              openingBalance: Decimal10k.zero,
                              nowUtc: refUtc,
                            );
                            setState(() => _selectedAccountId = newAccId);
                          },
                          icon: const Icon(Icons.add_card_rounded, size: 18),
                          label: const Text('Tạo Ví tiền mặt (0 ₫)'),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: LifeOsTokens.space2),
                ] else ...<Widget>[
                  DropdownButtonFormField<String>(
                    key: const Key('dropdown_quick_finance_account'),
                    initialValue: effectiveAccountId,
                    decoration: const InputDecoration(
                      labelText: 'Tài khoản / Ví *',
                      isDense: true,
                      border: OutlineInputBorder(),
                    ),
                    items: activeAccounts
                        .map(
                          (LocalEntityRow acc) => DropdownMenuItem<String>(
                            value: acc.id,
                            child: Text(
                              '${(acc.payload['name'] as String?) ?? 'Ví'} (${(acc.payload['currency'] as String?) ?? 'VND'})',
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (String? v) =>
                        setState(() => _selectedAccountId = v),
                  ),
                  const SizedBox(height: LifeOsTokens.space2),
                  if (matchingCategories.isNotEmpty) ...<Widget>[
                    DropdownButtonFormField<String?>(
                      key: const Key('dropdown_quick_finance_category'),
                      initialValue: effectiveCategoryId,
                      decoration: const InputDecoration(
                        labelText: 'Danh mục (tùy chọn)',
                        isDense: true,
                        border: OutlineInputBorder(),
                      ),
                      items: <DropdownMenuItem<String?>>[
                        const DropdownMenuItem<String?>(
                          value: null,
                          child: Text('Không chọn danh mục'),
                        ),
                        ...matchingCategories.map(
                          (LocalEntityRow cat) => DropdownMenuItem<String?>(
                            value: cat.id,
                            child: Text(
                              (cat.payload['name'] as String?) ?? 'Danh mục',
                            ),
                          ),
                        ),
                      ],
                      onChanged: (String? v) =>
                          setState(() => _selectedCategoryId = v),
                    ),
                    const SizedBox(height: LifeOsTokens.space2),
                  ],
                ],
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  decoration: BoxDecoration(
                    color: tokens.bgElevated,
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  ),
                  alignment: Alignment.centerRight,
                  child: Text(
                    CustomFinanceNumpad.toDecimal10k(_financeDigits)
                        .formatDisplay(),
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w700,
                      fontFeatures: <FontFeature>[FontFeature.tabularFigures()],
                    ),
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),
                CustomFinanceNumpad(
                  rawDigits: _financeDigits,
                  onChanged: (String next) {
                    setState(() => _financeDigits = next);
                    _syncDraft();
                  },
                ),
                const SizedBox(height: LifeOsTokens.space2),
                TextField(
                  key: const Key('input_quick_finance_note'),
                  controller: _financeNoteController,
                  onChanged: (_) => _syncDraft(),
                  decoration: const InputDecoration(
                    hintText: 'Ghi chú (ví dụ: Cà phê, Ăn trưa)...',
                    isDense: true,
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space3),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    key: const Key('btn_submit_quick_finance'),
                    onPressed: effectiveAccountId == null
                        ? null
                        : () {
                            final Decimal10k amt =
                                CustomFinanceNumpad.toDecimal10k(
                                    _financeDigits);
                            if (amt.scaledValue <= 0) return;
                            final String rawNote =
                                _financeNoteController.text.trim();
                            repo.createFinanceTransaction(
                              userId: userId,
                              accountId: effectiveAccountId,
                              categoryId: effectiveCategoryId,
                              kind: _financeKind,
                              amount: amt,
                              occurredOnIso: todayIso,
                              note: rawNote.isEmpty ? null : rawNote,
                              nowUtc: refUtc,
                            );
                            ref
                                .read(quickCaptureDraftProvider.notifier)
                                .state = ref
                                .read(quickCaptureDraftProvider)
                                .copyWith(
                                  financeDigits: '50000',
                                  financeNote: '',
                                );
                            Navigator.of(context).pop();
                          },
                    child: Text(
                      _financeKind == 'INCOME'
                          ? 'Ghi khoản thu'
                          : 'Ghi khoản chi',
                    ),
                  ),
                ),
              ] else ...<Widget>[
                TextField(
                  key: const Key('input_quick_note_title'),
                  controller: _noteTitleController,
                  onChanged: (_) {
                    if (_noteError != null) setState(() => _noteError = null);
                    _syncDraft();
                  },
                  decoration: InputDecoration(
                    hintText: 'Tiêu đề ghi chú...',
                    errorText: _noteError,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),
                TextField(
                  key: const Key('input_quick_note_body'),
                  controller: _noteBodyController,
                  maxLines: 3,
                  onChanged: (_) {
                    if (_noteError != null) setState(() => _noteError = null);
                    _syncDraft();
                  },
                  decoration: const InputDecoration(
                    hintText: 'Nội dung ghi chú (tự động lưu trên máy)...',
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space3),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    key: const Key('btn_submit_quick_note'),
                    onPressed: () {
                      final String title = _noteTitleController.text.trim();
                      final String body = _noteBodyController.text.trim();
                      if (title.isEmpty && body.isEmpty) {
                        setState(
                          () => _noteError =
                              'Vui lòng nhập tiêu đề hoặc nội dung ghi chú.',
                        );
                        return;
                      }
                      repo.saveNote(
                        userId: userId,
                        title: title.isEmpty ? 'Ghi chú nhanh' : title,
                        content: _noteBodyController.text,
                        nowUtc: refUtc,
                      );
                      ref.read(quickCaptureDraftProvider.notifier).state =
                          ref.read(quickCaptureDraftProvider).copyWith(
                                noteTitle: '',
                                noteBody: '',
                              );
                      Navigator.of(context).pop();
                    },
                    child: const Text('Lưu ghi chú'),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
