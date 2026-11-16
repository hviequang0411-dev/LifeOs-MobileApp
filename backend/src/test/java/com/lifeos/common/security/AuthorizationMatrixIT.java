package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.IntegrationTestBase;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * AUTHZ-01 / AUTHZ-02 (TESTING_RULES §7): quét TOÀN BỘ route từ RequestMappingHandlerMapping, nên route mới tự động
 * nằm trong phạm vi kiểm tra.
 * <ul>
 *   <li>AUTHZ-01: mọi route không thuộc danh sách công khai trả 401 khi không có token, token sai, token hết hạn.</li>
 *   <li>AUTHZ-02: route nào trả khác 401 khi không có token thì phải nằm trong SecurityConfig.PUBLIC_POST_ENDPOINTS;
 *       mọi mục trong danh sách công khai phải ứng với một route POST có thật và không bị chặn.</li>
 * </ul>
 * Context này là context dùng chung của IntegrationTestBase: không có controller nào chỉ dành cho test.
 */
class AuthorizationMatrixIT extends IntegrationTestBase {

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]+}");

    /** Bản đồ route của ứng dụng (Actuator có handler mapping riêng, kiểm ở ActuatorExposureIT / SEC-13). */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    JwtService jwtService;

    private final String ip = randomIp();

    private record Route(HttpMethod method, String pattern) implements Comparable<Route> {
        String concretePath() {
            Matcher m = PATH_VARIABLE.matcher(pattern);
            return m.replaceAll(UUID.randomUUID().toString());
        }

        @Override
        public int compareTo(Route o) {
            return (pattern + method).compareTo(o.pattern + o.method);
        }

        @Override
        public String toString() {
            return method + " " + pattern;
        }
    }

    private List<Route> routes() {
        Set<Route> routes = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            List<HttpMethod> httpMethods = methods.isEmpty()
                    ? List.of(HttpMethod.GET, HttpMethod.POST)
                    : methods.stream().map(m -> HttpMethod.valueOf(m.name())).toList();
            for (String pattern : info.getPatternValues()) {
                for (HttpMethod method : httpMethods) {
                    routes.add(new Route(method, pattern));
                }
            }
        }
        return new ArrayList<>(routes);
    }

    private static boolean isPublic(Route route) {
        return (route.method() == HttpMethod.POST
                && Arrays.asList(SecurityConfig.PUBLIC_POST_ENDPOINTS).contains(route.pattern()))
                || (route.method() == HttpMethod.GET
                && Arrays.asList(SecurityConfig.PUBLIC_GET_ENDPOINTS).contains(route.pattern()));
    }

    private MvcResult call(Route route, String authorization) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(route.method(), route.concretePath())
                .with(fromIp(ip));
        if (route.method() != HttpMethod.GET && route.method() != HttpMethod.DELETE) {
            request.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mvc.perform(request).andReturn();
    }

    @Test
    void routeCatalogIsCompleteAndContainsNoTestOnlyControllers() {
        List<Route> routes = routes();
        assertThat(routes).extracting(Route::pattern)
                .contains("/api/v1/auth/login", "/api/v1/me", "/api/v1/me/settings", "/api/v1/devices",
                        "/api/v1/devices/{id}")
                .noneMatch(p -> p.startsWith("/probe") || p.contains("test-probe"));
    }

    @Test
    void everyNonPublicRouteRejectsMissingInvalidAndExpiredTokens() throws Exception {
        Clock past = Clock.offset(clock, Duration.ofMinutes(-20)).withZone(ZoneOffset.UTC);
        String expired = new JwtService(jwtProperties, past).issue(UUID.randomUUID(), UUID.randomUUID()).token();
        String valid = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();
        String forged = valid.substring(0, valid.lastIndexOf('.') + 1) + "AAAA" + valid.substring(
                valid.lastIndexOf('.') + 5);

        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Route route : routes()) {
            if (isPublic(route)) {
                continue;
            }
            checked++;
            expect(failures, route, "không token", call(route, null), "AUTH_TOKEN_INVALID");
            expect(failures, route, "token rác", call(route, "Bearer not-a-jwt"), "AUTH_TOKEN_INVALID");
            expect(failures, route, "chữ ký sai", call(route, "Bearer " + forged), "AUTH_TOKEN_INVALID");
            expect(failures, route, "hết hạn", call(route, "Bearer " + expired), "AUTH_TOKEN_EXPIRED");
        }
        assertThat(checked).as("số route cần xác thực đã kiểm").isGreaterThanOrEqualTo(8);
        assertThat(failures).as("route không trả 401 như mong đợi").isEmpty();
    }

    private static void expect(List<String> failures, Route route, String label, MvcResult result,
            String errorCode) throws Exception {
        String body = result.getResponse().getContentAsString();
        if (result.getResponse().getStatus() != 401 || !body.contains("\"errorCode\":\"" + errorCode + "\"")) {
            failures.add(route + " [" + label + "] → " + result.getResponse().getStatus() + " " + body);
        }
    }

    @Test
    void onlyDeclaredPublicEndpointsAreReachableWithoutToken() throws Exception {
        List<String> undeclared = new ArrayList<>();
        Set<String> publicPostRoutesFound = new TreeSet<>();
        Set<String> publicGetRoutesFound = new TreeSet<>();
        for (Route route : routes()) {
            int status = call(route, null).getResponse().getStatus();
            if (status != 401) {
                if (isPublic(route) && route.method() == HttpMethod.GET) {
                    publicGetRoutesFound.add(route.pattern());
                } else if (isPublic(route)) {
                    publicPostRoutesFound.add(route.pattern());
                } else {
                    undeclared.add(route + " → " + status);
                }
            }
        }
        assertThat(undeclared).as("route mở không có trong PUBLIC_POST_ENDPOINTS").isEmpty();
        // Mọi mục trong danh sách công khai đều có route POST thật và không bị lớp bảo mật chặn.
        assertThat(publicPostRoutesFound).containsExactlyInAnyOrder(SecurityConfig.PUBLIC_POST_ENDPOINTS);
        assertThat(publicGetRoutesFound).containsExactlyInAnyOrder(SecurityConfig.PUBLIC_GET_ENDPOINTS);
    }
}
