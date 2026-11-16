import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../data/ai_models.dart';
import '../../data/ai_providers.dart';

/// Biểu mẫu quản lý Quyền riêng tư & Consent theo 5 miền dữ liệu (AWP-1).
class AiConsentSheet extends ConsumerStatefulWidget {
  const AiConsentSheet({super.key});

  @override
  ConsumerState<AiConsentSheet> createState() => _AiConsentSheetState();
}

class _AiConsentSheetState extends ConsumerState<AiConsentSheet> {
  late bool _taskCalendar;
  late bool _habit;
  late bool _goal;
  late bool _notes;
  late bool _finance;
  bool _termsAccepted = true;
  bool _isSaving = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    final AsyncValue<AiPreferencesResponse> state =
        ref.read(aiPreferencesProvider);
    final AiPreferencesResponse prefs = state.valueOrNull ??
        AiPreferencesResponse.initialDefault();
    _taskCalendar = prefs.taskCalendar;
    _habit = prefs.habit;
    _goal = prefs.goal;
    _notes = prefs.notes;
    _finance = prefs.finance;
  }

  int get _selectedCount {
    int c = 0;
    if (_taskCalendar) c++;
    if (_habit) c++;
    if (_goal) c++;
    if (_notes) c++;
    if (_finance) c++;
    return c;
  }

  Future<void> _handleSave() async {
    setState(() {
      _isSaving = true;
      _errorMessage = null;
    });

    try {
      final AiPreferencesRequest req = AiPreferencesRequest(
        taskCalendar: _taskCalendar,
        habit: _habit,
        goal: _goal,
        notes: _notes,
        finance: _finance,
        providerTermsAccepted: _termsAccepted,
      );
      await ref.read(aiPreferencesProvider.notifier).updatePreferences(req);
      if (mounted) {
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã cập nhật quyền riêng tư Trợ lý AI thành công.'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString();
          _isSaving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.9,
        ),
        child: Column(
          children: <Widget>[
          // Thanh tiêu đề
          Padding(
            padding: const EdgeInsets.fromLTRB(
              LifeOsTokens.space4,
              LifeOsTokens.space3,
              LifeOsTokens.space2,
              LifeOsTokens.space2,
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.shield_outlined,
                  color: tokens.accentPrimary,
                  size: 24,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    'Quyền riêng tư & Dữ liệu AI',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
                IconButton(
                  key: const Key('btn_close_ai_consent'),
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          // Nội dung cuộn
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(LifeOsTokens.space4),
              children: <Widget>[
                // Giới thiệu nguyên tắc an toàn
                LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        children: <Widget>[
                          Icon(
                            Icons.lock_clock_outlined,
                            size: 18,
                            color: tokens.accentPrimary,
                          ),
                          const SizedBox(width: LifeOsTokens.space2),
                          Text(
                            'Nguyên tắc An toàn & Minh bạch',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                              color: tokens.textPrimary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: LifeOsTokens.space2),
                      Text(
                        '• Mặc định toàn bộ quyền đọc dữ liệu đều TẮT.\n'
                        '• AI chỉ đọc các trường cần thiết theo miền bạn cho phép.\n'
                        '• Không chia sẻ dữ liệu huấn luyện mô hình.\n'
                        '• Thu hồi quyền có hiệu lực ngay lập tức từ yêu cầu kế tiếp.',
                        style: TextStyle(
                          fontSize: 13,
                          height: 1.45,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space3),

                // Trạng thái hiện tại
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: LifeOsTokens.space3,
                    vertical: LifeOsTokens.space2,
                  ),
                  decoration: BoxDecoration(
                    color: _selectedCount == 0
                        ? tokens.warning.withValues(alpha: 0.12)
                        : tokens.accentPrimary.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusSm),
                  ),
                  child: Row(
                    children: <Widget>[
                      Icon(
                        _selectedCount == 0
                            ? Icons.info_outline_rounded
                            : Icons.check_circle_outline_rounded,
                        size: 18,
                        color: _selectedCount == 0
                            ? tokens.warning
                            : tokens.accentPrimary,
                      ),
                      const SizedBox(width: LifeOsTokens.space2),
                      Expanded(
                        child: Text(
                          _selectedCount == 0
                              ? 'Chưa bật miền nào: Trợ lý AI đang hoạt động ở chế độ trò chuyện chung (không có ngữ cảnh dữ liệu cá nhân).'
                              : 'Đã cấp quyền đọc cho $_selectedCount/5 miền dữ liệu.',
                          style: TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.w600,
                            color: tokens.textPrimary,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space4),

                Text(
                  'Chọn các miền dữ liệu cho phép AI đọc:',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: tokens.textPrimary,
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space2),

                // 1. Công việc & Lịch
                _buildDomainSwitch(
                  key: const Key('switch_consent_task'),
                  icon: Icons.task_alt_rounded,
                  domainColor: tokens.domainTask,
                  title: 'Công việc & Lịch',
                  description:
                      'Đọc danh sách việc cần làm, deadline và lịch trình để gợi ý kế hoạch ngày/tuần.',
                  value: _taskCalendar,
                  onChanged: (bool val) => setState(() => _taskCalendar = val),
                  tokens: tokens,
                ),
                const SizedBox(height: LifeOsTokens.space2),

                // 2. Thói quen
                _buildDomainSwitch(
                  key: const Key('switch_consent_habit'),
                  icon: Icons.repeat_rounded,
                  domainColor: tokens.domainHabit,
                  title: 'Thói quen hàng ngày',
                  description:
                      'Đọc chuỗi duy trì và lịch sử thực hiện thói quen để đưa ra nhận xét tiến độ.',
                  value: _habit,
                  onChanged: (bool val) => setState(() => _habit = val),
                  tokens: tokens,
                ),
                const SizedBox(height: LifeOsTokens.space2),

                // 3. Mục tiêu dài hạn
                _buildDomainSwitch(
                  key: const Key('switch_consent_goal'),
                  icon: Icons.flag_outlined,
                  domainColor: tokens.domainGoal,
                  title: 'Mục tiêu & Cột mốc',
                  description:
                      'Đọc các mục tiêu dài hạn và hạn định cột mốc để tư vấn lộ trình thực hiện.',
                  value: _goal,
                  onChanged: (bool val) => setState(() => _goal = val),
                  tokens: tokens,
                ),
                const SizedBox(height: LifeOsTokens.space2),

                // 4. Ghi chú
                _buildDomainSwitch(
                  key: const Key('switch_consent_notes'),
                  icon: Icons.description_outlined,
                  domainColor: tokens.domainKnowledge,
                  title: 'Ghi chú cá nhân',
                  description:
                      'Đọc nội dung ghi chú bạn chọn để tóm tắt ý chính và trích xuất hành động.',
                  value: _notes,
                  onChanged: (bool val) => setState(() => _notes = val),
                  tokens: tokens,
                ),
                const SizedBox(height: LifeOsTokens.space3),

                // 5. Tài chính (Độc lập kèm cảnh báo)
                Container(
                  decoration: BoxDecoration(
                    border: Border.all(
                      color: _finance ? tokens.warning : tokens.borderSubtle,
                      width: 1.5,
                    ),
                    borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                  ),
                  child: Material(
                    type: MaterialType.transparency,
                    child: Column(
                      children: <Widget>[
                        _buildDomainSwitch(
                          key: const Key('switch_consent_finance'),
                          icon: Icons.account_balance_wallet_outlined,
                          domainColor: tokens.domainFinance,
                          title: 'Tài chính cá nhân (Độc lập)',
                          description:
                              'Chỉ đọc tổng hợp số liệu thu chi danh mục và ngân sách để phân tích xu hướng.',
                          value: _finance,
                          onChanged: (bool val) =>
                              setState(() => _finance = val),
                          tokens: tokens,
                          isCard: false,
                        ),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(LifeOsTokens.space3),
                          decoration: BoxDecoration(
                            color: tokens.warning.withValues(alpha: 0.1),
                            borderRadius: const BorderRadius.vertical(
                              bottom: Radius.circular(LifeOsTokens.radiusMd),
                            ),
                          ),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Icon(
                                Icons.warning_amber_rounded,
                                size: 18,
                                color: tokens.warning,
                              ),
                              const SizedBox(width: LifeOsTokens.space2),
                              Expanded(
                                child: Text(
                                  'Cảnh báo bảo vệ tài sản: Quyền này chỉ cho phép AI ĐỌC số liệu tổng hợp. '
                                  'Tuyệt đối KHÔNG có bất kỳ giao dịch tự động nào, không ghi/sửa hay thay đổi số dư.',
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                    height: 1.4,
                                    color: tokens.textPrimary,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: LifeOsTokens.space4),

                // Checkbox điều khoản
                CheckboxListTile(
                  key: const Key('checkbox_consent_terms'),
                  value: _termsAccepted,
                  onChanged: (bool? val) =>
                      setState(() => _termsAccepted = val ?? true),
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                  title: Text(
                    'Tôi đồng ý với chính sách xử lý dữ liệu AI (phiên bản 2026-11-10; không chia sẻ huấn luyện mô hình).',
                    style: TextStyle(
                      fontSize: 12.5,
                      color: tokens.textSecondary,
                    ),
                  ),
                ),

                if (_errorMessage != null) ...<Widget>[
                  const SizedBox(height: LifeOsTokens.space2),
                  Text(
                    _errorMessage!,
                    style: TextStyle(
                      fontSize: 13,
                      color: tokens.danger,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],

                const SizedBox(height: LifeOsTokens.space4),
                // Nút lưu
                FilledButton.icon(
                  key: const Key('btn_save_ai_consent'),
                  style: FilledButton.styleFrom(
                    minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                  ),
                  onPressed: _isSaving ? null : _handleSave,
                  icon: _isSaving
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.check_rounded),
                  label: Text(_isSaving ? 'Đang lưu...' : 'Lưu tùy chọn quyền riêng tư'),
                ),
              ],
            ),
          ),
        ],
      ),
    ),
  );
}

  Widget _buildDomainSwitch({
    required Key key,
    required IconData icon,
    required Color domainColor,
    required String title,
    required String description,
    required bool value,
    required ValueChanged<bool> onChanged,
    required LifeOsTokens tokens,
    bool isCard = true,
  }) {
    final Widget content = SwitchListTile(
      key: key,
      value: value,
      onChanged: onChanged,
      secondary: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: domainColor.withValues(alpha: 0.14),
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusSm),
        ),
        child: Icon(icon, color: domainColor, size: 20),
      ),
      title: Text(
        title,
        style: TextStyle(
          fontSize: 14.5,
          fontWeight: FontWeight.w700,
          color: tokens.textPrimary,
        ),
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 2),
        child: Text(
          description,
          style: TextStyle(
            fontSize: 12.5,
            color: tokens.textSecondary,
          ),
        ),
      ),
    );

    if (!isCard) return content;

    return LifeOsCard(
      elevation: 1,
      padding: EdgeInsets.zero,
      child: content,
    );
  }
}

