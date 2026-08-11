package com.raota.integration.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.account.infrastructure.config.EndpointAccessPolicy;
import com.raota.support.BaseIntegrationTest;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class ApiAccessPolicyInventoryTest extends BaseIntegrationTest {

    private static final int EXPECTED_APPLICATION_ENDPOINT_COUNT = 81;
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^/]+}");

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void 모든_애플리케이션_엔드포인트는_접근_등급이_명시되어_있다() {
        List<Endpoint> endpoints = applicationEndpoints();

        assertThat(endpoints).hasSize(EXPECTED_APPLICATION_ENDPOINT_COUNT);
        assertThat(endpoints).allSatisfy(endpoint -> {
            Set<EndpointAccessPolicy.AccessLevel> accessLevels = matchingAccessLevels(endpoint);

            assertThat(accessLevels)
                    .withFailMessage("접근 정책을 하나만 명시해야 합니다. endpoint=%s, matches=%s", endpoint, accessLevels)
                    .hasSize(1);
        });
    }

    @Test
    void 동적_경로는_숫자_ID만_기존_접근_정책에_포함한다() {
        assertThat(matchingAccessLevels("GET", "/community/posts/drafts")).isEmpty();
        assertThat(matchingAccessLevels("GET", "/community/posts/drafts/comments")).isEmpty();
        assertThat(matchingAccessLevels("POST", "/community/posts/drafts/views")).isEmpty();
        assertThat(matchingAccessLevels("GET", "/ramen-shops/internal")).isEmpty();
        assertThat(matchingAccessLevels("GET", "/ramen-shops/internal/menus")).isEmpty();
        assertThat(matchingAccessLevels("GET", "/ramen-logs/moderation")).isEmpty();

        assertThat(matchingAccessLevels("GET", "/community/posts/1"))
                .containsExactly(EndpointAccessPolicy.AccessLevel.PUBLIC);
        assertThat(matchingAccessLevels("GET", "/ramen-shops/1"))
                .containsExactly(EndpointAccessPolicy.AccessLevel.PUBLIC);
        assertThat(matchingAccessLevels("GET", "/ramen-logs/1"))
                .containsExactly(EndpointAccessPolicy.AccessLevel.PUBLIC);
    }

    @Test
    void Springdoc_YAML_명세도_공개한다() {
        assertThat(matchingAccessLevels("GET", "/v3/api-docs.yaml"))
                .containsExactly(EndpointAccessPolicy.AccessLevel.PUBLIC);
    }

    @Test
    void 공개_GET_경로는_암묵적인_HEAD_요청도_허용한다() {
        assertThat(matchingAccessLevels("HEAD", "/"))
                .containsExactly(EndpointAccessPolicy.AccessLevel.PUBLIC);
        assertThat(matchingAccessLevels("HEAD", "/community/posts/1"))
                .containsExactly(EndpointAccessPolicy.AccessLevel.PUBLIC);
        assertThat(matchingAccessLevels("HEAD", "/actuator/health"))
                .contains(EndpointAccessPolicy.AccessLevel.PUBLIC);
        assertThat(matchingAccessLevels("HEAD", "/security-policy-unclassified")).isEmpty();
    }

    private List<Endpoint> applicationEndpoints() {
        return requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> isApplicationController(entry.getValue()))
                .flatMap(entry -> endpoints(entry.getKey()).stream())
                .sorted(Comparator.comparing(Endpoint::path).thenComparing(Endpoint::method))
                .toList();
    }

    private boolean isApplicationController(HandlerMethod handlerMethod) {
        String packageName = handlerMethod.getBeanType().getPackageName();
        return packageName.startsWith("com.raota.");
    }

    private List<Endpoint> endpoints(RequestMappingInfo requestMappingInfo) {
        Set<RequestMethod> methods = requestMappingInfo.getMethodsCondition().getMethods();
        Set<String> paths = requestMappingInfo.getPathPatternsCondition().getPatternValues();

        assertThat(methods).as("HTTP 메서드가 없는 매핑: %s", requestMappingInfo).isNotEmpty();
        assertThat(paths).as("경로가 없는 매핑: %s", requestMappingInfo).isNotEmpty();

        return paths.stream()
                .flatMap(path -> methods.stream().map(method -> new Endpoint(method.name(), path)))
                .toList();
    }

    private Set<EndpointAccessPolicy.AccessLevel> matchingAccessLevels(Endpoint endpoint) {
        String examplePath = PATH_VARIABLE.matcher(endpoint.path()).replaceAll("1");
        return matchingAccessLevels(endpoint.method(), examplePath);
    }

    private Set<EndpointAccessPolicy.AccessLevel> matchingAccessLevels(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return EndpointAccessPolicy.matchingAccessLevels(request);
    }

    private record Endpoint(String method, String path) {
        private Endpoint {
            HttpMethod.valueOf(method);
        }
    }
}
