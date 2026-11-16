package com.lifeos.support;

import com.lifeos.notification.channel.PushSender;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * PushSender giả lập cho test (thay FCM, PHASE_4 NTF-05/NTF-06): ghi lại thông điệp, trả kết quả lập trình theo token,
 * và ghi nhận nếu bị gọi BÊN TRONG transaction DB (vi phạm D137).
 */
public class RecordingPushSender implements PushSender {

    private final List<PushMessage> sent = new CopyOnWriteArrayList<>();
    private final Map<String, Outcome> outcomes = new ConcurrentHashMap<>();
    private final List<String> calledInsideTransaction = new CopyOnWriteArrayList<>();
    private final Map<String, java.util.function.IntConsumer> hooks = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> calls = new ConcurrentHashMap<>();

    @Override
    public PushResult send(PushMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            calledInsideTransaction.add(message.platform());
        }
        java.util.function.IntConsumer hook = hooks.get(message.token());
        if (hook != null) {
            // Cho test đồng thời (F3): chặn/đánh hỏng lần gọi thứ n của một token; ngoại lệ = lỗi mạng tạm thời.
            hook.accept(calls.computeIfAbsent(message.token(), t -> new java.util.concurrent.atomic.AtomicInteger())
                    .incrementAndGet());
        }
        Outcome outcome = outcomes.getOrDefault(message.token(), Outcome.SENT);
        if (outcome == Outcome.SENT) {
            sent.add(message);
            return PushResult.sent();
        }
        return new PushResult(outcome, "TEST_" + outcome);
    }

    public void respond(String token, Outcome outcome) {
        outcomes.put(token, outcome);
    }

    /** Gọi {@code hook(n)} ngay đầu lần gửi thứ n (từ 1) tới token này, trước khi ghi nhận kết quả. */
    public void onSend(String token, java.util.function.IntConsumer hook) {
        hooks.put(token, hook);
    }

    public List<PushMessage> sentTo(String token) {
        return sent.stream().filter(m -> m.token().equals(token)).toList();
    }

    public List<String> calledInsideTransaction() {
        return List.copyOf(calledInsideTransaction);
    }
}
