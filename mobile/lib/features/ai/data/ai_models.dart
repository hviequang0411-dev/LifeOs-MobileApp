import 'package:flutter/foundation.dart';

/// Các miền dữ liệu người dùng có thể cấp quyền truy cập cho AI (AWP-1).
enum AiContextDomain {
  taskCalendar('TASK_CALENDAR', 'Công việc & Lịch', 'Đọc công việc, dự án và lịch trình để sắp xếp thời gian.'),
  habit('HABIT', 'Thói quen', 'Đọc lịch sử thực hiện thói quen và chuỗi ngày duy trì.'),
  goal('GOAL', 'Mục tiêu', 'Đọc danh sách mục tiêu dài hạn và các cột mốc đã lên kế hoạch.'),
  notes('NOTES', 'Ghi chú', 'Đọc nội dung ghi chú được chọn để phân tích và tóm tắt ý chính.'),
  finance('FINANCE', 'Tài chính cá nhân', 'Đọc tổng hợp số liệu danh mục và ngân sách (chỉ đọc, không giao dịch).');

  const AiContextDomain(this.code, this.labelVi, this.descriptionVi);

  final String code;
  final String labelVi;
  final String descriptionVi;

  static AiContextDomain fromCode(String code) {
    for (final AiContextDomain domain in values) {
      if (domain.code == code) return domain;
    }
    return AiContextDomain.taskCalendar;
  }
}

/// Chế độ sinh nội dung của AI theo OpenAPI.
enum AiGenerationMode {
  chat('CHAT'),
  dailyPlan('DAILY_PLAN'),
  weeklyPlan('WEEKLY_PLAN'),
  taskBreakdown('TASK_BREAKDOWN'),
  habitInsight('HABIT_INSIGHT'),
  financeInsight('FINANCE_INSIGHT'),
  noteSummary('NOTE_SUMMARY');

  const AiGenerationMode(this.code);
  final String code;

  static AiGenerationMode fromCode(String code) {
    for (final AiGenerationMode mode in values) {
      if (mode.code == code) return mode;
    }
    return AiGenerationMode.chat;
  }
}

/// Vai trò tin nhắn trong hội thoại AI.
enum AiMessageRole {
  user('USER'),
  assistant('ASSISTANT');

  const AiMessageRole(this.code);
  final String code;

  static AiMessageRole fromCode(String code) {
    return code.toUpperCase() == 'ASSISTANT'
        ? AiMessageRole.assistant
        : AiMessageRole.user;
  }
}

/// Các cờ tính năng AI được bật từ máy chủ.
@immutable
class AiFeatureFlags {
  const AiFeatureFlags({
    required this.chat,
    required this.insights,
    required this.notes,
    required this.finance,
    required this.rag,
    required this.actions,
  });

  factory AiFeatureFlags.fromJson(Map<String, dynamic> json) {
    return AiFeatureFlags(
      chat: json['chat'] as bool? ?? false,
      insights: json['insights'] as bool? ?? false,
      notes: json['notes'] as bool? ?? false,
      finance: json['finance'] as bool? ?? false,
      rag: json['rag'] as bool? ?? false,
      actions: json['actions'] as bool? ?? false,
    );
  }

  final bool chat;
  final bool insights;
  final bool notes;
  final bool finance;
  final bool rag;
  final bool actions;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'chat': chat,
        'insights': insights,
        'notes': notes,
        'finance': finance,
        'rag': rag,
        'actions': actions,
      };
}

/// Yêu cầu cập nhật quyền consent của người dùng (`PUT /api/v1/ai/preferences`).
@immutable
class AiPreferencesRequest {
  const AiPreferencesRequest({
    required this.taskCalendar,
    required this.habit,
    required this.goal,
    required this.notes,
    required this.finance,
    required this.providerTermsAccepted,
  });

  final bool taskCalendar;
  final bool habit;
  final bool goal;
  final bool notes;
  final bool finance;
  final bool providerTermsAccepted;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'taskCalendar': taskCalendar,
        'habit': habit,
        'goal': goal,
        'notes': notes,
        'finance': finance,
        'providerTermsAccepted': providerTermsAccepted,
      };
}

