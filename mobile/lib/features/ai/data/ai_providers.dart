import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import 'ai_api.dart';
import 'ai_models.dart';

/// Provider khởi tạo `AiApiClient` dựa trên `LifeOsApiClient` hiện có của ứng dụng.
final Provider<AiApiClient> aiApiClientProvider = Provider<AiApiClient>((Ref ref) {
  final LifeOsApiClient client = ref.watch(apiClientProvider);
  return AiApiClient(client);
});

/// Lưu trữ trạng thái cấu hình consent trong bộ nhớ phiên (không lưu vào SQLite/outbox).
class AiPreferencesNotifier extends StateNotifier<AsyncValue<AiPreferencesResponse>> {
  AiPreferencesNotifier(this._api) : super(AsyncValue<AiPreferencesResponse>.data(AiPreferencesResponse.initialDefault()));

  final AiApiClient _api;

  Future<void> loadPreferences() async {
    state = const AsyncValue<AiPreferencesResponse>.loading();
    try {
      final AiPreferencesResponse prefs = await _api.getPreferences();
      state = AsyncValue<AiPreferencesResponse>.data(prefs);
    } catch (e, st) {
      state = AsyncValue<AiPreferencesResponse>.error(e, st);
    }
  }

  Future<AiPreferencesResponse> updatePreferences(AiPreferencesRequest request) async {
    final AiPreferencesResponse updated = await _api.replacePreferences(request);
    state = AsyncValue<AiPreferencesResponse>.data(updated);
    return updated;
  }

  void resetToDefault() {
    state = AsyncValue<AiPreferencesResponse>.data(AiPreferencesResponse.initialDefault());
  }
}

final StateNotifierProvider<AiPreferencesNotifier, AsyncValue<AiPreferencesResponse>>
    aiPreferencesProvider =
    StateNotifierProvider<AiPreferencesNotifier, AsyncValue<AiPreferencesResponse>>(
  (Ref ref) => AiPreferencesNotifier(ref.watch(aiApiClientProvider)),
);

/// ID cuộc hội thoại hiện đang mở trên màn hình chat (bộ nhớ phiên).
final StateProvider<String?> aiActiveConversationIdProvider =
    StateProvider<String?>((Ref ref) => null);

/// Trạng thái mạng trực tuyến cho tính năng AI (true nếu có mạng).
final StateProvider<bool> aiIsOnlineProvider =
    StateProvider<bool>((Ref ref) => true);

