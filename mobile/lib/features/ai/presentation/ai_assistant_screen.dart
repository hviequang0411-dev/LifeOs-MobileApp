import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/vi_strings.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/time/user_day_clock.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../data/ai_api.dart';
import '../data/ai_models.dart';
import '../data/ai_providers.dart';
import 'widgets/ai_consent_sheet.dart';
import 'widgets/ai_context_disclosure_sheet.dart';
import 'widgets/ai_conversation_list_sheet.dart';
import 'widgets/ai_disclaimer_banner.dart';
import 'widgets/ai_plans_and_insights_sheets.dart';
import 'widgets/ai_usage_and_limits_sheet.dart';

/// Màn hình Trợ lý AI chính (AWP-1, AWP-2, AWP-3).
///
/// Tính năng Online-Only:
/// - Không đi qua Outbox.
/// - Không lưu hội thoại hay tin nhắn vào CSDL SQLite nội bộ (chỉ cache phiên trong bộ nhớ).
class AiAssistantScreen extends ConsumerStatefulWidget {
  const AiAssistantScreen({super.key});

  @override
  ConsumerState<AiAssistantScreen> createState() => _AiAssistantScreenState();
}

class _AiAssistantScreenState extends ConsumerState<AiAssistantScreen> {
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  List<AiMessageResponse> _messages = <AiMessageResponse>[];
  String? _messagesNextCursor;
  bool _hasMoreMessages = false;
  bool _isLoadingOlderMessages = false;
  bool _isLoadingHistory = false;
  bool _isSending = false;
  bool _loadingPlan = false;
  int _waitingSeconds = 0;
  Timer? _waitingTimer;

