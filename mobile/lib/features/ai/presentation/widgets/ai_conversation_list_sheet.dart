import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../data/ai_api.dart';
import '../../data/ai_models.dart';
import '../../data/ai_providers.dart';

/// Sheet hiển thị danh sách các cuộc hội thoại AI đã lưu trên server (AWP-2).
class AiConversationListSheet extends ConsumerStatefulWidget {
  const AiConversationListSheet({
    super.key,
    required this.onSelectConversation,
    required this.onNewConversation,
  });

  final ValueChanged<String> onSelectConversation;
  final VoidCallback onNewConversation;

  @override
  ConsumerState<AiConversationListSheet> createState() =>
      _AiConversationListSheetState();
}

class _AiConversationListSheetState
    extends ConsumerState<AiConversationListSheet> {
  List<AiConversationResponse> _items = <AiConversationResponse>[];
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadConversations();
  }

  Future<void> _loadConversations() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    final AiApiClient api = ref.read(aiApiClientProvider);
    try {
      final AiConversationPage page = await api.listConversations();
      if (mounted) {
        setState(() {
          _items = page.items;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _handleDelete(AiConversationResponse conv) async {
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Xóa cuộc hội thoại?'),
        content: Text(
          'Bạn có chắc muốn xóa hội thoại "${conv.title ?? 'Không có tiêu đề'}"? '
          'Toàn bộ lịch sử tin nhắn của hội thoại này trên máy chủ sẽ bị xóa.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: LifeOsTokens.of(context).danger,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      try {
        final AiApiClient api = ref.read(aiApiClientProvider);
        await api.deleteConversation(conv.id);
        setState(() {
          _items.removeWhere((AiConversationResponse c) => c.id == conv.id);
        });
        if (ref.read(aiActiveConversationIdProvider) == conv.id) {
          ref.read(aiActiveConversationIdProvider.notifier).state = null;
        }
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Đã xóa cuộc hội thoại.')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Lỗi khi xóa hội thoại: $e')),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final String? activeId = ref.watch(aiActiveConversationIdProvider);

    return Container(
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusLg),
        ),
      ),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.85,
      ),
      child: Column(
        children: <Widget>[
          // Header
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
                  Icons.history_rounded,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    'Lịch sử hội thoại AI',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          // Nút tạo mới
          Padding(
            padding: const EdgeInsets.all(LifeOsTokens.space3),
            child: FilledButton.icon(
              key: const Key('btn_new_ai_conversation'),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
              ),
              onPressed: () {
                Navigator.of(context).pop();
                widget.onNewConversation();
              },
              icon: const Icon(Icons.add_comment_rounded),
              label: const Text('Bắt đầu cuộc trò chuyện mới'),
            ),
          ),

          // Body list
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _errorMessage != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(LifeOsTokens.space4),
                          child: LifeOsErrorBanner(message: _errorMessage!),
                        ),
                      )
                    : _items.isEmpty
                        ? Center(
                            child: Padding(
                              padding: const EdgeInsets.all(LifeOsTokens.space4),
                              child: Text(
                                'Chưa có cuộc trò chuyện nào.\nNhấn nút phía trên để bắt đầu trò chuyện với AI.',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  fontSize: 13.5,
                                  color: tokens.textSecondary,
                                ),
                              ),
                            ),
                          )
                        : ListView.separated(
                            itemCount: _items.length,
                            separatorBuilder: (_, __) =>
                                const Divider(height: 1),
                            itemBuilder: (BuildContext ctx, int idx) {
                              final AiConversationResponse conv = _items[idx];
                              final bool isActive = conv.id == activeId;

                              return ListTile(
                                leading: Icon(
                                  Icons.chat_bubble_outline_rounded,
                                  color: isActive
                                      ? tokens.accentPrimary
                                      : tokens.textSecondary,
                                ),
                                title: Text(
                                  conv.title ?? 'Cuộc trò chuyện',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    fontWeight: isActive
                                        ? FontWeight.w700
                                        : FontWeight.w500,
                                    color: isActive
                                        ? tokens.accentPrimary
                                        : tokens.textPrimary,
                                  ),
                                ),
                                subtitle: Text(
                                  conv.updatedAt.length >= 10
                                      ? conv.updatedAt.substring(0, 10)
                                      : conv.updatedAt,
                                  style: TextStyle(
                                    fontSize: 11.5,
                                    color: tokens.textSecondary,
                                  ),
                                ),
                                trailing: IconButton(
                                  icon: Icon(
                                    Icons.delete_outline_rounded,
                                    size: 20,
                                    color: tokens.textSecondary,
                                  ),
                                  onPressed: () => _handleDelete(conv),
                                ),
                                onTap: () {
                                  Navigator.of(context).pop();
                                  widget.onSelectConversation(conv.id);
                                },
                              );
                            },
                          ),
          ),
        ],
      ),
    );
  }
}

