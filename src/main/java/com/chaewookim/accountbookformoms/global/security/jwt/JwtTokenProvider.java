package com.chaewookim.accountbookformoms.global.security.jwt;

import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 30;            // 30분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 14; // 14일

    private final Key key;

    // application.yml에서 secret 값 가져와서 키 생성
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성
    public String createAccessToken(String email, String role) {
        return createToken(email, role, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // Refresh Token 생성
    public String createRefreshToken(String email) {
        return createToken(email, null, REFRESH_TOKEN_EXPIRE_TIME);
    }

    // 공통 토큰 빌더
    private String createToken(String email, String role, long validityTime) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityTime);

        JwtBuilder builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256);

        // Access Token일 때만 권한 클레임 추가
        if (role != null) {
            builder.claim(AUTHORITIES_KEY, role);
        }

        return builder.compact();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // 토큰에서 인증 정보(Authentication) 꺼내기
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        if (claims.get(AUTHORITIES_KEY) == null) {
            log.warn("권한 정보가 없는 토큰으로 인증을 시도했습니다. 이메일: {}", claims.getSubject());
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        String email = claims.getSubject();
        String authClaim = claims.get(AUTHORITIES_KEY) != null ? claims.get(AUTHORITIES_KEY).toString() : "";

        UserRole userRole = UserRole.valueOf(authClaim);
        UserDetails userDetails = new UserPrincipal(null, email, "", userRole);

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // 토큰 파싱 (내부용)
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 토큰에서 이메일(Subject) 추출
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }
}
