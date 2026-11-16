import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/vi_strings.dart';
import '../../../core/money/decimal10k.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../data/lifeos_local_repository.dart';

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
  final TextEditingController _taskController = TextEditingController();
  final TextEditingController _noteTitleController = TextEditingController();
  final TextEditingController _noteBodyController = TextEditingController();
  String _financeDigits = '50000';

  @override
  void dispose() {
    _taskController.dispose();
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

    return Padding(
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
                decoration: const InputDecoration(
                  hintText: 'Nhập tên việc cần làm hôm nay...',
                ),
              ),
              const SizedBox(height: LifeOsTokens.space3),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  key: const Key('btn_submit_quick_task'),
                  onPressed: () {
                    final String title = _taskController.text.trim().isEmpty
                        ? 'Công việc mới trong ngày'
                        : _taskController.text.trim();
                    repo.createTask(
                      userId: userId,
                      title: title,
                      dueDate: todayIso,
                      isMyDay: true,
                    );
                    Navigator.of(context).pop();
                  },
                  child: const Text('Lưu việc cần làm'),
                ),
              ),
            ] else if (_tabIndex == 1) ...<Widget>[
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
                onChanged: (String next) =>
                    setState(() => _financeDigits = next),
              ),
              const SizedBox(height: LifeOsTokens.space3),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  key: const Key('btn_submit_quick_finance'),
                  onPressed: () {
                    final Decimal10k amt =
                        CustomFinanceNumpad.toDecimal10k(_financeDigits);
                    repo.createFinanceTransaction(
                      userId: userId,
                      accountId: 'default-wallet',
                      kind: 'EXPENSE',
                      amount: amt,
                      occurredOnIso: todayIso,
                      note: 'Ghi nhanh',
                    );
                    Navigator.of(context).pop();
                  },
                  child: const Text('Ghi khoản chi'),
                ),
              ),
            ] else ...<Widget>[
              TextField(
                controller: _noteTitleController,
                decoration:
                    const InputDecoration(hintText: 'Tiêu đề ghi chú...'),
              ),
              const SizedBox(height: LifeOsTokens.space2),
              TextField(
                controller: _noteBodyController,
                maxLines: 3,
                decoration: const InputDecoration(
                  hintText: 'Nội dung ghi chú (tự động lưu trên máy)...',
                ),
              ),
              const SizedBox(height: LifeOsTokens.space3),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    repo.saveNote(
                      userId: userId,
                      title: _noteTitleController.text.trim().isEmpty
                          ? 'Ghi chú nhanh'
                          : _noteTitleController.text.trim(),
                      content: _noteBodyController.text,
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
    );
  }
}

