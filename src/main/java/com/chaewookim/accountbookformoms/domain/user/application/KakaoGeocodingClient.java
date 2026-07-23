package com.chaewookim.accountbookformoms.domain.user.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class KakaoGeocodingClient {

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String SEARCH_PATH = "/v2/local/search/address.json";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoGeocodingClient(@Value("${kakao.rest-api-key:}") String restApiKey) {
        this.restApiKey = restApiKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(defaultFactory())
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory defaultFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return factory;
    }

    /**
     * 주소 문자열을 위경도로 변환. 실패/미매칭 시 {@link Optional#empty()} 반환.
     * 실패해도 예외를 던지지 않음 — 회원가입/프로필 수정 흐름을 깨지 않기 위함.
     */
    public Optional<Coordinates> geocode(String rawAddress) {
        if (restApiKey == null || restApiKey.isBlank()) {
            log.warn("Kakao REST API key가 설정되지 않아 지오코딩을 건너뜁니다.");
            return Optional.empty();
        }
        if (rawAddress == null) return Optional.empty();

        String query = rawAddress.replaceFirst("^\\[\\d+\\]\\s*", "").trim();
        if (query.isEmpty()) return Optional.empty();

        try {
            KakaoAddressSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(SEARCH_PATH)
                            .queryParam("query", query)
                            .queryParam("size", 1)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(KakaoAddressSearchResponse.class);

            if (response == null || response.documents() == null || response.documents().isEmpty()) {
                log.info("Kakao 지오코딩 결과 없음 - query: {}", query);
                return Optional.empty();
            }

            KakaoAddressDocument top = response.documents().get(0);
            double longitude = Double.parseDouble(top.x());
            double latitude = Double.parseDouble(top.y());
            return Optional.of(new Coordinates(latitude, longitude));
        } catch (Exception e) {
            log.warn("Kakao 지오코딩 호출 실패 - query: {}, message: {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    public record Coordinates(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAddressSearchResponse(List<KakaoAddressDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAddressDocument(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("x") String x,
            @JsonProperty("y") String y
    ) {
    }
}
