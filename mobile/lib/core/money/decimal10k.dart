/// Biểu diễn tiền tệ chính xác tuyệt đối theo quy tắc của LifeOS:
/// - **Cấm tuyệt đối `double` / `float`** cho mọi phép tính tài chính.
/// - Trong SQLite: lưu dưới dạng số nguyên 64-bit (`scaledValue` = giá trị thực × 10.000, tức 4 chữ số thập phân).
/// - Trong JSON hợp đồng API/Sync: luôn là chuỗi thập phân đã cắt số `0` thừa ở phần thập phân
///   (ví dụ `"125000"`, `"10.5"`, `"0.125"`, `"-50000"`).
/// - Khi nhân/chia tỷ giá hoặc tính phần trăm: dùng `BigInt` với quy tắc làm tròn `HALF_UP`
///   khớp tuyệt đối với `BigDecimal` (`RoundingMode.HALF_UP`) trên Backend Java.
class Decimal10k implements Comparable<Decimal10k> {
  const Decimal10k.fromScaled(this.scaledValue);

  static const int scaleDigits = 4;
  static const int scaleFactor = 10000;
  static const Decimal10k zero = Decimal10k.fromScaled(0);

  /// Giá trị nguyên đã nhân 10.000 (lưu trực tiếp vào cột INTEGER của SQLite).
  final int scaledValue;

  /// Khởi tạo từ số nguyên đơn vị tiền tệ (ví dụ: `Decimal10k.fromWhole(125000)` -> `1250000000`).
  factory Decimal10k.fromWhole(int wholeUnits) =>
      Decimal10k.fromScaled(wholeUnits * scaleFactor);

  /// Phân tích từ chuỗi thập phân JSON của Server (ví dụ `"125000"`, `"125000.50"`, `"-10.1234"`).
  factory Decimal10k.parseJsonString(String raw) {
    final String trimmed = raw.trim();
    if (trimmed.isEmpty) {
      throw const FormatException('Chuỗi tiền tệ không được rỗng');
    }

    final bool isNegative = trimmed.startsWith('-');
    final String unsigned =
        (isNegative || trimmed.startsWith('+')) ? trimmed.substring(1) : trimmed;

    if (unsigned.isEmpty) {
      throw FormatException('Chuỗi tiền tệ không hợp lệ: "$raw"');
    }

    final List<String> parts = unsigned.split('.');
    if (parts.length > 2) {
      throw FormatException('Chuỗi tiền tệ có nhiều dấu chấm thập phân: "$raw"');
    }

    final String wholePartStr = parts[0].isEmpty ? '0' : parts[0];
    final int wholePart = int.parse(wholePartStr);

    int fracScaled = 0;
    if (parts.length == 2 && parts[1].isNotEmpty) {
      final String fracRaw = parts[1];
      if (!RegExp(r'^\d+$').hasMatch(fracRaw)) {
        throw FormatException('Phần thập phân không hợp lệ: "$raw"');
      }
      if (fracRaw.length <= scaleDigits) {
        final String padded = fracRaw.padRight(scaleDigits, '0');
        fracScaled = int.parse(padded);
      } else {
        // Làm tròn HALF_UP nếu chuỗi đầu vào có hơn 4 chữ số thập phân
        final String firstFour = fracRaw.substring(0, scaleDigits);
        final int fifthDigit = int.parse(fracRaw[scaleDigits]);
        fracScaled = int.parse(firstFour) + (fifthDigit >= 5 ? 1 : 0);
      }
    }

    final int combined = wholePart * scaleFactor + fracScaled;
    return Decimal10k.fromScaled(isNegative ? -combined : combined);
  }

  bool get isZero => scaledValue == 0;
  bool get isNegative => scaledValue < 0;
  bool get isPositive => scaledValue > 0;

  Decimal10k abs() =>
      Decimal10k.fromScaled(scaledValue < 0 ? -scaledValue : scaledValue);

  Decimal10k operator -() => Decimal10k.fromScaled(-scaledValue);