/// Phản hồi cấu hình consent từ máy chủ (`GET/PUT /api/v1/ai/preferences`).
@immutable
class AiPreferencesResponse {
  const AiPreferencesResponse({
    required this.taskCalendar,
    required this.habit,
    required this.goal,
    required this.notes,
    required this.finance,
    required this.policyVersion,
    required this.providerNoticeVersion,
    this.providerTermsAcceptedAt,
    this.updatedAt,
    required this.features,
  });

  factory AiPreferencesResponse.fromJson(Map<String, dynamic> json) {
    return AiPreferencesResponse(
      taskCalendar: json['taskCalendar'] as bool? ?? false,
      habit: json['habit'] as bool? ?? false,
      goal: json['goal'] as bool? ?? false,
      notes: json['notes'] as bool? ?? false,
      finance: json['finance'] as bool? ?? false,
      policyVersion: (json['policyVersion'] as String?) ?? '2026-11-10',
      providerNoticeVersion:
          (json['providerNoticeVersion'] as String?) ?? 'openai-api-2026-11-10',
      providerTermsAcceptedAt: json['providerTermsAcceptedAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      features: json['features'] is Map<String, dynamic>
          ? AiFeatureFlags.fromJson(json['features'] as Map<String, dynamic>)
          : const AiFeatureFlags(
              chat: true,
              insights: false,
              notes: false,
              finance: false,
              rag: false,
              actions: false,
            ),
    );
  }

  /// Mặc định ban đầu khi chưa cấp quyền: TẤT CẢ TẮT (D264).
  factory AiPreferencesResponse.initialDefault() {
    return const AiPreferencesResponse(
      taskCalendar: false,
      habit: false,
      goal: false,
      notes: false,
      finance: false,
      policyVersion: '2026-11-10',
      providerNoticeVersion: 'openai-api-2026-11-10',
      providerTermsAcceptedAt: null,
      updatedAt: null,
      features: AiFeatureFlags(
        chat: true,
        insights: true,
        notes: false,
        finance: false,
        rag: false,
        actions: false,
      ),
    );
  }

  final bool taskCalendar;
  final bool habit;
  final bool goal;
  final bool notes;
  final bool finance;
  final String policyVersion;
  final String providerNoticeVersion;
  final String? providerTermsAcceptedAt;
  final String? updatedAt;
  final AiFeatureFlags features;

  bool isDomainAllowed(AiContextDomain domain) {
    switch (domain) {
      case AiContextDomain.taskCalendar:
        return taskCalendar;
      case AiContextDomain.habit:
        return habit;
      case AiContextDomain.goal:
        return goal;
      case AiContextDomain.notes:
        return notes;
      case AiContextDomain.finance:
        return finance;
    }
  }

  int get allowedDomainsCount {
    int count = 0;
    if (taskCalendar) count++;
    if (habit) count++;
    if (goal) count++;
    if (notes) count++;
    if (finance) count++;
    return count;
  }

  List<String> get allowedDomainCodes {
    final List<String> list = <String>[];
    if (taskCalendar) list.add(AiContextDomain.taskCalendar.code);
    if (habit) list.add(AiContextDomain.habit.code);
    if (goal) list.add(AiContextDomain.goal.code);
    if (notes) list.add(AiContextDomain.notes.code);
    if (finance) list.add(AiContextDomain.finance.code);
    return list;
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'taskCalendar': taskCalendar,
        'habit': habit,
        'goal': goal,
        'notes': notes,
        'finance': finance,
        'policyVersion': policyVersion,
        'providerNoticeVersion': providerNoticeVersion,
        'providerTermsAcceptedAt': providerTermsAcceptedAt,
        'updatedAt': updatedAt,
        'features': features.toJson(),
      };
}

/// Thông tin một cuộc hội thoại AI (`GET /api/v1/ai/conversations`).
@immutable
class AiConversationResponse {
  const AiConversationResponse({
    required this.id,
    this.title,
    required this.createdAt,
    required this.updatedAt,
    required this.expiresAt,
  });

  factory AiConversationResponse.fromJson(Map<String, dynamic> json) {
    return AiConversationResponse(
      id: json['id'] as String,
      title: json['title'] as String?,
      createdAt: json['createdAt'] as String,
      updatedAt: json['updatedAt'] as String,
      expiresAt: json['expiresAt'] as String,
    );
  }

  final String id;
  final String? title;
  final String createdAt;
  final String updatedAt;
  final String expiresAt;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'title': title,
        'createdAt': createdAt,
        'updatedAt': updatedAt,
        'expiresAt': expiresAt,
      };
}

