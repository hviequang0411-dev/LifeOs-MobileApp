package com.lifeos.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletRequest;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Giới hạn body 1 MB (SECURITY_RULES §9.3, D55). Phần kiểm tra đầu-cuối qua HTTP (SEC-10) nằm ở bước 1e. */
class RequestSizeLimitFilterTest {

    private static final int MAX = 1_048_576;

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(MAX);

    @Test
    void declaredBodyOverLimitIsRejectedWith413BeforeReading() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContent(new byte[MAX + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("\"errorCode\":\"PAYLOAD_TOO_LARGE\"");
        assertThat(chain.getRequest()).as("không chuyển tiếp request").isNull();
    }

    @Test
    void bodyAtLimitPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContent(new byte[MAX]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void undeclaredBodyIsCountedWhileReading() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login") {
            @Override
            public long getContentLengthLong() {
                return -1; // chunked: không có Content-Length
            }
        };
        request.setContent(new byte[MAX + 10]);
        AtomicReference<ServletRequest> passed = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passed.set(req));

        InputStream in = passed.get().getInputStream();
        byte[] buffer = new byte[8192];
        assertThatThrownBy(() -> {
            while (in.read(buffer) != -1) {
                // đọc đến khi vượt giới hạn
            }
        }).isInstanceOf(RequestSizeLimitFilter.PayloadTooLargeException.class);
    }

    @Test
    void undeclaredSmallBodyIsReadNormally() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent("{\"a\":1}".getBytes());
        AtomicReference<ServletRequest> passed = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passed.set(req));

        assertThat(new String(passed.get().getInputStream().readAllBytes())).isEqualTo("{\"a\":1}");
    }
}
