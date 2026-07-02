package com.chaewookim.accountbookformoms.domain.bookmark.dao;

import com.chaewookim.accountbookformoms.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId);

    List<Bookmark> findByUserId(Long userId);

    List<Bookmark> findByUserIdAndBoardIdIn(Long userId, Collection<Long> boardIds);
}
