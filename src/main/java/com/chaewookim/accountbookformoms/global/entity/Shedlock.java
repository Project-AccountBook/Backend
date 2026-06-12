package com.chaewookim.accountbookformoms.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ShedLock 의 락 상태 저장용 테이블. JPA 엔티티는 오직 ddl-auto: update 가 테이블을 만들기 위한 용도.
 * <p>애플리케이션 코드는 이 엔티티를 참조하지 않으며, ShedLock 라이브러리가 JdbcTemplate 으로 직접 다룬다.</p>
 * <p>운영 환경(ddl-auto: validate) 에서는 DDL 을 수동 적용해야 한다:</p>
 * <pre>
 * CREATE TABLE shedlock(
 *     name       VARCHAR(64)  NOT NULL,
 *     lock_until TIMESTAMP(3) NOT NULL,
 *     locked_at  TIMESTAMP(3) NOT NULL,
 *     locked_by  VARCHAR(255) NOT NULL,
 *     PRIMARY KEY (name)
 * );
 * </pre>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "shedlock")
public class Shedlock {

    @Id
    @Column(length = 64)
    private String name;

    @Column(name = "lock_until", nullable = false)
    private LocalDateTime lockUntil;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;
}
