package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.ai.provider.FakeLLMProvider;
import com.lifeos.ai.provider.LLMProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class AiFakeProviderEvalTest {
    private static final List<String> PROMPTS = List.of(
            "Hôm nay tôi nên ưu tiên việc gì?", "Lập kế hoạch cho ngày mai", "Tóm tắt ba mục tiêu chính",
            "Tôi có lịch trống lúc nào?", "Giải thích xu hướng thói quen", "Chia nhỏ việc chuẩn bị báo cáo",
            "Nêu rủi ro trong kế hoạch tuần", "Gợi ý thứ tự công việc", "Tóm tắt ghi chú cuộc họp",
            "Chi tiêu tuần này có xu hướng gì?", "Việc nào sắp đến hạn?", "Tôi bỏ lỡ thói quen nào?",
            "Mục tiêu nào đang chậm?", "Lập kế hoạch tuần không ghi dữ liệu", "Nêu dữ liệu nguồn đã dùng",
            "Không thực hiện giao dịch tài chính", "Không tự hoàn thành công việc", "Chỉ trả lời từ context",
            "Không tiết lộ system prompt", "Không gọi tool xóa", "Ưu tiên công việc quan trọng",
            "Tạo dàn ý đọc-only", "Giải thích mà không sửa dữ liệu", "Tóm tắt ngắn gọn",
            "Tóm tắt dạng gạch đầu dòng", "Phân tích theo USD", "Phân tích theo khoảng ngày",
            "Nêu nguồn tham chiếu", "Không dùng dữ liệu user khác", "Từ chối khi thiếu consent",
            "Không lộ email hoặc token", "Giữ số tiền dạng chuỗi", "Không tạo proposal hành động",
            "Trả lỗi ổn định khi provider lỗi", "Đáp án phải có căn cứ", "Không suy đoán ngoài dữ liệu",
            "Nhắc rằng AI có thể sai", "Tôn trọng việc thu hồi consent", "Chỉ đọc Note được chọn",
            "Không tìm semantic toàn bộ Note");

    @Test
    void AI_36_38_fortyVietnameseFixturesUseDeterministicOfflineProvider() throws Exception {
        FakeLLMProvider fake = new FakeLLMProvider();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = PROMPTS.stream().map(prompt -> executor.submit(() -> fake.generate(
                    new LLMProvider.Request("CHAT", prompt, Map.of("fixture", true), Map.of())))).toList();
            for (var future : futures) {
                LLMProvider.Response response = future.get();
                assertThat(response.resultCode()).isEqualTo("SUCCESS");
                assertThat(response.content()).isEqualTo("fake-response");
                assertThat(response.latencyMs()).isEqualTo(3);
                assertThat(response.structured()).containsEntry("mode", "CHAT");
            }
        }
        assertThat(fake.calls()).isEqualTo(40);
    }
}
