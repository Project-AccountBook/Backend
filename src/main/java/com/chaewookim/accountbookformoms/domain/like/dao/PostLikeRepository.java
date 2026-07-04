package com.chaewookim.accountbookformoms.domain.like.dao;

import com.chaewookim.accountbookformoms.domain.like.entity.PostLike;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, LikeTargetType targetType);

    long countByTargetIdAndTargetType(Long targetId, LikeTargetType targetType);

    @Query("SELECT l.targetId AS targetId, COUNT(l) AS cnt " +
            "FROM PostLike l " +
            "WHERE l.targetType = :type AND l.targetId IN :ids " +
            "GROUP BY l.targetId")
    List<TargetCount> countByTargets(@Param("type") LikeTargetType type, @Param("ids") Collection<Long> ids);

    @Query("SELECT l.targetId FROM PostLike l " +
            "WHERE l.userId = :userId AND l.targetType = :type AND l.targetId IN :ids")
    List<Long> findLikedTargets(@Param("userId") Long userId,
                                @Param("type") LikeTargetType type,
                                @Param("ids") Collection<Long> ids);

    interface TargetCount {
        Long getTargetId();
        Long getCnt();
    }
}