/// Danh sách phân trang các cuộc hội thoại.
@immutable
class AiConversationPage {
  const AiConversationPage({
    required this.items,
    this.nextCursor,
    required this.hasMore,
  });

  factory AiConversationPage.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawList = json['items'] as List<dynamic>?;
    return AiConversationPage(
      items: rawList != null
          ? rawList
              .map((dynamic e) =>
                  AiConversationResponse.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiConversationResponse>[],
      nextCursor: json['nextCursor'] as String?,
      hasMore: json['hasMore'] as bool? ?? false,
    );
  }

  final List<AiConversationResponse> items;
  final String? nextCursor;
  final bool hasMore;
}

/// Một tin nhắn trong cuộc hội thoại AI.
@immutable
class AiMessageResponse {
  const AiMessageResponse({
    required this.id,
    required this.role,
    required this.content,
    required this.mode,
    required this.contextAvailable,
    required this.createdAt,
  });

  factory AiMessageResponse.fromJson(Map<String, dynamic> json) {
    return AiMessageResponse(
      id: json['id'] as String,
      role: AiMessageRole.fromCode(json['role'] as String? ?? 'USER'),
      content: json['content'] as String? ?? '',
      mode: AiGenerationMode.fromCode(json['mode'] as String? ?? 'CHAT'),
      contextAvailable: json['contextAvailable'] as bool? ?? false,
      createdAt: json['createdAt'] as String? ?? '',
    );
  }

  final String id;
  final AiMessageRole role;
  final String content;
  final AiGenerationMode mode;
  final bool contextAvailable;
  final String createdAt;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'role': role.code,
        'content': content,
        'mode': mode.code,
        'contextAvailable': contextAvailable,
        'createdAt': createdAt,
      };
}

/// Trang tin nhắn trong một cuộc hội thoại.
@immutable
class AiMessagePage {
  const AiMessagePage({
    required this.items,
    this.nextCursor,
    required this.hasMore,
  });

  factory AiMessagePage.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawList = json['items'] as List<dynamic>?;
    return AiMessagePage(
      items: rawList != null
          ? rawList
              .map((dynamic e) =>
                  AiMessageResponse.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiMessageResponse>[],
      nextCursor: json['nextCursor'] as String?,
      hasMore: json['hasMore'] as bool? ?? false,
    );
  }

  final List<AiMessageResponse> items;
  final String? nextCursor;
  final bool hasMore;
}

/// Chi tiết cuộc hội thoại kèm lịch sử tin nhắn.
@immutable
class AiConversationDetailResponse {
  const AiConversationDetailResponse({
    required this.conversation,
    required this.messages,
  });

  factory AiConversationDetailResponse.fromJson(Map<String, dynamic> json) {
    return AiConversationDetailResponse(
      conversation: AiConversationResponse.fromJson(
        json['conversation'] as Map<String, dynamic>,
      ),
      messages: AiMessagePage.fromJson(
        json['messages'] as Map<String, dynamic>,
      ),
    );
  }

  final AiConversationResponse conversation;
  final AiMessagePage messages;
}

/// Yêu cầu gửi tin nhắn tới AI (`POST /api/v1/ai/conversations/{id}/messages`).
@immutable
class AiChatRequest {
  const AiChatRequest({
    required this.message,
    this.mode = AiGenerationMode.chat,
    required this.contextDomains,
  });

  factory AiChatRequest.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? domains = json['contextDomains'] as List<dynamic>?;
    return AiChatRequest(
      message: json['message'] as String? ?? '',
      mode: AiGenerationMode.fromCode(json['mode'] as String? ?? 'CHAT'),
      contextDomains: domains != null
          ? domains.map((dynamic e) => e.toString()).toList()
          : const <String>[],
    );
  }

  final String message;
  final AiGenerationMode mode;
  final List<String> contextDomains;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'message': message,
        'mode': mode.code,
        'contextDomains': contextDomains,
      };
}

/// Phản hồi gửi tin nhắn chat AI (`ai-chat.response.json`).
@immutable
class AiChatResponse {
  const AiChatResponse({
    required this.conversationId,
    required this.userMessage,
    required this.assistantMessage,
    this.actionProposals = const <dynamic>[],
  });

