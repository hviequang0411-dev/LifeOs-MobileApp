/// Quy tắc xác định "Hôm nay" (`YYYY-MM-DD`) và ranh giới tuần theo cài đặt `user_settings`
/// (`docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md` D192 dòng 49 & `docs/api/openapi.yaml` dòng 8175–8201):
/// - "Ngày" = `[00:00, 24:00)` giờ địa phương theo `user_settings.timezone` hiện tại (mặc định `Asia/Ho_Chi_Minh`).
/// - "Tuần" bắt đầu theo `user_settings.weekStartDay` (`1` = Thứ Hai … `7` = Chủ Nhật; mặc định `1`).
/// - Không sử dụng trường giả định `dayStartHour` (do `user_settings` trên backend không có trường này).
class UserDayClock {
  const UserDayClock({
    this.timezoneId = 'Asia/Ho_Chi_Minh',
    this.utcOffsetMinutes = 420,
    this.weekStartDay = 1,
  }) : assert(weekStartDay >= 1 && weekStartDay <= 7);

  final String timezoneId;
  final int utcOffsetMinutes;
  final int weekStartDay;

  /// Khởi tạo từ bản ghi `user_settings` (`timezone`, `weekStartDay`).
  factory UserDayClock.fromUserSettings({
    String timezone = 'Asia/Ho_Chi_Minh',
    int weekStartDay = 1,
  }) {
    final int offsetMinutes = _resolveTimezoneOffsetMinutes(timezone);
    final int safeWeekStart = weekStartDay.clamp(1, 7);
    return UserDayClock(
      timezoneId: timezone,
      utcOffsetMinutes: offsetMinutes,
      weekStartDay: safeWeekStart,
    );
  }

  static int _resolveTimezoneOffsetMinutes(String tz) {
    return switch (tz) {
      'Asia/Ho_Chi_Minh' || 'Asia/Saigon' || 'Asia/Bangkok' => 7 * 60,
      'UTC' || 'Etc/UTC' || 'GMT' => 0,
      'Asia/Singapore' || 'Asia/Tokyo' => tz == 'Asia/Tokyo' ? 9 * 60 : 8 * 60,
      'America/New_York' => -5 * 60,
      'America/Los_Angeles' => -8 * 60,
      'Europe/London' => 0,
      'Europe/Paris' || 'Europe/Berlin' => 1 * 60,
      _ => 7 * 60,
    };
  }

  /// Trả về ngày nghiệp vụ `YYYY-MM-DD` (`[00:00, 24:00)` theo `timezone` của user) cho một thời điểm UTC.
  String businessDateForUtc(DateTime utcInstant) {
    final DateTime shifted = toUserWallClock(utcInstant);
    return formatIsoDate(shifted.year, shifted.month, shifted.day);
  }

  /// Trả về ngày đầu tuần (`YYYY-MM-DD`) chứa `isoDate` theo `weekStartDay` (`1..7`).
  String startOfWeekForDate(String isoDate) {
    final DateTime dt = parseIsoDateUtc(isoDate);
    final int diff = (dt.weekday - weekStartDay + 7) % 7;
    final DateTime start = dt.subtract(Duration(days: diff));
    return formatIsoDate(start.year, start.month, start.day);
  }

  /// Chuyển đổi thời điểm UTC sang giờ địa phương của người dùng.
  DateTime toUserWallClock(DateTime utcInstant) {
    return utcInstant.toUtc().add(Duration(minutes: utcOffsetMinutes));
  }

  /// Trả về chuỗi `YYYY-MM-DD`.
  static String formatIsoDate(int year, int month, int day) {
    final String y = year.toString().padLeft(4, '0');
    final String m = month.toString().padLeft(2, '0');
    final String d = day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  /// Phân tích chuỗi `YYYY-MM-DD` thành `DateTime.utc(year, month, day)`.
  static DateTime parseIsoDateUtc(String isoDate) {
    final List<String> parts = isoDate.split('-');
    if (parts.length != 3) {
      throw FormatException('Ngày không đúng định dạng YYYY-MM-DD: "$isoDate"');
    }
    return DateTime.utc(
      int.parse(parts[0]),
      int.parse(parts[1]),
      int.parse(parts[2]),
    );
  }

  /// Cộng/trừ số ngày trên chuỗi `YYYY-MM-DD`.
  static String addDays(String isoDate, int deltaDays) {
    final DateTime dt = parseIsoDateUtc(isoDate).add(Duration(days: deltaDays));
    return formatIsoDate(dt.year, dt.month, dt.day);
  }
}
