import math

def srgb_to_linear(c):
    s = c / 255.0
    return s / 12.92 if s <= 0.04045 else math.pow((s + 0.055) / 1.055, 2.4)

def relative_luminance(hex_str):
    h = hex_str.lstrip('#')
    r = int(h[0:2], 16)
    g = int(h[2:4], 16)
    b = int(h[4:6], 16)
    return 0.2126 * srgb_to_linear(r) + 0.7152 * srgb_to_linear(g) + 0.0722 * srgb_to_linear(b)

def contrast_ratio(hex1, hex2):
    l1 = relative_luminance(hex1)
    l2 = relative_luminance(hex2)
    lighter = max(l1, l2)
    darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)

print("=== WCAG 2.x Contrast Verification Script (Python) ===\n")
print("--- 1. Kiểm chứng các ca bị lệch trong v1 do người dùng chỉ ra ---")
tests = [
    ("A: hổ phách cũ trên nền kem", "#C8681A", "#F8F7F3", 4.5),
    ("A: hổ phách cũ trên nền trắng", "#C8681A", "#FFFFFF", 4.5),
    ("A: chữ mờ cũ trên nền kem", "#707C76", "#F8F7F3", 4.5),
    ("B: san hô cũ trên sương sớm", "#F43F5E", "#F4F6FB", 4.5),
    ("B: xanh lam cũ trên sương sớm", "#0284C7", "#F4F6FB", 4.5),
    ("Chữ trắng trên nút hổ phách cũ", "#FFFFFF", "#C8681A", 4.5),
]

for name, fg, bg, target in tests:
    r = contrast_ratio(fg, bg)
    status = "ĐẠT" if r >= target else "KHÔNG ĐẠT"
    print(f"{name}: {fg} vs {bg} -> {r:.2f}:1 (Chuẩn: {target}:1, Kết quả: {status})")