  factory AiChatResponse.fromJson(Map<String, dynamic> json) {
    return AiChatResponse(
      conversationId: json['conversationId'] as String,
      userMessage: AiMessageResponse.fromJson(
        json['userMessage'] as Map<String, dynamic>,
      ),
      assistantMessage: AiMessageResponse.fromJson(
        json['assistantMessage'] as Map<String, dynamic>,
      ),
      actionProposals: json['actionProposals'] as List<dynamic>? ?? const <dynamic>[],
    );
  }

  final String conversationId;
  final AiMessageResponse userMessage;
  final AiMessageResponse assistantMessage;
  final List<dynamic> actionProposals;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'conversationId': conversationId,
        'userMessage': userMessage.toJson(),
        'assistantMessage': assistantMessage.toJson(),
        'actionProposals': actionProposals,
      };
}

/// Thực thể dữ liệu trong ngữ cảnh minh bạch AI (AWP-3).
@immutable
class AiContextEntity {
  const AiContextEntity({
    required this.domain,
    required this.entityType,
    required this.entityId,
    this.entityVersion,
    required this.fields,
  });

  factory AiContextEntity.fromJson(Map<String, dynamic> json) {
    return AiContextEntity(
      domain: json['domain'] as String? ?? '',
      entityType: json['entityType'] as String? ?? '',
      entityId: json['entityId'] as String? ?? '',
      entityVersion: json['entityVersion'] as int?,
      fields: (json['fields'] as Map<String, dynamic>?) ?? <String, dynamic>{},
    );
  }

  final String domain;
  final String entityType;
  final String entityId;
  final int? entityVersion;
  final Map<String, dynamic> fields;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'domain': domain,
        'entityType': entityType,
        'entityId': entityId,
        'entityVersion': entityVersion,
        'fields': fields,
      };
}

/// Phản hồi xem ngữ cảnh chính xác "AI đã đọc gì?" (`GET /api/v1/ai/messages/{id}/context`).
@immutable
class AiContextResponse {
  const AiContextResponse({
    required this.messageId,
    required this.capturedAt,
    required this.domains,
    required this.entities,
    required this.truncated,
  });

  factory AiContextResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawDomains = json['domains'] as List<dynamic>?;
    final List<dynamic>? rawEntities = json['entities'] as List<dynamic>?;
    return AiContextResponse(
      messageId: json['messageId'] as String? ?? '',
      capturedAt: json['capturedAt'] as String? ?? '',
      domains: rawDomains != null
          ? rawDomains.map((dynamic e) => e.toString()).toList()
          : const <String>[],
      entities: rawEntities != null
          ? rawEntities
              .map((dynamic e) =>
                  AiContextEntity.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiContextEntity>[],
      truncated: json['truncated'] as bool? ?? false,
    );
  }

  final String messageId;
  final String capturedAt;
  final List<String> domains;
  final List<AiContextEntity> entities;
  final bool truncated;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'messageId': messageId,
        'capturedAt': capturedAt,
        'domains': domains,
        'entities': entities.map((e) => e.toJson()).toList(),
        'truncated': truncated,
      };
}

/// Nguồn tham chiếu dữ liệu của kế hoạch / insight.
@immutable
class AiSourceReference {
  const AiSourceReference({
    required this.domain,
    required this.entityType,
    required this.entityId,
  });

  factory AiSourceReference.fromJson(Map<String, dynamic> json) {
    return AiSourceReference(
      domain: json['domain'] as String? ?? '',
      entityType: json['entityType'] as String? ?? '',
      entityId: json['entityId'] as String? ?? '',
    );
  }

  final String domain;
  final String entityType;
  final String entityId;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'domain': domain,
        'entityType': entityType,
        'entityId': entityId,
      };
}

/// Một mục gợi ý trong kế hoạch ngày/tuần.
@immutable
class AiPlanItem {
  const AiPlanItem({
    required this.title,
    required this.reason,
    required this.priority,
    this.suggestedStartAt,
    this.suggestedEndAt,
    this.sourceRefs = const <AiSourceReference>[],
  });

  factory AiPlanItem.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawRefs = json['sourceRefs'] as List<dynamic>?;
    return AiPlanItem(
      title: json['title'] as String? ?? '',
      reason: json['reason'] as String? ?? '',
      priority: json['priority'] as String? ?? 'MEDIUM',
      suggestedStartAt: json['suggestedStartAt'] as String?,
      suggestedEndAt: json['suggestedEndAt'] as String?,
      sourceRefs: rawRefs != null
          ? rawRefs
              .map((dynamic e) =>
                  AiSourceReference.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiSourceReference>[],
    );
  }

