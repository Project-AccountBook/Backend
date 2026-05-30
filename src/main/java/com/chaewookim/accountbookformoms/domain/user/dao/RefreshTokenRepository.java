package com.chaewookim.accountbookformoms.domain.user.dao;

import com.chaewookim.accountbookformoms.domain.user.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);
}
