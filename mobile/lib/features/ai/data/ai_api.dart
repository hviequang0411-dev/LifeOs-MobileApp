import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import 'ai_models.dart';

/// Client giao tiếp trực tiếp với các endpoint AI của Backend qua `LifeOsApiClient`.
///
/// Tính năng ONLINE-ONLY:
/// - Không đi qua `outbox`.
/// - Không lưu hội thoại hay tin nhắn vào CSDL SQLite nội bộ (chỉ cache phiên trong bộ nhớ).
class AiApiClient {
  AiApiClient(this._apiClient);

  final LifeOsApiClient _apiClient;
  Dio get _dio => _apiClient.dio;

  static const Duration _aiCallTimeout = Duration(seconds: 35);

  /// Đọc cấu hình quyền riêng tư và consent hiện tại (`GET /api/v1/ai/preferences`).
  Future<AiPreferencesResponse> getPreferences() async {
    try {
      final Response<dynamic> resp = await _dio.get<dynamic>(
        '/api/v1/ai/preferences',
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiPreferencesResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Cập nhật toàn bộ công tắc consent (`PUT /api/v1/ai/preferences`).
  Future<AiPreferencesResponse> replacePreferences(
    AiPreferencesRequest request,
  ) async {
    try {
      final Response<dynamic> resp = await _dio.put<dynamic>(
        '/api/v1/ai/preferences',
        data: request.toJson(),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiPreferencesResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Danh sách các cuộc hội thoại AI (`GET /api/v1/ai/conversations`).
  Future<AiConversationPage> listConversations({
    int limit = 50,
    String? cursor,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.get<dynamic>(
        '/api/v1/ai/conversations',
        queryParameters: <String, dynamic>{
          'limit': limit,
          if (cursor != null && cursor.isNotEmpty) 'cursor': cursor,
        },
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiConversationPage.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Tạo cuộc hội thoại AI mới (`POST /api/v1/ai/conversations`).
  Future<AiConversationResponse> createConversation({String? title}) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/conversations',
        data: <String, dynamic>{'title': title},
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiConversationResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Lấy chi tiết cuộc hội thoại kèm tin nhắn (`GET /api/v1/ai/conversations/{id}`).
  Future<AiConversationDetailResponse> getConversation(
    String id, {
    int limit = 50,
    String? cursor,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.get<dynamic>(
        '/api/v1/ai/conversations/$id',
        queryParameters: <String, dynamic>{
          'limit': limit,
          if (cursor != null && cursor.isNotEmpty) 'cursor': cursor,
        },
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiConversationDetailResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Xóa cuộc hội thoại (`DELETE /api/v1/ai/conversations/{id}`).
  Future<void> deleteConversation(String id) async {
    try {
      await _dio.delete<dynamic>('/api/v1/ai/conversations/$id');
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Gửi tin nhắn chat và nhận phản hồi Non-Streaming (`POST /api/v1/ai/conversations/{id}/messages`).
  Future<AiChatResponse> sendMessage(
    String conversationId,
    AiChatRequest request,
  ) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/conversations/$conversationId/messages',
        data: request.toJson(),
        options: Options(
          receiveTimeout: _aiCallTimeout,
          sendTimeout: const Duration(seconds: 30),
        ),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiChatResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Minh bạch ngữ cảnh: "AI đã đọc gì?" (`GET /api/v1/ai/messages/{id}/context`).
  Future<AiContextResponse> getMessageContext(String messageId) async {
    try {
      final Response<dynamic> resp = await _dio.get<dynamic>(
        '/api/v1/ai/messages/$messageId/context',
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiContextResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Xóa toàn bộ dữ liệu AI trên server và reset consent về tắt (`DELETE /api/v1/ai/data`).
  Future<void> deleteAllData() async {
    try {
      await _dio.delete<dynamic>('/api/v1/ai/data');
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Lập kế hoạch ngày chỉ đọc (`POST /api/v1/ai/plans/daily`).
  Future<AiPlanResponse> createDailyPlan(String periodStart, List<String> domains) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/plans/daily',
        data: <String, dynamic>{
          'periodStart': periodStart,
          'contextDomains': domains,
        },
        options: Options(receiveTimeout: _aiCallTimeout),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiPlanResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Lập kế hoạch tuần chỉ đọc (`POST /api/v1/ai/plans/weekly`).
  Future<AiPlanResponse> createWeeklyPlan(String periodStart, List<String> domains) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/plans/weekly',
        data: <String, dynamic>{
          'periodStart': periodStart,
          'contextDomains': domains,
        },
        options: Options(receiveTimeout: _aiCallTimeout),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiPlanResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Chia nhỏ công việc chỉ đọc (`POST /api/v1/ai/tasks/{id}/breakdown`).
  Future<AiTaskBreakdownResponse> breakDownTask(
    String taskId, {
    String? instruction,
    required List<String> contextDomains,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/tasks/$taskId/breakdown',
        data: <String, dynamic>{
          'instruction': instruction,
          'contextDomains': contextDomains,
        },
        options: Options(receiveTimeout: _aiCallTimeout),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiTaskBreakdownResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Insight Thói quen chỉ đọc (`POST /api/v1/ai/insights/habits`).
  Future<AiInsightResponse> createHabitInsight({
    required String from,
    required String to,
    required List<String> contextDomains,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/insights/habits',
        data: <String, dynamic>{
          'from': from,
          'to': to,
          'contextDomains': contextDomains,
        },
        options: Options(receiveTimeout: _aiCallTimeout),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiInsightResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Insight Tài chính chỉ đọc (`POST /api/v1/ai/insights/finance`).
  Future<AiInsightResponse> createFinanceInsight({
    required String from,
    required String to,
    required String currency,
    required List<String> contextDomains,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/insights/finance',
        data: <String, dynamic>{
          'from': from,
          'to': to,
          'currency': currency,
          'contextDomains': contextDomains,
        },
        options: Options(receiveTimeout: _aiCallTimeout),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiInsightResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Tóm tắt ghi chú chỉ đọc (`POST /api/v1/ai/notes/{id}/summary`).
  Future<AiNoteSummaryResponse> summarizeNote(
    String noteId, {
    String style = 'BULLETS',
    required List<String> contextDomains,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.post<dynamic>(
        '/api/v1/ai/notes/$noteId/summary',
        data: <String, dynamic>{
          'style': style,
          'contextDomains': contextDomains,
        },
        options: Options(receiveTimeout: _aiCallTimeout),
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiNoteSummaryResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Lấy dữ liệu sử dụng theo ngày (`GET /api/v1/ai/usage`).
  Future<AiUsageResponse> getUsage({
    required String from,
    required String to,
  }) async {
    try {
      final Response<dynamic> resp = await _dio.get<dynamic>(
        '/api/v1/ai/usage',
        queryParameters: <String, dynamic>{'from': from, 'to': to},
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiUsageResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  /// Lấy thông tin hạn mức, quota và trạng thái dịch vụ (`GET /api/v1/ai/limits`).
  Future<AiLimitsResponse> getLimits() async {
    try {
      final Response<dynamic> resp = await _dio.get<dynamic>(
        '/api/v1/ai/limits',
      );
      final Map<String, dynamic> data =
          _asMap(resp.data) ?? <String, dynamic>{};
      return AiLimitsResponse.fromJson(data);
    } catch (e) {
      throw LifeOsApiClient.normalizeError(e);
    }
  }

  static Map<String, dynamic>? _asMap(dynamic raw) {
    if (raw is Map<String, dynamic>) return raw;
    if (raw is Map<dynamic, dynamic>) return raw.cast<String, dynamic>();
    return null;
  }
}