  String? _lastFailedMessage;
  AiErrorDescriptor? _currentError;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initSession();
    });
  }

  @override
  void dispose() {
    _waitingTimer?.cancel();
    _inputController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _initSession() async {
    // 1. Tải cấu hình consent hiện tại
    await ref.read(aiPreferencesProvider.notifier).loadPreferences();

    // 2. Mở hoặc tạo hội thoại
    final String? convId = ref.read(aiActiveConversationIdProvider);
    if (convId == null) {
      await _startNewConversation(silent: true);
    } else {
      await _loadConversationDetail(convId);
    }
  }

  Future<void> _startNewConversation({bool silent = false}) async {
    setState(() {
      _isLoadingHistory = true;
      _messages.clear();
      _messagesNextCursor = null;
      _hasMoreMessages = false;
      _isLoadingOlderMessages = false;
      _currentError = null;
    });

    final AiApiClient api = ref.read(aiApiClientProvider);
    try {
      final AiConversationResponse conv = await api.createConversation();
      ref.read(aiActiveConversationIdProvider.notifier).state = conv.id;
      if (mounted) {
        setState(() => _isLoadingHistory = false);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoadingHistory = false;
          _currentError = ViStrings.mapAiError(_extractErrorCode(e));
        });
      }
    }
  }

  Future<void> _loadConversationDetail(String convId) async {
    setState(() {
      _isLoadingHistory = true;
      _currentError = null;
    });

    final AiApiClient api = ref.read(aiApiClientProvider);
    try {
      final AiConversationDetailResponse detail =
          await api.getConversation(convId);
      ref.read(aiActiveConversationIdProvider.notifier).state = convId;
      if (mounted) {
        setState(() {
          _messages = List<AiMessageResponse>.from(detail.messages.items);
          _messagesNextCursor = detail.messages.nextCursor;
          _hasMoreMessages = detail.messages.hasMore;
          _isLoadingHistory = false;
        });
        _scrollToBottom();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoadingHistory = false;
          _currentError = ViStrings.mapAiError(_extractErrorCode(e));
        });
      }
    }
  }

  Future<void> _loadOlderMessages() async {
    final String? convId = ref.read(aiActiveConversationIdProvider);
    if (convId == null ||
        _isLoadingOlderMessages ||
        !_hasMoreMessages ||
        _messagesNextCursor == null) {
      return;
    }
    setState(() => _isLoadingOlderMessages = true);
    final AiApiClient api = ref.read(aiApiClientProvider);
    try {
      final AiConversationDetailResponse detail = await api.getConversation(
        convId,
        cursor: _messagesNextCursor,
      );
      if (mounted) {
        final Set<String> seen =
            _messages.map((AiMessageResponse m) => m.id).toSet();
        final List<AiMessageResponse> olderUnique = detail.messages.items
            .where((AiMessageResponse m) => !seen.contains(m.id))
            .toList();
        setState(() {
          _messages = <AiMessageResponse>[...olderUnique, ..._messages];
          _messagesNextCursor = detail.messages.nextCursor;
          _hasMoreMessages = detail.messages.hasMore;
          _isLoadingOlderMessages = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingOlderMessages = false);
        final AiErrorDescriptor err = ViStrings.mapAiError(_extractErrorCode(e));
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(err.message)),
        );
      }
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _handleSendMessage({String? retryText}) async {
    final String text = (retryText ?? _inputController.text).trim();
    if (text.isEmpty || _isSending) return;

    String? resolvedConvId = ref.read(aiActiveConversationIdProvider);
    if (resolvedConvId == null) {
      await _startNewConversation();
      resolvedConvId = ref.read(aiActiveConversationIdProvider);
    }
    if (resolvedConvId == null) {
      if (mounted) {
        setState(() {
          _lastFailedMessage = text;
          _currentError ??= ViStrings.mapAiError('NETWORK_ERROR');
        });
      }
      return;
    }

    if (retryText == null) {
      _inputController.clear();
    }

    final AsyncValue<AiPreferencesResponse> prefsAsync =
        ref.read(aiPreferencesProvider);
    final AiPreferencesResponse prefs =
        prefsAsync.valueOrNull ?? AiPreferencesResponse.initialDefault();
    final List<String> activeDomains = prefs.allowedDomainCodes;

    // Thêm tin nhắn người dùng tạm thời vào danh sách giao diện
    final AiMessageResponse tempUserMsg = AiMessageResponse(
      id: 'local_user_${DateTime.now().millisecondsSinceEpoch}',
      role: AiMessageRole.user,
      content: text,
      mode: AiGenerationMode.chat,
      contextAvailable: false,
      createdAt: DateTime.now().toUtc().toIso8601String(),
    );

    final bool alreadyHasPendingUserBubble = retryText != null &&
        _messages.isNotEmpty &&
        _messages.last.role == AiMessageRole.user &&
        _messages.last.content == text;

    setState(() {
      if (!alreadyHasPendingUserBubble) {
        _messages.add(tempUserMsg);
      }
      _isSending = true;
      _waitingSeconds = 0;
      _lastFailedMessage = null;
      _currentError = null;
    });
    _scrollToBottom();

    // Bắt đầu đếm thời gian chờ tới 30 giây theo O-AI-08
    _waitingTimer?.cancel();
    _waitingTimer = Timer.periodic(const Duration(seconds: 1), (Timer t) {
      if (!mounted) {
        t.cancel();
        return;
      }
      setState(() => _waitingSeconds = t.tick);
      if (t.tick >= 30) {
        t.cancel();
        _handleTimeoutError(text);
      }
    });

    final AiApiClient api = ref.read(aiApiClientProvider);
    final AiChatRequest req = AiChatRequest(
      message: text,
      mode: AiGenerationMode.chat,
      contextDomains: activeDomains,
    );

    try {
      final AiChatResponse resp = await api.sendMessage(resolvedConvId, req);
      _waitingTimer?.cancel();

      if (mounted) {
        setState(() {
          _isSending = false;
          _waitingSeconds = 0;
          // Thay thế hoặc thêm tin nhắn chuẩn từ phản hồi máy chủ
          _messages.removeWhere((AiMessageResponse m) => m.id == tempUserMsg.id);
          _messages.add(resp.userMessage);
          _messages.add(resp.assistantMessage);
        });
        _scrollToBottom();
      }
    } catch (e) {
      _waitingTimer?.cancel();
      if (mounted) {
        final String errCode = _extractErrorCode(e);
        setState(() {
          _isSending = false;
          _waitingSeconds = 0;
          _lastFailedMessage = text;
          _currentError = ViStrings.mapAiError(errCode);
        });
        _scrollToBottom();
      }
    }
  }

  void _handleTimeoutError(String failedText) {
    if (!_isSending) return;
    setState(() {
      _isSending = false;
      _waitingSeconds = 0;
      _lastFailedMessage = failedText;
      _currentError = const AiErrorDescriptor(
        message:
            'Yêu cầu đã quá thời gian chờ (30 giây) hoặc dịch vụ AI tạm thời không phản hồi.',
        suggestedAction: 'Thử gửi lại',
        actionType: AiErrorActionType.retry,
      );
    });
    _scrollToBottom();
  }

  String _extractErrorCode(Object error) {
    if (error is LifeOsApiException) {
      return error.errorCode;
    }
    return 'NETWORK_ERROR';
  }

  void _handleErrorAction(AiErrorActionType type) {
    switch (type) {
      case AiErrorActionType.retry:
        if (_lastFailedMessage != null) {
          _handleSendMessage(retryText: _lastFailedMessage);
        }
        break;
      case AiErrorActionType.openConsent:
        _openConsentSheet();
        break;
      case AiErrorActionType.viewLimits:
        _openUsageSheet();
        break;
      case AiErrorActionType.newConversation:
        _startNewConversation();
        break;
      case AiErrorActionType.none:
        break;
    }
  }

  void _openConsentSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => const AiConsentSheet(),
    );
  }

  void _openConversationsSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => AiConversationListSheet(
        onSelectConversation: (String id) => _loadConversationDetail(id),
        onNewConversation: () => _startNewConversation(),
      ),
    );
  }

  void _openUsageSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => const AiUsageAndLimitsSheet(),
    );
  }

  void _openContextDisclosure(String messageId) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) =>
          AiContextDisclosureSheet(messageId: messageId),
    );
  }

  Future<void> _handlePlanRequest({required bool weekly}) async {
    if (_loadingPlan) return;
    final AiPreferencesResponse prefs =
        ref.read(aiPreferencesProvider).valueOrNull ??
            AiPreferencesResponse.initialDefault();
    if (!prefs.taskCalendar) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Tính năng Lập kế hoạch cần cấp quyền miền "Công việc & Lịch" trong cài đặt.',
          ),
        ),
      );
      _openConsentSheet();
      return;
    }

    if (!ref.read(aiIsOnlineProvider)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Cần kết nối mạng để dùng AI lập kế hoạch.'),
        ),
      );
      return;
    }

    setState(() => _loadingPlan = true);
    final String userId = ref.read(currentUserIdProvider);
    final DateTime refUtc = ref.read(referenceUtcProvider);
    final UserDayClock clock =
        ref.read(localRepositoryProvider).getUserDayClock(userId);
    final String todayIso = clock.businessDateForUtc(refUtc);
    final String periodStart =
        weekly ? clock.startOfWeekForDate(todayIso) : todayIso;
    final AiApiClient api = ref.read(aiApiClientProvider);

    try {
      final List<String> domains = <String>[AiContextDomain.taskCalendar.code];
      final AiPlanResponse plan = weekly
          ? await api.createWeeklyPlan(periodStart, domains)
          : await api.createDailyPlan(periodStart, domains);
      if (mounted) {
        unawaited(
          showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            backgroundColor: Colors.transparent,
            builder: (BuildContext ctx) => AiPlanResultSheet(plan: plan),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        final AiErrorDescriptor desc =
            ViStrings.mapAiError(_extractErrorCode(e));
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(desc.message)),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _loadingPlan = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final AsyncValue<AiPreferencesResponse> prefsAsync =
        ref.watch(aiPreferencesProvider);
    final AiPreferencesResponse prefs =
        prefsAsync.valueOrNull ?? AiPreferencesResponse.initialDefault();
    final bool isOnline = ref.watch(aiIsOnlineProvider);

    return Scaffold(
      backgroundColor: tokens.bgPrimary,
      appBar: AppBar(
        title: Row(
          children: <Widget>[
            Icon(
              Icons.auto_awesome_rounded,
              color: tokens.accentPrimary,
              size: 20,
            ),
            const SizedBox(width: 8),
            const Text(
              'Trợ lý AI',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
          ],
        ),
        actions: <Widget>[
          // Huy hiệu số miền được cấp quyền
          ActionChip(
            key: const Key('chip_ai_domain_status'),
            visualDensity: VisualDensity.compact,
            avatar: Icon(
              prefs.allowedDomainsCount > 0
                  ? Icons.lock_open_rounded
                  : Icons.lock_outline_rounded,
              size: 14,
              color: prefs.allowedDomainsCount > 0
                  ? tokens.accentPrimary
                  : tokens.warning,
            ),
            label: Text(
              '${prefs.allowedDomainsCount}/5 miền',
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w600,
                color: prefs.allowedDomainsCount > 0
                    ? tokens.accentPrimary
                    : tokens.warning,
              ),
            ),
            onPressed: _openConsentSheet,
          ),
          IconButton(
            key: const Key('btn_open_ai_conversations'),
            tooltip: 'Lịch sử hội thoại',
            icon: const Icon(Icons.history_rounded),
            onPressed: _openConversationsSheet,
          ),
          IconButton(
            key: const Key('btn_open_ai_usage'),
            tooltip: 'Hạn mức & Sử dụng',
            icon: const Icon(Icons.pie_chart_outline_rounded),
            onPressed: _openUsageSheet,
          ),
          IconButton(
            key: const Key('btn_open_ai_consent'),
            tooltip: 'Quyền riêng tư AI',
            icon: const Icon(Icons.shield_outlined),
            onPressed: _openConsentSheet,
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: <Widget>[
            // 1. Dòng cố định cảnh báo AI (AWP-1)
            const AiDisclaimerBanner(),

            // 2. Thông báo chế độ trò chuyện chung nếu chưa cấp quyền miền nào
            if (prefs.allowedDomainsCount == 0)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: LifeOsTokens.space3,
                  vertical: 6,
                ),
                color: tokens.warning.withValues(alpha: 0.12),
                child: Row(
                  children: <Widget>[
                    Icon(
                      Icons.shield_outlined,
                      size: 16,
                      color: tokens.warning,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Chế độ chat chung: AI chưa đọc bất kỳ dữ liệu cá nhân nào.',
                        style: TextStyle(
                          fontSize: 11.5,
                          fontWeight: FontWeight.w600,
                          color: tokens.textPrimary,
                        ),
                      ),
                    ),
                    TextButton(
                      style: TextButton.styleFrom(
                        visualDensity: VisualDensity.compact,
                        padding: const EdgeInsets.symmetric(horizontal: 6),
                      ),
                      onPressed: _openConsentSheet,
                      child: const Text('Cấp quyền',
                          style: TextStyle(fontSize: 11.5)),
                    ),
                  ],
                ),
              ),

            // 3. Danh sách tin nhắn
            Expanded(
              child: _isLoadingHistory
                  ? const Center(child: CircularProgressIndicator())
                  : _messages.isEmpty && !_isSending
                      ? _buildEmptyWelcomeState(tokens, prefs)
                      : ListView.builder(
                          controller: _scrollController,
                          padding: const EdgeInsets.symmetric(
                            horizontal: LifeOsTokens.space3,
                            vertical: LifeOsTokens.space3,
                          ),
                          itemCount: (_hasMoreMessages ? 1 : 0) +
                              _messages.length +
                              (_isSending ? 1 : 0) +
                              (_currentError != null ? 1 : 0),
                          itemBuilder: (BuildContext ctx, int idx) {
                            final int headerOffset = _hasMoreMessages ? 1 : 0;
                            if (_hasMoreMessages && idx == 0) {
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 8),
                                child: Center(
                                  child: TextButton.icon(
                                    key: const Key('btn_load_older_ai_messages'),
                                    onPressed: _isLoadingOlderMessages
                                        ? null
                                        : _loadOlderMessages,
                                    icon: _isLoadingOlderMessages
                                        ? const SizedBox(
                                            width: 14,
                                            height: 14,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                            ),
                                          )
                                        : const Icon(Icons.expand_less_rounded,
                                            size: 18),
                                    label: Text(
                                      _isLoadingOlderMessages
                                          ? 'Đang tải...'
                                          : 'Tải tin nhắn cũ hơn',
                                    ),
                                  ),
                                ),
                              );
                            }
                            final int msgIdx = idx - headerOffset;
                            if (msgIdx < _messages.length) {
                              return _buildMessageItem(
                                _messages[msgIdx],
                                tokens,
                              );
                            } else if (_isSending &&
                                msgIdx == _messages.length) {
                              return _buildWaitingBubble(tokens);
                            } else {
                              return _buildErrorBubble(tokens);
                            }
                          },
                        ),
            ),

            // 4. Thanh lối tắt gợi ý nhanh (AWP-4)
            if (prefs.taskCalendar && !_isSending && isOnline)
              Container(
                height: 38,
                padding: const EdgeInsets.symmetric(
                  horizontal: LifeOsTokens.space3,
                ),
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: <Widget>[
                    ActionChip(
                      key: const Key('chip_ai_daily_plan'),
                      visualDensity: VisualDensity.compact,
                      avatar: _loadingPlan
                          ? const SizedBox(
                              width: 12,
                              height: 12,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.today_rounded, size: 14),
                      label: const Text(
                        'Gợi ý Kế hoạch Hôm nay',
                        style: TextStyle(fontSize: 11.5),
                      ),
                      onPressed: _loadingPlan
                          ? null
                          : () => _handlePlanRequest(weekly: false),
                    ),
                    const SizedBox(width: 8),
                    ActionChip(
                      key: const Key('chip_ai_weekly_plan'),
                      visualDensity: VisualDensity.compact,
                      avatar: const Icon(Icons.date_range_rounded, size: 14),
                      label: const Text(
                        'Lập kế hoạch tuần',
                        style: TextStyle(fontSize: 11.5),
                      ),
                      onPressed: _loadingPlan
                          ? null
                          : () => _handlePlanRequest(weekly: true),
                    ),
                  ],
                ),
              ),

            // 5. Cảnh báo ngoại tuyến nếu mất mạng
            if (!isOnline)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(LifeOsTokens.space2),
                color: tokens.borderSubtle,
                child: Text(
                  'Cần kết nối mạng để trò chuyện với Trợ lý AI. Dữ liệu hội thoại không được lưu ngoại tuyến.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 12, color: tokens.textSecondary),
                ),
              ),

            // 6. Thanh nhập liệu (Có Concurrency Gate chặn gửi chồng khi đang chờ)
            Container(
              padding: const EdgeInsets.fromLTRB(
                LifeOsTokens.space3,
                LifeOsTokens.space2,
                LifeOsTokens.space3,
                LifeOsTokens.space2,
              ),
              decoration: BoxDecoration(
                color: tokens.bgSurface,
                border: Border(
                  top: BorderSide(color: tokens.borderSubtle),
                ),
              ),
              child: Row(
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      key: const Key('input_ai_chat_message'),
                      controller: _inputController,
                      enabled: !_isSending && isOnline,
                      minLines: 1,
                      maxLines: 4,
                      maxLength: 8000,
                      decoration: InputDecoration(
                        counterText: '',
                        hintText: !isOnline
                            ? 'Mất kết nối mạng...'
                            : _isSending
                                ? 'AI đang suy nghĩ...'
                                : 'Hỏi Trợ lý AI...',
                        isDense: true,
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: LifeOsTokens.space3,
                          vertical: 10,
                        ),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide(color: tokens.borderSubtle),
                        ),
                      ),
                      onSubmitted: (_) => _handleSendMessage(),
                    ),
                  ),
                  const SizedBox(width: LifeOsTokens.space2),
                  IconButton.filled(
                    key: const Key('btn_send_ai_message'),
                    onPressed: (!_isSending && isOnline)
                        ? () => _handleSendMessage()
                        : null,
                    icon: _isSending
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Icon(Icons.arrow_upward_rounded),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyWelcomeState(
    LifeOsTokens tokens,
    AiPreferencesResponse prefs,
  ) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(LifeOsTokens.space5),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: tokens.accentPrimary.withValues(alpha: 0.12),
              ),
              child: Icon(
                Icons.auto_awesome_rounded,
                size: 36,
                color: tokens.accentPrimary,
              ),
            ),
            const SizedBox(height: LifeOsTokens.space3),
            Text(
              'Xin chào! Tôi có thể giúp gì cho bạn?',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: tokens.textPrimary,
              ),
            ),
            const SizedBox(height: LifeOsTokens.space2),
            Text(
              'Bạn có thể hỏi về kế hoạch hôm nay, chia nhỏ công việc, '
              'hoặc phân tích thói quen dựa trên các miền dữ liệu đã cấp quyền.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                color: tokens.textSecondary,
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageItem(AiMessageResponse msg, LifeOsTokens tokens) {
    final bool isUser = msg.role == AiMessageRole.user;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment:
            isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          if (!isUser) ...<Widget>[
            CircleAvatar(
              radius: 14,
              backgroundColor: tokens.accentPrimary.withValues(alpha: 0.16),
              child: Icon(
                Icons.auto_awesome_rounded,
                size: 15,
                color: tokens.accentPrimary,
              ),
            ),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: Column(
              crossAxisAlignment:
                  isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
              children: <Widget>[
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: LifeOsTokens.space3,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    color: isUser
                        ? tokens.accentPrimary
                        : tokens.bgElevated,
                    borderRadius: BorderRadius.only(
                      topLeft: const Radius.circular(16),
                      topRight: const Radius.circular(16),
                      bottomLeft: Radius.circular(isUser ? 16 : 4),
                      bottomRight: Radius.circular(isUser ? 4 : 16),
                    ),
                  ),
                  child: Text(
                    msg.content,
                    style: TextStyle(
                      fontSize: 14,
                      height: 1.4,
                      color: isUser ? Colors.white : tokens.textPrimary,
                    ),
                  ),
                ),
                // Nút "AI đã đọc gì?" nếu tin nhắn của Assistant có ngữ cảnh
                if (!isUser && msg.contextAvailable) ...<Widget>[
                  const SizedBox(height: 4),
                  InkWell(
                    key: Key('btn_view_context_${msg.id}'),
                    onTap: () => _openContextDisclosure(msg.id),
                    borderRadius: BorderRadius.circular(12),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Icon(
                            Icons.visibility_outlined,
                            size: 13,
                            color: tokens.accentPrimary,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            'AI đã đọc gì?',
                            style: TextStyle(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w600,
                              color: tokens.accentPrimary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
          if (isUser) const SizedBox(width: 4),
        ],
      ),
    );
  }

  Widget _buildWaitingBubble(LifeOsTokens tokens) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          CircleAvatar(
            radius: 14,
            backgroundColor: tokens.accentPrimary.withValues(alpha: 0.16),
            child: Icon(
              Icons.auto_awesome_rounded,
              size: 15,
              color: tokens.accentPrimary,
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: LifeOsTokens.space3,
              vertical: 10,
            ),
            decoration: BoxDecoration(
              color: tokens.bgElevated,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(16),
                topRight: Radius.circular(16),
                bottomLeft: Radius.circular(4),
                bottomRight: Radius.circular(16),
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                const SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
                const SizedBox(width: 8),
                Text(
                  'Đang xử lý ($_waitingSeconds / 30s)...',
                  style: TextStyle(
                    fontSize: 13,
                    fontStyle: FontStyle.italic,
                    color: tokens.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorBubble(LifeOsTokens tokens) {
    if (_currentError == null) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: LifeOsCard(
        elevation: 1,
        padding: const EdgeInsets.all(LifeOsTokens.space3),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Icon(Icons.error_outline_rounded,
                    color: tokens.danger, size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _currentError!.message,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: tokens.danger,
                    ),
                  ),
                ),
              ],
            ),
            if (_currentError!.actionType != AiErrorActionType.none) ...<Widget>[
              const SizedBox(height: LifeOsTokens.space2),
              Align(
                alignment: Alignment.centerRight,
                child: FilledButton.tonal(
                  key: const Key('btn_ai_error_action'),
                  style: FilledButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                  ),
                  onPressed: () =>
                      _handleErrorAction(_currentError!.actionType),
                  child: Text(_currentError!.suggestedAction),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