  final String title;
  final String reason;
  final String priority;
  final String? suggestedStartAt;
  final String? suggestedEndAt;
  final List<AiSourceReference> sourceRefs;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'title': title,
        'reason': reason,
        'priority': priority,
        'suggestedStartAt': suggestedStartAt,
        'suggestedEndAt': suggestedEndAt,
        'sourceRefs': sourceRefs.map((e) => e.toJson()).toList(),
      };
}

/// Kế hoạch ngày hoặc tuần được AI đề xuất (`POST /api/v1/ai/plans/daily|weekly`).
@immutable
class AiPlanResponse {
  const AiPlanResponse({
    required this.periodType,
    required this.periodStart,
    required this.summary,
    required this.items,
    required this.contextMessageId,
  });

  factory AiPlanResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawItems = json['items'] as List<dynamic>?;
    return AiPlanResponse(
      periodType: json['periodType'] as String? ?? 'DAY',
      periodStart: json['periodStart'] as String? ?? '',
      summary: json['summary'] as String? ?? '',
      items: rawItems != null
          ? rawItems
              .map((dynamic e) => AiPlanItem.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiPlanItem>[],
      contextMessageId: json['contextMessageId'] as String? ?? '',
    );
  }

  final String periodType;
  final String periodStart;
  final String summary;
  final List<AiPlanItem> items;
  final String contextMessageId;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'periodType': periodType,
        'periodStart': periodStart,
        'summary': summary,
        'items': items.map((e) => e.toJson()).toList(),
        'contextMessageId': contextMessageId,
      };
}

/// Dự thảo công việc độc lập từ chia nhỏ Task (`POST /api/v1/ai/tasks/{id}/breakdown`).
@immutable
class AiTaskDraft {
  const AiTaskDraft({
    required this.title,
    this.description,
    required this.priority,
    this.suggestedDueDate,
    required this.reason,
  });

  factory AiTaskDraft.fromJson(Map<String, dynamic> json) {
    return AiTaskDraft(
      title: json['title'] as String? ?? '',
      description: json['description'] as String?,
      priority: json['priority'] as String? ?? 'MEDIUM',
      suggestedDueDate: json['suggestedDueDate'] as String?,
      reason: json['reason'] as String? ?? '',
    );
  }

  final String title;
  final String? description;
  final String priority;
  final String? suggestedDueDate;
  final String reason;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'title': title,
        'description': description,
        'priority': priority,
        'suggestedDueDate': suggestedDueDate,
        'reason': reason,
      };
}

/// Phản hồi chia nhỏ công việc.
@immutable
class AiTaskBreakdownResponse {
  const AiTaskBreakdownResponse({
    required this.sourceTaskId,
    required this.items,
    required this.contextMessageId,
  });

  factory AiTaskBreakdownResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawItems = json['items'] as List<dynamic>?;
    return AiTaskBreakdownResponse(
      sourceTaskId: json['sourceTaskId'] as String? ?? '',
      items: rawItems != null
          ? rawItems
              .map((dynamic e) => AiTaskDraft.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiTaskDraft>[],
      contextMessageId: json['contextMessageId'] as String? ?? '',
    );
  }

  final String sourceTaskId;
  final List<AiTaskDraft> items;
  final String contextMessageId;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'sourceTaskId': sourceTaskId,
        'items': items.map((e) => e.toJson()).toList(),
        'contextMessageId': contextMessageId,
      };
}

/// Quan sát / phân tích trong insight Thói quen & Tài chính.
@immutable
class AiObservation {
  const AiObservation({
    required this.title,
    required this.detail,
    required this.trend,
    this.sourceRefs = const <AiSourceReference>[],
  });

  factory AiObservation.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawRefs = json['sourceRefs'] as List<dynamic>?;
    return AiObservation(
      title: json['title'] as String? ?? '',
      detail: json['detail'] as String? ?? '',
      trend: json['trend'] as String? ?? 'STABLE',
      sourceRefs: rawRefs != null
          ? rawRefs
              .map((dynamic e) =>
                  AiSourceReference.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiSourceReference>[],
    );
  }

