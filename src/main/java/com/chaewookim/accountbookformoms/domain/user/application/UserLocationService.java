package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LocationUpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.LocationResponse;
import com.chaewookim.accountbookformoms.domain.user.dto.response.NearbyUserResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLocationService {

    public static final String USER_GEO_KEY = "user:geo";

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public LocationResponse updateLocation(Long userId, LocationUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateLocation(request.latitude(), request.longitude());

        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        geoOps.add(USER_GEO_KEY, new Point(request.longitude(), request.latitude()), String.valueOf(userId));

        log.info("Redis GEO에 사용자 위치 저장 - userId: {}, lat: {}, lon: {}", userId, request.latitude(), request.longitude());
        return new LocationResponse(userId, request.latitude(), request.longitude());
    }

    public LocationResponse getMyLocation(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        List<Point> positions = redisTemplate.opsForGeo().position(USER_GEO_KEY, String.valueOf(userId));
        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            throw new CustomException(UserErrorCode.LOCATION_NOT_REGISTERED);
        }

        Point point = positions.get(0);
        return new LocationResponse(userId, point.getY(), point.getX());
    }

    public List<NearbyUserResponse> findNearbyUsers(Long userId, double radiusKm) {

        if (!userRepository.existsById(userId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
                USER_GEO_KEY,
                String.valueOf(userId),
                distance,
                args
        );

        if (results == null) {
            return Collections.emptyList();
        }

        String me = String.valueOf(userId);
        return results.getContent().stream()
                .filter(r -> !me.equals(r.getContent().getName()))
                .map(this::toNearbyUserResponse)
                .collect(Collectors.toList());
    }

    private NearbyUserResponse toNearbyUserResponse(GeoResult<RedisGeoCommands.GeoLocation<String>> result) {
        RedisGeoCommands.GeoLocation<String> location = result.getContent();
        Point point = location.getPoint();
        return new NearbyUserResponse(
                Long.valueOf(location.getName()),
                point.getY(),
                point.getX(),
                result.getDistance().getValue()
        );
    }
}
