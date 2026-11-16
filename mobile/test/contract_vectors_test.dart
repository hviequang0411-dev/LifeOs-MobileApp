import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/calculators/analytics_calculator.dart';
import 'package:lifeos_mobile/core/calculators/habit_calculator.dart';
import 'package:lifeos_mobile/core/calculators/recurrence_calculator.dart';
import 'package:lifeos_mobile/core/money/decimal10k.dart';
import 'package:lifeos_mobile/core/sync/entity_types.dart';
import 'package:lifeos_mobile/core/sync/three_way_merger.dart';

Map<String, dynamic> _loadFixtureJson(String filename) {
  final List<String> candidates = <String>[
    '../docs/api/fixtures/$filename',
    'docs/api/fixtures/$filename',
  ];
  for (final String path in candidates) {
    final File f = File(path);
    if (f.existsSync()) {
      return (jsonDecode(f.readAsStringSync()) as Map<dynamic, dynamic>)
          .cast<String, dynamic>();
    }
  }
  throw StateError('Không tìm thấy tệp fixture: $filename');
}

void main() {
  group('PWP6 — Kiểm thử đối chiếu Vector Chuỗi ngày Thói quen (habit-vectors.json)', () {
    test('Khớp 100% toàn bộ ca kiểm thử trong habit-vectors.json với Server Java', () {
      final Map<String, dynamic> doc = _loadFixtureJson('habit-vectors.json');
      final List<dynamic> cases = doc['cases'] as List<dynamic>;
      expect(cases, isNotEmpty);

      for (final dynamic raw in cases) {
        final Map<String, dynamic> c =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final String name = c['name'] as String;
        final Map<String, dynamic> expected =
            (c['expected'] as Map<dynamic, dynamic>).cast<String, dynamic>();
        final Map<String, dynamic> expectedStats =
            (expected['stats'] as Map<dynamic, dynamic>).cast<String, dynamic>();

        final HabitStatsResult actual =
            HabitCalculator.evaluateSharedVectorCase(c);

        expect(actual.currentStreak, expected['currentStreak'],
            reason: 'Ca [$name] lệch currentStreak');
        expect(actual.bestStreak, expected['longestStreak'],
            reason: 'Ca [$name] lệch longestStreak');
        expect(actual.scheduledCount, expectedStats['scheduled'],
            reason: 'Ca [$name] lệch stats.scheduled');
        expect(actual.doneCount, expectedStats['done'],
            reason: 'Ca [$name] lệch stats.done');
        expect(actual.skippedCount, expectedStats['skipped'],
            reason: 'Ca [$name] lệch stats.skipped');
        expect(actual.missedCount, expectedStats['missed'],
            reason: 'Ca [$name] lệch stats.missed');
        expect(actual.completionRateString, expectedStats['completionRate'],
            reason: 'Ca [$name] lệch stats.completionRate');
      }
    });
  });

  group('PWP8 — Kiểm thử đối chiếu Vector Lặp sự kiện RFC-5545 (recurrence-vectors.json)', () {
    test('Khớp 100% toàn bộ ca kiểm thử trong recurrence-vectors.json với Server Java', () {
      final Map<String, dynamic> doc =
          _loadFixtureJson('recurrence-vectors.json');
      final List<dynamic> cases = doc['cases'] as List<dynamic>;
      expect(cases, isNotEmpty);

      for (final dynamic raw in cases) {
        final Map<String, dynamic> c =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final String name = c['name'] as String;
        final List<String> expectedLocal = (c['expected'] as List<dynamic>)
            .map((dynamic e) => e.toString())
            .toList();

        final ({
          List<String> localOccurrences,
          List<String> utcOccurrences,
        }) actual = RecurrenceCalculator.evaluateSharedVectorCase(c);

        expect(actual.localOccurrences, expectedLocal,
            reason: 'Ca [$name] lệch danh sách expected giờ địa phương');

        if (c['expectedUtc'] != null) {
          final List<String> expectedUtc = (c['expectedUtc'] as List<dynamic>)
              .map((dynamic e) => e.toString())
              .toList();
          expect(actual.utcOccurrences, expectedUtc,
              reason: 'Ca [$name] lệch danh sách expectedUtc (DST)');
        }
      }
    });
  });

  group('PWP12 — Kiểm thử đối chiếu Vector Analytics (analytics-vectors.json)', () {
    test('Khớp 100% toàn bộ ca periods, tasks, finance, goals trong analytics-vectors.json', () {
      final Map<String, dynamic> doc =
          _loadFixtureJson('analytics-vectors.json');

      // 1. Periods (D192)
      final List<dynamic> periods = doc['periods'] as List<dynamic>;
      for (final dynamic raw in periods) {
        final Map<String, dynamic> p =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final ({String start, String end, String? startAt, String? endAt}) res =
            AnalyticsCalculator.evaluatePeriodVector(p);
        expect(res.start, p['start']);
        expect(res.end, p['end']);
        if (p.containsKey('startAt')) {
          expect(res.startAt, p['startAt']);
          expect(res.endAt, p['endAt']);
        }
      }

      // 2. Tasks (D198)
      final List<dynamic> tasks = doc['tasks'] as List<dynamic>;
      for (final dynamic raw in tasks) {
        final Map<String, dynamic> t =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final Map<String, dynamic> expected =
            (t['expected'] as Map<dynamic, dynamic>).cast<String, dynamic>();
        final Map<String, dynamic> actual =
            AnalyticsCalculator.evaluateTasksVector(t);
        expect(actual, expected);
      }

      // 3. Finance (D199)
      final List<dynamic> finance = doc['finance'] as List<dynamic>;
      for (final dynamic raw in finance) {
        final Map<String, dynamic> f =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final Map<String, String?> actual =
            AnalyticsCalculator.evaluateFinanceVector(f);
        if (f.containsKey('savingsRate')) {
          expect(actual['savingsRate'], f['savingsRate']);
        } else {
          expect(actual['pace'], f['pace']);
        }
      }

      // 4. Goals (D200)
      final List<dynamic> goals = doc['goals'] as List<dynamic>;
      for (final dynamic raw in goals) {
        final Map<String, dynamic> g =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final String? actualRisk = AnalyticsCalculator.evaluateGoalRiskVector(g);
        expect(actualRisk, g['risk']);
      }
    });
  });

  group('PWP4 — Kiểm thử đối chiếu Vector Hợp nhất Xung đột 3 bên (merge-vectors.json)', () {
    test('Khớp 100% toàn bộ vector MERGE-01..MERGE-05', () {
      final Map<String, dynamic> doc = _loadFixtureJson('merge-vectors.json');
      final List<dynamic> vectors = doc['vectors'] as List<dynamic>;
      expect(vectors.length, 5);

      for (final dynamic raw in vectors) {
        final Map<String, dynamic> vec =
            (raw as Map<dynamic, dynamic>).cast<String, dynamic>();
        final String id = vec['id'] as String;
        final SyncEntityType entityType =
            SyncEntityType.fromWireName(vec['entityType'] as String);
        final Map<String, dynamic> baseSnap =
            (vec['baseSnapshot'] as Map<dynamic, dynamic>)
                .cast<String, dynamic>();
        final Map<String, dynamic> serverCur =
            (vec['serverCurrent'] as Map<dynamic, dynamic>)
                .cast<String, dynamic>();
        final Map<String, dynamic> clientLoc =
            (vec['clientLocal'] as Map<dynamic, dynamic>)
                .cast<String, dynamic>();
        final Map<String, dynamic> expected =
            (vec['expectedOutcome'] as Map<dynamic, dynamic>)
                .cast<String, dynamic>();

        final ThreeWayMergeOutcome outcome = ThreeWayMerger.merge(
          entityType: entityType,
          baseSnapshot: baseSnap,
          serverCurrent: serverCur,
          clientLocal: clientLoc,
        );

        expect(outcome.autoMerged, expected['autoMerged'],
            reason: '$id lệch autoMerged');
        expect(outcome.conflictingFields, expected['conflictingFields'],
            reason: '$id lệch conflictingFields');
        expect(outcome.newBaseVersion, expected['newBaseVersion'],
            reason: '$id lệch newBaseVersion');
        expect(outcome.mergedPayload, expected['mergedPayload'],
            reason: '$id lệch mergedPayload');
      }
    });
  });

  group('Kiểm thử Hợp đồng 15 Fixture JSON dùng chung Java–Dart', () {
    const List<String> fixtureFiles = <String>[
      'auth-login.request.json',
      'auth-login.response.json',
      'auth-refresh.request.json',
      'auth-refresh.response.json',
      'calendar-event.response.json',
      'finance-account.response.json',
      'finance-transaction.request.json',
      'finance-transaction.response.json',
      'goal-detail.response.json',
      'goal.response.json',
      'note.response.json',
      'reminder.response.json',
      'review.response.json',
      'tag.response.json',
      'task.response.json',
    ];

    test('Đọc và kiểm tra định dạng hợp đồng tất cả 15 tệp fixture', () {
      for (final String file in fixtureFiles) {
        final Map<String, dynamic> json = _loadFixtureJson(file);
        expect(json, isNotEmpty, reason: 'Fixture $file không được rỗng');
      }

      // Kiểm tra tiền tệ trong finance-transaction luôn là chuỗi thập phân, không phải số thực
      final Map<String, dynamic> finReq =
          _loadFixtureJson('finance-transaction.request.json');
      expect(finReq['amount'], isA<String>());
      final Decimal10k parsedReqAmt =
          Decimal10k.parseJsonString(finReq['amount'] as String);
      expect(parsedReqAmt.toJsonString(), finReq['amount']);

      final Map<String, dynamic> finResp =
          _loadFixtureJson('finance-transaction.response.json');
      expect(finResp['amount'], isA<String>());
      final Decimal10k parsedRespAmt =
          Decimal10k.parseJsonString(finResp['amount'] as String);
      expect(parsedRespAmt.toJsonString(), finResp['amount']);

      final Map<String, dynamic> accResp =
          _loadFixtureJson('finance-account.response.json');
      expect(accResp['openingBalance'], isA<String>());
      expect(accResp['balance'], isA<String>());
    });
  });
}