  final String title;
  final String detail;
  final String trend;
  final List<AiSourceReference> sourceRefs;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'title': title,
        'detail': detail,
        'trend': trend,
        'sourceRefs': sourceRefs.map((e) => e.toJson()).toList(),
      };
}

/// Phản hồi insight Thói quen / Tài chính.
@immutable
class AiInsightResponse {
  const AiInsightResponse({
    required this.summary,
    required this.observations,
    required this.contextMessageId,
  });

  factory AiInsightResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawObs = json['observations'] as List<dynamic>?;
    return AiInsightResponse(
      summary: json['summary'] as String? ?? '',
      observations: rawObs != null
          ? rawObs
              .map((dynamic e) =>
                  AiObservation.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiObservation>[],
      contextMessageId: json['contextMessageId'] as String? ?? '',
    );
  }

  final String summary;
  final List<AiObservation> observations;
  final String contextMessageId;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'summary': summary,
        'observations': observations.map((e) => e.toJson()).toList(),
        'contextMessageId': contextMessageId,
      };
}

/// Phản hồi tóm tắt ghi chú (`POST /api/v1/ai/notes/{id}/summary`).
@immutable
class AiNoteSummaryResponse {
  const AiNoteSummaryResponse({
    required this.noteId,
    required this.summary,
    required this.keyPoints,
    required this.contextMessageId,
  });

  factory AiNoteSummaryResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawPoints = json['keyPoints'] as List<dynamic>?;
    return AiNoteSummaryResponse(
      noteId: json['noteId'] as String? ?? '',
      summary: json['summary'] as String? ?? '',
      keyPoints: rawPoints != null
          ? rawPoints.map((dynamic e) => e.toString()).toList()
          : const <String>[],
      contextMessageId: json['contextMessageId'] as String? ?? '',
    );
  }

  final String noteId;
  final String summary;
  final List<String> keyPoints;
  final String contextMessageId;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'noteId': noteId,
        'summary': summary,
        'keyPoints': keyPoints,
        'contextMessageId': contextMessageId,
      };
}

/// Thay đổi đề xuất trong action proposal (dùng để kiểm thử fixture `ai-action-proposal.response.json`).
@immutable
class AiProposedChange {
  const AiProposedChange({
    required this.field,
    this.before,
    this.after,
  });

  factory AiProposedChange.fromJson(Map<String, dynamic> json) {
    return AiProposedChange(
      field: json['field'] as String? ?? '',
      before: json['before'],
      after: json['after'],
    );
  }

  final String field;
  final dynamic before;
  final dynamic after;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'field': field,
        'before': before,
        'after': after,
      };
}

/// Đề xuất hành động từ AI (`ai-action-proposal.response.json`).
@immutable
class AiActionProposalResponse {
  const AiActionProposalResponse({
    required this.id,
    required this.conversationId,
    required this.sourceMessageId,
    required this.actionType,
    required this.status,
    required this.version,
    required this.payloadHash,
    required this.title,
    required this.changes,
    required this.expiresAt,
    this.result,
    this.failureCode,
    required this.createdAt,
    required this.updatedAt,
  });

  factory AiActionProposalResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawChanges = json['changes'] as List<dynamic>?;
    return AiActionProposalResponse(
      id: json['id'] as String,
      conversationId: json['conversationId'] as String,
      sourceMessageId: json['sourceMessageId'] as String,
      actionType: json['actionType'] as String,
      status: json['status'] as String,
      version: json['version'] as int? ?? 1,
      payloadHash: json['payloadHash'] as String? ?? '',
      title: json['title'] as String? ?? '',
      changes: rawChanges != null
          ? rawChanges
              .map((dynamic e) =>
                  AiProposedChange.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiProposedChange>[],
      expiresAt: json['expiresAt'] as String,
      result: json['result'],
      failureCode: json['failureCode'] as String?,
      createdAt: json['createdAt'] as String,
      updatedAt: json['updatedAt'] as String,
    );
  }

