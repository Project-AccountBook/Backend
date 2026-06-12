package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LocationUpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.LocationResponse;
import com.chaewookim.accountbookformoms.domain.user.dto.response.NearbyUserResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserLocationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @InjectMocks
    private UserLocationService userLocationService;

    @Test
    @DisplayName("위치 등록 - 성공: DB 갱신 및 Redis GEO 저장")
    void updateLocation_success() {

        // given
        Long userId = 1L;
        LocationUpdateRequest request = new LocationUpdateRequest(37.5665, 126.9780);
        User user = User.forTestBuilder().id(userId).email("test@email.com").username("user").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);

        // when
        LocationResponse response = userLocationService.updateLocation(userId, request);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.latitude()).isEqualTo(37.5665);
        assertThat(response.longitude()).isEqualTo(126.9780);
        assertThat(user.getLatitude()).isEqualTo(37.5665);
        assertThat(user.getLongitude()).isEqualTo(126.9780);
        verify(geoOperations).add(eq(UserLocationService.USER_GEO_KEY), any(Point.class), eq("1"));
    }

    @Test
    @DisplayName("위치 등록 - 사용자 없음 예외")
    void updateLocation_fail_user_not_found() {

        // given
        LocationUpdateRequest request = new LocationUpdateRequest(37.5665, 126.9780);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userLocationService.updateLocation(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 위치 조회 - 성공")
    void getMyLocation_success() {

        // given
        Long userId = 1L;
        given(userRepository.existsById(userId)).willReturn(true);
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);
        given(geoOperations.position(UserLocationService.USER_GEO_KEY, "1"))
                .willReturn(List.of(new Point(126.9780, 37.5665)));

        // when
        LocationResponse response = userLocationService.getMyLocation(userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.latitude()).isEqualTo(37.5665);
        assertThat(response.longitude()).isEqualTo(126.9780);
    }

    @Test
    @DisplayName("내 위치 조회 - 사용자 없음 예외")
    void getMyLocation_fail_user_not_found() {

        // given
        given(userRepository.existsById(1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userLocationService.getMyLocation(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 위치 조회 - 등록된 위치 없음 예외")
    void getMyLocation_fail_location_not_registered() {

        // given
        given(userRepository.existsById(1L)).willReturn(true);
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);
        given(geoOperations.position(UserLocationService.USER_GEO_KEY, "1"))
                .willReturn(Collections.singletonList(null));

        // when & then
        assertThatThrownBy(() -> userLocationService.getMyLocation(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.LOCATION_NOT_REGISTERED);
    }

    @Test
    @DisplayName("근처 사용자 조회 - 성공: 본인 제외")
    @SuppressWarnings("unchecked")
    void findNearbyUsers_success() {

        // given
        Long userId = 1L;
        given(userRepository.existsById(userId)).willReturn(true);
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);

        RedisGeoCommands.GeoLocation<String> self = new RedisGeoCommands.GeoLocation<>("1", new Point(126.9780, 37.5665));
        RedisGeoCommands.GeoLocation<String> other = new RedisGeoCommands.GeoLocation<>("2", new Point(126.9790, 37.5670));

        GeoResult<RedisGeoCommands.GeoLocation<String>> selfResult = mock(GeoResult.class);
        GeoResult<RedisGeoCommands.GeoLocation<String>> otherResult = mock(GeoResult.class);
        given(selfResult.getContent()).willReturn(self);
        given(selfResult.getDistance()).willReturn(new Distance(0.0, Metrics.KILOMETERS));
        given(otherResult.getContent()).willReturn(other);
        given(otherResult.getDistance()).willReturn(new Distance(0.12, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                new GeoResults<>(List.of(selfResult, otherResult));

        given(geoOperations.radius(eq(UserLocationService.USER_GEO_KEY), eq("1"), any(Distance.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .willReturn(results);

        // when
        List<NearbyUserResponse> nearby = userLocationService.findNearbyUsers(userId, 3.0);

        // then
        assertThat(nearby).hasSize(1);
        assertThat(nearby.get(0).userId()).isEqualTo(2L);
        assertThat(nearby.get(0).latitude()).isEqualTo(37.5670);
        assertThat(nearby.get(0).longitude()).isEqualTo(126.9790);
        assertThat(nearby.get(0).distanceKm()).isEqualTo(0.12);
    }

    @Test
    @DisplayName("근처 사용자 조회 - 결과 없음 처리")
    void findNearbyUsers_empty_when_radius_returns_null() {

        // given
        given(userRepository.existsById(1L)).willReturn(true);
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);
        given(geoOperations.radius(anyString(), anyString(), any(Distance.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .willReturn(null);

        // when
        List<NearbyUserResponse> nearby = userLocationService.findNearbyUsers(1L, 3.0);

        // then
        assertThat(nearby).isEmpty();
    }

    @Test
    @DisplayName("근처 사용자 조회 - 사용자 없음 예외")
    void findNearbyUsers_fail_user_not_found() {

        // given
        given(userRepository.existsById(1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userLocationService.findNearbyUsers(1L, 3.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("findNearbyUserIds - 반경 내 사용자 ID 집합 반환 (본인 제외)")
    void findNearbyUserIds_success() {

        // given
        Long userId = 1L;
        given(userRepository.existsById(userId)).willReturn(true);
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);
        given(geoOperations.position(UserLocationService.USER_GEO_KEY, "1"))
                .willReturn(List.of(new Point(126.9780, 37.5665)));

        RedisGeoCommands.GeoLocation<String> self = new RedisGeoCommands.GeoLocation<>("1", new Point(126.9780, 37.5665));
        RedisGeoCommands.GeoLocation<String> other2 = new RedisGeoCommands.GeoLocation<>("2", new Point(126.9790, 37.5670));
        RedisGeoCommands.GeoLocation<String> other3 = new RedisGeoCommands.GeoLocation<>("3", new Point(126.9800, 37.5680));

        @SuppressWarnings("unchecked")
        GeoResult<RedisGeoCommands.GeoLocation<String>> r1 = mock(GeoResult.class);
        @SuppressWarnings("unchecked")
        GeoResult<RedisGeoCommands.GeoLocation<String>> r2 = mock(GeoResult.class);
        @SuppressWarnings("unchecked")
        GeoResult<RedisGeoCommands.GeoLocation<String>> r3 = mock(GeoResult.class);
        given(r1.getContent()).willReturn(self);
        given(r1.getDistance()).willReturn(new Distance(0.0, Metrics.KILOMETERS));
        given(r2.getContent()).willReturn(other2);
        given(r2.getDistance()).willReturn(new Distance(0.12, Metrics.KILOMETERS));
        given(r3.getContent()).willReturn(other3);
        given(r3.getDistance()).willReturn(new Distance(0.25, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                new GeoResults<>(List.of(r1, r2, r3));
        given(geoOperations.radius(eq(UserLocationService.USER_GEO_KEY), eq("1"), any(Distance.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .willReturn(results);

        // when
        Set<Long> ids = userLocationService.findNearbyUserIds(userId, 3.0);

        // then
        assertThat(ids).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("findNearbyUserIds - 위치 미등록 사용자 예외")
    void findNearbyUserIds_fail_location_not_registered() {

        // given
        given(userRepository.existsById(1L)).willReturn(true);
        given(redisTemplate.opsForGeo()).willReturn(geoOperations);
        given(geoOperations.position(UserLocationService.USER_GEO_KEY, "1"))
                .willReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> userLocationService.findNearbyUserIds(1L, 3.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.LOCATION_NOT_REGISTERED);
    }

    @Test
    @DisplayName("findNearbyUserIds - 사용자 없음 예외")
    void findNearbyUserIds_fail_user_not_found() {

        // given
        given(userRepository.existsById(1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userLocationService.findNearbyUserIds(1L, 3.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }
}
