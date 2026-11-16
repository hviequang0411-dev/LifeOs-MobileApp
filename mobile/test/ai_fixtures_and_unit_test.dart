import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/l10n/vi_strings.dart';
import 'package:lifeos_mobile/features/ai/data/ai_models.dart';

void main() {
  group('AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json)', () {
    String readFixture(String filename) {
      final String path = '../docs/api/fixtures/$filename';
      final File file = File(path);
      if (!file.existsSync()) {
        final File fallback = File('docs/api/fixtures/$filename');
        if (fallback.existsSync()) return fallback.readAsStringSync();
        throw Exception('Không tìm thấy fixture: $path');
      }
      return file.readAsStringSync();
    }

    test('1. Parse ai-preferences.response.json', () {
      final String raw = readFixture('ai-preferences.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiPreferencesResponse resp = AiPreferencesResponse.fromJson(json);

      expect(resp.taskCalendar, isTrue);
      expect(resp.habit, isTrue);
      expect(resp.goal, isTrue);
      expect(resp.notes, isFalse);
      expect(resp.finance, isFalse);
      expect(resp.policyVersion, equals('2026-11-10'));
      expect(resp.providerNoticeVersion, equals('openai-api-2026-11-10'));
      expect(resp.features.chat, isTrue);
      expect(resp.features.insights, isTrue);
      expect(resp.features.notes, isFalse);
      expect(resp.allowedDomainsCount, equals(3));
      expect(resp.isDomainAllowed(AiContextDomain.taskCalendar), isTrue);
      expect(resp.isDomainAllowed(AiContextDomain.finance), isFalse);

      final Map<String, dynamic> reEncoded = resp.toJson();
      expect(reEncoded['taskCalendar'], isTrue);
      expect(reEncoded['features']['chat'], isTrue);
    });

    test('2. Parse ai-chat.request.json', () {
      final String raw = readFixture('ai-chat.request.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiChatRequest req = AiChatRequest.fromJson(json);

      expect(req.message, equals('Hôm nay tôi nên ưu tiên việc nào?'));
      expect(req.mode, equals(AiGenerationMode.chat));
      expect(req.contextDomains, containsAll(<String>['TASK_CALENDAR', 'GOAL']));
    });

    test('3. Parse ai-chat.response.json', () {
      final String raw = readFixture('ai-chat.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiChatResponse resp = AiChatResponse.fromJson(json);

      expect(resp.conversationId, equals('10000000-0000-4000-8000-000000000001'));
      expect(resp.userMessage.role, equals(AiMessageRole.user));
      expect(resp.userMessage.content, equals('Hôm nay tôi nên ưu tiên việc nào?'));
      expect(resp.userMessage.contextAvailable, isFalse);
      expect(resp.assistantMessage.role, equals(AiMessageRole.assistant));
      expect(resp.assistantMessage.content,
          contains('Ưu tiên hoàn thiện báo cáo'));
      expect(resp.assistantMessage.contextAvailable, isTrue);
      expect(resp.actionProposals, isEmpty);
    });

    test('4. Parse ai-context.response.json', () {
      final String raw = readFixture('ai-context.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiContextResponse resp = AiContextResponse.fromJson(json);

      expect(resp.messageId, equals('10000000-0000-4000-8000-000000000003'));
      expect(resp.capturedAt, equals('2026-11-10T08:00:01Z'));
      expect(resp.domains, containsAll(<String>['TASK_CALENDAR', 'GOAL']));
      expect(resp.entities, hasLength(1));
      expect(resp.entities.first.domain, equals('TASK_CALENDAR'));
      expect(resp.entities.first.entityType, equals('task'));
      expect(resp.entities.first.entityVersion, equals(3));
      expect(resp.entities.first.fields['title'], equals('Hoàn thiện báo cáo'));
      expect(resp.truncated, isFalse);
    });

    test('5. Parse ai-error.response.json (RFC 9457)', () {
      final String raw = readFixture('ai-error.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;

      expect(json['errorCode'], equals('AI_CONSENT_REQUIRED'));
      expect(json['status'], equals(403));
      expect(json['requiredDomains'], contains('FINANCE'));

      final AiErrorDescriptor desc =
          ViStrings.mapAiError(json['errorCode'] as String?);
      expect(desc.message, contains('quyền truy cập dữ liệu'));
      expect(desc.suggestedAction, contains('cài đặt quyền AI'));
      expect(desc.actionType, equals(AiErrorActionType.openConsent));
    });

    test('6. Parse ai-plan.response.json', () {
      final String raw = readFixture('ai-plan.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiPlanResponse resp = AiPlanResponse.fromJson(json);

      expect(resp.periodType, equals('DAY'));
      expect(resp.periodStart, equals('2026-11-10'));
      expect(resp.summary, contains('Một việc trọng tâm'));
      expect(resp.items, hasLength(1));
      expect(resp.items.first.title, equals('Hoàn thiện báo cáo'));
      expect(resp.items.first.priority, equals('HIGH'));
      expect(resp.items.first.sourceRefs, hasLength(1));
      expect(resp.items.first.sourceRefs.first.entityType, equals('task'));
    });

    test('7. Parse ai-usage.response.json', () {
      final String raw = readFixture('ai-usage.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiUsageResponse resp = AiUsageResponse.fromJson(json);

      expect(resp.from, equals('2026-11-10'));
      expect(resp.to, equals('2026-11-10'));
      expect(resp.days, hasLength(1));
      expect(resp.days.first.requests, equals(2));
      expect(resp.days.first.inputTokens, equals(1840));
      expect(resp.days.first.outputTokens, equals(320));
      expect(resp.days.first.estimatedCostMicrousd, equals(6880));
      expect(resp.totals.estimatedCostMicrousd, equals(6880));

      // Test formatMicroUsd với 6880 micro-USD
      final String formattedCost = formatMicroUsd(resp.totals.estimatedCostMicrousd);
      expect(formattedCost, equals('\$0.0068 USD'));
    });

    test('8. Parse ai-action-proposal.response.json', () {
      final String raw = readFixture('ai-action-proposal.response.json');
      final Map<String, dynamic> json = jsonDecode(raw) as Map<String, dynamic>;
      final AiActionProposalResponse resp =
          AiActionProposalResponse.fromJson(json);

      expect(resp.id, equals('30000000-0000-4000-8000-000000000001'));
      expect(resp.actionType, equals('UPDATE_TASK_PRIORITY'));
      expect(resp.status, equals('PROPOSED'));
      expect(resp.version, equals(1));
      expect(resp.changes, hasLength(1));
      expect(resp.changes.first.field, equals('priority'));
      expect(resp.changes.first.before, equals('MEDIUM'));
      expect(resp.changes.first.after, equals('HIGH'));
    });
  });

  group('AWP-2 & AWP-6: Ánh xạ 15 Mã lỗi AI OpenAPI sang tiếng Việt & Gợi ý hành động', () {
    const List<String> all15AiErrorCodes = <String>[
      'AI_DISABLED',
      'AI_CONSENT_REQUIRED',
      'AI_CONTEXT_DOMAIN_NOT_ALLOWED',
      'AI_CONVERSATION_NOT_FOUND',
      'AI_MESSAGE_TOO_LARGE',
      'AI_RATE_LIMITED',
      'AI_QUOTA_EXCEEDED',
      'AI_BUDGET_EXCEEDED',
      'AI_PROVIDER_UNAVAILABLE',
      'AI_RESPONSE_INVALID',
      'AI_ACTION_NOT_FOUND',
      'AI_ACTION_EXPIRED',
      'AI_ACTION_STATE_CONFLICT',
      'AI_ACTION_FORBIDDEN',
      'AI_ACTION_VERSION_CONFLICT',
    ];

    test('Bao phủ đủ 15 mã lỗi trong ViStrings.mapBackendErrorCode và ViStrings.mapAiError', () {
      expect(all15AiErrorCodes.length, equals(15));

      for (final String code in all15AiErrorCodes) {
        final String viMsg = ViStrings.mapBackendErrorCode(code);
        expect(viMsg, isNotEmpty, reason: '$code phải có thông điệp tiếng Việt');
        expect(viMsg, isNot(contains(code)),
            reason: '$code không được lộ mã kỹ thuật ra người dùng');

        final AiErrorDescriptor desc = ViStrings.mapAiError(code);
        expect(desc.message, equals(viMsg));
        expect(desc.suggestedAction, isNotEmpty,
            reason: '$code phải có hành động gợi ý rõ ràng');
      }
    });

    test('Kiểm tra đúng loại hành động gợi ý cho các mã quan trọng', () {
      expect(ViStrings.mapAiError('AI_CONSENT_REQUIRED').actionType,
          equals(AiErrorActionType.openConsent));
      expect(ViStrings.mapAiError('AI_CONTEXT_DOMAIN_NOT_ALLOWED').actionType,
          equals(AiErrorActionType.openConsent));
      expect(ViStrings.mapAiError('AI_QUOTA_EXCEEDED').actionType,
          equals(AiErrorActionType.viewLimits));
      expect(ViStrings.mapAiError('AI_BUDGET_EXCEEDED').actionType,
          equals(AiErrorActionType.viewLimits));
      expect(ViStrings.mapAiError('AI_PROVIDER_UNAVAILABLE').actionType,
          equals(AiErrorActionType.retry));
      expect(ViStrings.mapAiError('AI_RATE_LIMITED').actionType,
          equals(AiErrorActionType.retry));
      expect(ViStrings.mapAiError('AI_CONVERSATION_NOT_FOUND').actionType,
          equals(AiErrorActionType.newConversation));
    });
  });

  group('AWP-5: Tính toán micro-USD và hạn mức không dùng số thực double', () {
    test('formatMicroUsd định dạng chính xác bằng phép chia nguyên', () {
      expect(formatMicroUsd(0), equals('0.00 USD'));
      expect(formatMicroUsd(-50), equals('0.00 USD'));
      expect(formatMicroUsd(50), equals('50 µUSD'));
      expect(formatMicroUsd(6880), equals('\$0.0068 USD'));
      expect(formatMicroUsd(1000000), equals('\$1.00 USD'));
      expect(formatMicroUsd(25400000), equals('\$25.40 USD'));
    });

    test('AiQuotaWindow.isNearLimit báo cảnh báo khi đạt >= 80% (integer math)', () {
      const AiQuotaWindow underLimit = AiQuotaWindow(
        requestLimit: 100,
        requestsUsed: 79,
        inputTokenLimit: 1000,
        inputTokensUsed: 500,
        outputTokenLimit: 500,
        outputTokensUsed: 100,
        resetsAt: '2026-11-10T24:00:00Z',
      );
      expect(underLimit.isNearLimit, isFalse);

      const AiQuotaWindow atLimit = AiQuotaWindow(
        requestLimit: 100,
        requestsUsed: 80,
        inputTokenLimit: 1000,
        inputTokensUsed: 500,
        outputTokenLimit: 500,
        outputTokensUsed: 100,
        resetsAt: '2026-11-10T24:00:00Z',
      );
      expect(atLimit.isNearLimit, isTrue);

      const AiQuotaWindow overLimit = AiQuotaWindow(
        requestLimit: 20,
        requestsUsed: 19,
        inputTokenLimit: 1000,
        inputTokensUsed: 500,
        outputTokenLimit: 500,
        outputTokensUsed: 100,
        resetsAt: '2026-11-10T24:00:00Z',
      );
      expect(overLimit.isNearLimit, isTrue);
    });

    test('AiPreferencesResponse.initialDefault mặc định toàn bộ 5 miền TẮT (D264)', () {
      final AiPreferencesResponse def = AiPreferencesResponse.initialDefault();
      expect(def.taskCalendar, isFalse);
      expect(def.habit, isFalse);
      expect(def.goal, isFalse);
      expect(def.notes, isFalse);
      expect(def.finance, isFalse);
      expect(def.allowedDomainsCount, equals(0));
      expect(def.allowedDomainCodes, isEmpty);
    });
  });
}