  final String id;
  final String conversationId;
  final String sourceMessageId;
  final String actionType;
  final String status;
  final int version;
  final String payloadHash;
  final String title;
  final List<AiProposedChange> changes;
  final String expiresAt;
  final dynamic result;
  final String? failureCode;
  final String createdAt;
  final String updatedAt;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'conversationId': conversationId,
        'sourceMessageId': sourceMessageId,
        'actionType': actionType,
        'status': status,
        'version': version,
        'payloadHash': payloadHash,
        'title': title,
        'changes': changes.map((e) => e.toJson()).toList(),
        'expiresAt': expiresAt,
        'result': result,
        'failureCode': failureCode,
        'createdAt': createdAt,
        'updatedAt': updatedAt,
      };
}

/// Dữ liệu sử dụng theo từng ngày (`GET /api/v1/ai/usage`).
@immutable
class AiUsageDay {
  const AiUsageDay({
    required this.date,
    required this.requests,
    required this.inputTokens,
    required this.outputTokens,
    required this.estimatedCostMicrousd,
  });

  factory AiUsageDay.fromJson(Map<String, dynamic> json) {
    return AiUsageDay(
      date: json['date'] as String? ?? '',
      requests: json['requests'] as int? ?? 0,
      inputTokens: json['inputTokens'] as int? ?? 0,
      outputTokens: json['outputTokens'] as int? ?? 0,
      estimatedCostMicrousd: json['estimatedCostMicrousd'] as int? ?? 0,
    );
  }

  final String date;
  final int requests;
  final int inputTokens;
  final int outputTokens;
  final int estimatedCostMicrousd;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'date': date,
        'requests': requests,
        'inputTokens': inputTokens,
        'outputTokens': outputTokens,
        'estimatedCostMicrousd': estimatedCostMicrousd,
      };
}

/// Tổng kết dữ liệu sử dụng AI.
@immutable
class AiUsageTotals {
  const AiUsageTotals({
    required this.requests,
    required this.inputTokens,
    required this.outputTokens,
    required this.estimatedCostMicrousd,
  });

  factory AiUsageTotals.fromJson(Map<String, dynamic> json) {
    return AiUsageTotals(
      requests: json['requests'] as int? ?? 0,
      inputTokens: json['inputTokens'] as int? ?? 0,
      outputTokens: json['outputTokens'] as int? ?? 0,
      estimatedCostMicrousd: json['estimatedCostMicrousd'] as int? ?? 0,
    );
  }

  final int requests;
  final int inputTokens;
  final int outputTokens;
  final int estimatedCostMicrousd;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'requests': requests,
        'inputTokens': inputTokens,
        'outputTokens': outputTokens,
        'estimatedCostMicrousd': estimatedCostMicrousd,
      };
}

/// Phản hồi mức sử dụng AI (`GET /api/v1/ai/usage`).
@immutable
class AiUsageResponse {
  const AiUsageResponse({
    required this.from,
    required this.to,
    required this.days,
    required this.totals,
  });

  factory AiUsageResponse.fromJson(Map<String, dynamic> json) {
    final List<dynamic>? rawDays = json['days'] as List<dynamic>?;
    return AiUsageResponse(
      from: json['from'] as String? ?? '',
      to: json['to'] as String? ?? '',
      days: rawDays != null
          ? rawDays
              .map((dynamic e) => AiUsageDay.fromJson(e as Map<String, dynamic>))
              .toList()
          : const <AiUsageDay>[],
      totals: json['totals'] is Map<String, dynamic>
          ? AiUsageTotals.fromJson(json['totals'] as Map<String, dynamic>)
          : const AiUsageTotals(
              requests: 0,
              inputTokens: 0,
              outputTokens: 0,
              estimatedCostMicrousd: 0,
            ),
    );
  }

  final String from;
  final String to;
  final List<AiUsageDay> days;
  final AiUsageTotals totals;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'from': from,
        'to': to,
        'days': days.map((e) => e.toJson()).toList(),
        'totals': totals.toJson(),
      };
}

/// Cửa sổ hạn mức (ngày hoặc tháng).
@immutable
class AiQuotaWindow {
  const AiQuotaWindow({
    required this.requestLimit,
    required this.requestsUsed,
    required this.inputTokenLimit,
    required this.inputTokensUsed,
    required this.outputTokenLimit,
    required this.outputTokensUsed,
    required this.resetsAt,
  });