# Full table of refined colors
refined_pairs = [
    # HƯỚNG A - LIGHT
    ("A. Botanical", "Light", "Chữ chính trên Nền kem", "#141A17", "Mực than sẫm", "#F8F7F3", "Kem mỹ thuật", 4.5),
    ("A. Botanical", "Light", "Chữ chính trên Thẻ trắng", "#141A17", "Mực than sẫm", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Chữ phụ trên Thẻ trắng", "#3D4843", "Xám rêu vừa", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Chữ phụ trên Nền kem", "#3D4843", "Xám rêu vừa", "#F8F7F3", "Kem mỹ thuật", 4.5),
    ("A. Botanical", "Light", "Chữ mờ trên Thẻ trắng", "#505C56", "Xám mờ đạt chuẩn", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Xanh rừng (Task) trên Nền kem", "#135E44", "Xanh thông rừng", "#F8F7F3", "Kem mỹ thuật", 4.5),
    ("A. Botanical", "Light", "Hổ phách chữ (Habit) trên Nền kem", "#8A3C01", "Hổ phách đậm (chữ)", "#F8F7F3", "Kem mỹ thuật", 4.5),
    ("A. Botanical", "Light", "Nút điểm danh: Chữ tối trên Nền cam", "#141A17", "Mực than sẫm", "#E58E26", "Cam hổ phách nút", 4.5),
    ("A. Botanical", "Light", "Nút đã điểm danh: Chữ xanh trên Thẻ trắng", "#135E44", "Xanh thông rừng", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Mục tiêu (Tím hoàng gia) trên Thẻ trắng", "#7E22CE", "Tím hoàng gia", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Lịch (Chàm sâu) trên Thẻ trắng", "#3730A3", "Chàm sâu", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Đỏ chi tiêu trên Thẻ trắng", "#B91C1C", "Đỏ gạch chuẩn", "#FFFFFF", "Trắng bề mặt", 4.5),
    ("A. Botanical", "Light", "Xanh thu nhập trên Thẻ trắng", "#15803D", "Xanh ngọc lục bảo", "#FFFFFF", "Trắng bề mặt", 4.5),

    # HƯỚNG A - DARK
    ("A. Botanical", "Dark", "Chữ chính trên Nền tối", "#EDF2EE", "Trắng ngọc", "#0E1311", "Đá phiến ngọc", 4.5),
    ("A. Botanical", "Dark", "Chữ chính trên Thẻ tối", "#EDF2EE", "Trắng ngọc", "#161E1A", "Nhung sẫm", 4.5),
    ("A. Botanical", "Dark", "Chữ phụ trên Thẻ tối", "#9BA9A1", "Xám bạc rêu", "#161E1A", "Nhung sẫm", 4.5),
    ("A. Botanical", "Dark", "Chữ mờ trên Thẻ tối", "#7E8F87", "Xám rêu mờ chuẩn", "#161E1A", "Nhung sẫm", 4.5),
    ("A. Botanical", "Dark", "Ngọc bích (Task) trên Thẻ tối", "#34D399", "Ngọc bích sáng", "#161E1A", "Nhung sẫm", 4.5),
    ("A. Botanical", "Dark", "Vàng hổ phách (Habit) trên Thẻ tối", "#FBBF24", "Vàng hổ phách", "#161E1A", "Nhung sẫm", 4.5),
    ("A. Botanical", "Dark", "Mục tiêu (Tím sáng) trên Thẻ tối", "#C084FC", "Tím sáng", "#161E1A", "Nhung sẫm", 4.5),
    ("A. Botanical", "Dark", "Lịch (Chàm sáng) trên Thẻ tối", "#818CF8", "Chàm sáng", "#161E1A", "Nhung sẫm", 4.5),

    # HƯỚNG B - LIGHT
    ("B. Aurora", "Light", "Chữ chính trên Nền sương lam", "#0B132B", "Xanh đêm sâu", "#F4F6FB", "Sương sớm lam", 4.5),
    ("B. Aurora", "Light", "Chữ phụ trên Thẻ trắng", "#334155", "Xám đá lam", "#FFFFFF", "Trắng tinh thể", 4.5),
    ("B. Aurora", "Light", "Lam ngọc (Cyan) trên Nền sương lam", "#0369A1", "Lam ngọc đậm", "#F4F6FB", "Sương sớm lam", 4.5),
    ("B. Aurora", "Light", "San hô (Coral) trên Nền sương lam", "#BE123C", "San hô đậm", "#F4F6FB", "Sương sớm lam", 4.5),
    ("B. Aurora", "Light", "Mục tiêu (Tím cực quang) trên Thẻ trắng", "#6D28D9", "Tím cực quang", "#FFFFFF", "Trắng tinh thể", 4.5),

    # HƯỚNG B - DARK
    ("B. Aurora", "Dark", "Chữ chính trên Thẻ đêm", "#F8FAFC", "Trắng ánh lam", "#131B2E", "Bề mặt tinh thể", 4.5),
    ("B. Aurora", "Dark", "Chữ phụ trên Thẻ đêm", "#94A3B8", "Xám bạc lam", "#131B2E", "Bề mặt tinh thể", 4.5),
    ("B. Aurora", "Dark", "Cyan Neon trên Thẻ đêm", "#38BDF8", "Cyan Neon", "#131B2E", "Bề mặt tinh thể", 4.5),
    ("B. Aurora", "Dark", "San hô Neon trên Thẻ đêm", "#FB7185", "San hô Neon", "#131B2E", "Bề mặt tinh thể", 4.5),
    ("B. Aurora", "Dark", "Tím Neon trên Thẻ đêm", "#A78BFA", "Tím Neon", "#131B2E", "Bề mặt tinh thể", 4.5),

    # HƯỚNG C - LIGHT
    ("C. Ivory & Ink", "Light", "Mực carbon trên Nền ngà voi", "#111111", "Mực carbon", "#FAF9F6", "Ngà voi", 4.5),
    ("C. Ivory & Ink", "Light", "Chữ phụ trên Thẻ trắng", "#444444", "Xám chì đậm", "#FFFFFF", "Trắng tinh khiết", 4.5),
    ("C. Ivory & Ink", "Light", "Ngọc lục bảo Thụy Sĩ trên Nền ngà", "#0F5132", "Ngọc lục bảo Thụy Sĩ", "#FAF9F6", "Ngà voi", 4.5),
    ("C. Ivory & Ink", "Light", "Mục tiêu (Thạch anh) trên Thẻ trắng", "#581C87", "Thạch anh sẫm", "#FFFFFF", "Trắng tinh khiết", 4.5),

    # HƯỚNG C - DARK
    ("C. Ivory & Ink", "Dark", "Chữ chính trên Thẻ than", "#F5F5F5", "Trắng mực in", "#1E1E1E", "Bề mặt than", 4.5),
    ("C. Ivory & Ink", "Dark", "Chữ phụ trên Thẻ than", "#A3A3A3", "Xám bạc", "#1E1E1E", "Bề mặt than", 4.5),
    ("C. Ivory & Ink", "Dark", "Ngọc lục bảo sáng trên Thẻ than", "#4EAE7C", "Ngọc lục bảo sáng", "#1E1E1E", "Bề mặt than", 4.5),
    ("C. Ivory & Ink", "Dark", "Mục tiêu sáng trên Thẻ than", "#C084FC", "Thạch anh sáng", "#1E1E1E", "Bề mặt than", 4.5),
]

print("\n--- 2. Bảng Tỷ lệ Tương phản Toàn diện v2 (Tính bằng Script) ---")
print("| Hướng | Chế độ | Thành phần UI | Màu chữ/thành phần (FG) | Màu nền (BG) | Tỷ lệ (Script) | Ngưỡng WCAG AA | Kết quả |")
print("|---|---|---|---|---|:---:|:---:|:---:|")

for direction, mode, component, fg, fg_name, bg, bg_name, target in refined_pairs:
    r = contrast_ratio(fg, bg)
    status = "✅ ĐẠT" if r >= target else "❌ KHÔNG ĐẠT"
    print(f"| {direction} | {mode} | {component} | `{fg}` ({fg_name}) | `{bg}` ({bg_name}) | **{r:.2f}:1** | $\\ge {target:.1f}:1$ | {status} |")
