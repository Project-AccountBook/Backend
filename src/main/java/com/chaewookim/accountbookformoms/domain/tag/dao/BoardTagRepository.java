package com.chaewookim.accountbookformoms.domain.tag.dao;

import com.chaewookim.accountbookformoms.domain.tag.entity.BoardTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BoardTagRepository extends JpaRepository<BoardTag, Long> {

    List<BoardTag> findByBoardId(Long boardId);

    List<BoardTag> findByBoardIdIn(Collection<Long> boardIds);

    void deleteByBoardId(Long boardId);

    @Query("SELECT bt.boardId FROM BoardTag bt WHERE bt.tagId = :tagId")
    List<Long> findBoardIdsByTagId(@Param("tagId") Long tagId);
}
