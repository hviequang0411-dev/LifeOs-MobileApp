import 'package:flutter/material.dart';
import '../../../../core/theme/app_tokens.dart';

class TimelineEntryItem {
  const TimelineEntryItem({
    required this.id,
    required this.title,
    required this.timeStr,
    required this.kind, // 'EVENT' | 'TIME_BLOCK' | 'TASK'
    required this.isDone,
    this.durationMinutes,
    this.entityId,
    this.occurrenceStartLocal,
  });

  final String id;
  final String title;
  final String timeStr;
  final String kind;
  final bool isDone;
  final int? durationMinutes;
  final String? entityId;
  final String? occurrenceStartLocal;
}

/// Widget Dòng thời gian ngày kết hợp (Integrated Daily Timeline)
class DailyTimelineWidget extends StatelessWidget {
  const DailyTimelineWidget({
    super.key,
    required this.items,
    this.onItemTap,
  });

  final List<TimelineEntryItem> items;
  final void Function(TimelineEntryItem item)? onItemTap;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    if (items.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(LifeOsTokens.space4),
        decoration: BoxDecoration(
          color: tokens.bgSurface,
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
          border: Border.all(color: tokens.borderSubtle),
          boxShadow: LifeOsTokens.shadowElevation1,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Icon(
                  Icons.timeline_rounded,
                  size: 18,
                  color: tokens.domainCalendar,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Dòng thời gian hôm nay',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: LifeOsTokens.space2),
            Text(
              'Hôm nay chưa có lịch trình hoặc khối thời gian.',
              style: TextStyle(
                fontSize: 13,
                color: tokens.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    return Semantics(
      label: 'Dòng thời gian hôm nay có ${items.length} mục',
      child: Container(
        padding: const EdgeInsets.all(LifeOsTokens.space4),
        decoration: BoxDecoration(
          color: tokens.bgSurface,
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
          border: Border.all(color: tokens.borderSubtle),
          boxShadow: LifeOsTokens.shadowElevation1,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Icon(
                  Icons.timeline_rounded,
                  size: 18,
                  color: tokens.domainCalendar,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Dòng thời gian hôm nay',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: LifeOsTokens.space3),
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: items.length,
              separatorBuilder: (BuildContext context, int index) => const SizedBox(height: 12),
              itemBuilder: (BuildContext context, int index) {
                final TimelineEntryItem item = items[index];
                Color nodeColor = tokens.domainCalendar;
                IconData nodeIcon = Icons.calendar_today_rounded;

                if (item.kind == 'TIME_BLOCK') {
                  nodeColor = tokens.domainTask;
                  nodeIcon = Icons.hourglass_top_rounded;
                } else if (item.kind == 'TASK') {
                  nodeColor = tokens.accentSecondary;
                  nodeIcon = Icons.task_alt_rounded;
                }

                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    // Cột giờ bên trái
                    SizedBox(
                      width: 52,
                      child: Text(
                        item.timeStr,
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: tokens.textSecondary,
                          fontFeatures: const <FontFeature>[
                            FontFeature.tabularFigures(),
                          ],
                        ),
                      ),
                    ),
                    // Điểm nút timeline
                    Column(
                      children: <Widget>[
                        Container(
                          width: 22,
                          height: 22,
                          decoration: BoxDecoration(
                            color: nodeColor.withValues(alpha: 0.15),
                            shape: BoxShape.circle,
                            border: Border.all(color: nodeColor, width: 1.5),
                          ),
                          child: Icon(nodeIcon, size: 12, color: nodeColor),
                        ),
                      ],
                    ),
                    const SizedBox(width: 10),
                    // Thẻ nội dung
                    Expanded(
                      child: InkWell(
                        borderRadius:
                            BorderRadius.circular(LifeOsTokens.radiusMd),
                        onTap: onItemTap != null ? () => onItemTap!(item) : null,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 8,
                          ),
                          decoration: BoxDecoration(
                            color: tokens.bgElevated,
                            borderRadius:
                                BorderRadius.circular(LifeOsTokens.radiusMd),
                            border: Border.all(color: tokens.borderSubtle),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                item.title,
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: tokens.textPrimary,
                                  decoration: item.isDone
                                      ? TextDecoration.lineThrough
                                      : null,
                                ),
                              ),
                              if (item.durationMinutes != null) ...<Widget>[
                                const SizedBox(height: 2),
                                Text(
                                  '${item.durationMinutes} phút tập trung',
                                  style: TextStyle(
                                    fontSize: 11,
                                    color: tokens.textMuted,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