  Decimal10k operator +(Decimal10k other) =>
      Decimal10k.fromScaled(scaledValue + other.scaledValue);

  Decimal10k operator -(Decimal10k other) =>
      Decimal10k.fromScaled(scaledValue - other.scaledValue);

  /// Nhân một số tiền `Decimal10k` với một tỷ giá / hệ số `Decimal10k` khác,
  /// làm tròn `HALF_UP` về đúng 4 chữ số thập phân.
  Decimal10k multiplyRate(Decimal10k rate) {
    final BigInt product =
        BigInt.from(scaledValue) * BigInt.from(rate.scaledValue);
    final BigInt divisor = BigInt.from(scaleFactor);
    final BigInt half = BigInt.from(scaleFactor ~/ 2);
    final BigInt adjusted =
        product >= BigInt.zero ? product + half : product - half;
    return Decimal10k.fromScaled((adjusted ~/ divisor).toInt());
  }

  /// Tính tỷ lệ phần trăm nguyên từ `0..10000` (basis points, ví dụ `6250` = `62.50%`)
  /// mà không dùng `double`.
  int percentageBasisPointsOf(Decimal10k total) {
    if (total.scaledValue <= 0) return 0;
    final BigInt num = BigInt.from(scaledValue.abs()) * BigInt.from(10000);
    final BigInt den = BigInt.from(total.scaledValue.abs());
    final BigInt half = den ~/ BigInt.two;
    return ((num + half) ~/ den).toInt();
  }

  /// Xuất ra chuỗi thập phân chuẩn hợp đồng JSON của Backend (không có `.0000` thừa).
  String toJsonString() {
    if (scaledValue == 0) return '0';
    final bool neg = scaledValue < 0;
    final int absVal = scaledValue.abs();
    final int whole = absVal ~/ scaleFactor;
    final int frac = absVal % scaleFactor;
    final String sign = neg ? '-' : '';
    if (frac == 0) {
      return '$sign$whole';
    }
    final String fracStr =
        frac.toString().padLeft(scaleDigits, '0').replaceFirst(RegExp(r'0+$'), '');
    return '$sign$whole.$fracStr';
  }

  /// Định dạng hiển thị tiền tệ tiếng Việt (có hỗ trợ chế độ ẩn số tiền `••••••`).
  String formatDisplay({
    String currency = 'VND',
    bool masked = false,
    bool showPlusSign = false,
  }) {
    final String symbol = switch (currency.toUpperCase()) {
      'VND' => '₫',
      'USD' => r'$',
      'EUR' => '€',
      _ => currency.toUpperCase(),
    };
    if (masked) {
      return '•••••• $symbol';
    }

    final bool neg = scaledValue < 0;
    final int absVal = scaledValue.abs();
    final int whole = absVal ~/ scaleFactor;
    final int frac = absVal % scaleFactor;

    final String wholeFormatted = _formatThousandsDot(whole);
    final String sign = neg
        ? '-'
        : (showPlusSign && scaledValue > 0)
            ? '+'
            : '';

    if (frac == 0) {
      return '$sign$wholeFormatted $symbol';
    }
    final String fracTrimmed =
        frac.toString().padLeft(scaleDigits, '0').replaceFirst(RegExp(r'0+$'), '');
    return '$sign$wholeFormatted,$fracTrimmed $symbol';
  }

  static String _formatThousandsDot(int number) {
    final String raw = number.toString();
    final StringBuffer buffer = StringBuffer();
    for (int i = 0; i < raw.length; i++) {
      if (i > 0 && (raw.length - i) % 3 == 0) {
        buffer.write('.');
      }
      buffer.write(raw[i]);
    }
    return buffer.toString();
  }

  @override
  int compareTo(Decimal10k other) => scaledValue.compareTo(other.scaledValue);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Decimal10k && other.scaledValue == scaledValue);

  @override
  int get hashCode => scaledValue.hashCode;

  @override
  String toString() => toJsonString();
}