  factory AiQuotaWindow.fromJson(Map<String, dynamic> json) {
    return AiQuotaWindow(
      requestLimit: json['requestLimit'] as int? ?? 0,
      requestsUsed: json['requestsUsed'] as int? ?? 0,
      inputTokenLimit: json['inputTokenLimit'] as int? ?? 0,
      inputTokensUsed: json['inputTokensUsed'] as int? ?? 0,
      outputTokenLimit: json['outputTokenLimit'] as int? ?? 0,
      outputTokensUsed: json['outputTokensUsed'] as int? ?? 0,
      resetsAt: json['resetsAt'] as String? ?? '',
    );
  }

  final int requestLimit;
  final int requestsUsed;
  final int inputTokenLimit;
  final int inputTokensUsed;
  final int outputTokenLimit;
  final int outputTokensUsed;
  final String resetsAt;

  int get remainingRequests =>
      (requestLimit - requestsUsed) > 0 ? (requestLimit - requestsUsed) : 0;

  bool get isNearLimit {
    if (requestLimit <= 0) return false;
    return requestsUsed * 10 >= requestLimit * 8; // >= 80% (integer math)
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'requestLimit': requestLimit,
        'requestsUsed': requestsUsed,
        'inputTokenLimit': inputTokenLimit,
        'inputTokensUsed': inputTokensUsed,
        'outputTokenLimit': outputTokenLimit,
        'outputTokensUsed': outputTokensUsed,
        'resetsAt': resetsAt,
      };
}

/// Phản hồi hạn mức và trạng thái dịch vụ (`GET /api/v1/ai/limits`).
@immutable
class AiLimitsResponse {
  const AiLimitsResponse({
    required this.enabled,
    required this.budgetAvailable,
    required this.daily,
    required this.monthly,
    required this.rateLimitPerMinute,
    required this.maxConcurrentPerUser,
  });

  factory AiLimitsResponse.fromJson(Map<String, dynamic> json) {
    return AiLimitsResponse(
      enabled: json['enabled'] as bool? ?? false,
      budgetAvailable: json['budgetAvailable'] as bool? ?? false,
      daily: json['daily'] is Map<String, dynamic>
          ? AiQuotaWindow.fromJson(json['daily'] as Map<String, dynamic>)
          : const AiQuotaWindow(
              requestLimit: 0,
              requestsUsed: 0,
              inputTokenLimit: 0,
              inputTokensUsed: 0,
              outputTokenLimit: 0,
              outputTokensUsed: 0,
              resetsAt: '',
            ),
      monthly: json['monthly'] is Map<String, dynamic>
          ? AiQuotaWindow.fromJson(json['monthly'] as Map<String, dynamic>)
          : const AiQuotaWindow(
              requestLimit: 0,
              requestsUsed: 0,
              inputTokenLimit: 0,
              inputTokensUsed: 0,
              outputTokenLimit: 0,
              outputTokensUsed: 0,
              resetsAt: '',
            ),
      rateLimitPerMinute: json['rateLimitPerMinute'] as int? ?? 5,
      maxConcurrentPerUser: json['maxConcurrentPerUser'] as int? ?? 2,
    );
  }

  final bool enabled;
  final bool budgetAvailable;
  final AiQuotaWindow daily;
  final AiQuotaWindow monthly;
  final int rateLimitPerMinute;
  final int maxConcurrentPerUser;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'enabled': enabled,
        'budgetAvailable': budgetAvailable,
        'daily': daily.toJson(),
        'monthly': monthly.toJson(),
        'rateLimitPerMinute': rateLimitPerMinute,
        'maxConcurrentPerUser': maxConcurrentPerUser,
      };
}

/// Định dạng micro-USD (1 USD = 1.000.000 µUSD) sang chuỗi hiển thị gọn.
///
/// TUYỆT ĐỐI KHÔNG dùng phép tính `double/float` (tuân thủ quy định tài chính của hệ thống).
String formatMicroUsd(int microUsd) {
  if (microUsd <= 0) return '0.00 USD';
  final int wholeUsd = microUsd ~/ 1000000;
  final int remainder = microUsd % 1000000;
  if (wholeUsd > 0) {
    final int cents = remainder ~/ 10000; // 2 số thập phân
    return '\$$wholeUsd.${cents.toString().padLeft(2, '0')} USD';
  } else if (remainder >= 100) {
    // 4 số thập phân (e.g. 6880 -> 0068 => $0.0068 USD)
    final int sub = remainder ~/ 100;
    return '\$0.${sub.toString().padLeft(4, '0')} USD';
  } else {
    return '$microUsd µUSD';
  }
}

