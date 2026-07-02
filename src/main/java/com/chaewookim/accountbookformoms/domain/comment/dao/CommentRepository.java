package com.chaewookim.accountbookformoms.domain.comment.dao;

import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(
            Long referenceId, ReferenceType referenceType);

    Page<Comment> findByReferenceIdAndReferenceTypeAndParentIdIsNullOrderByCreatedAtAsc(
            Long referenceId, ReferenceType referenceType, Pageable pageable);

    List<Comment> findByParentIdInOrderByCreatedAtAsc(java.util.Collection<Long> parentIds);

    Page<Comment> findByReferenceType(ReferenceType referenceType, Pageable pageable);

    Page<Comment> findByReferenceIdAndReferenceType(
            Long referenceId, ReferenceType referenceType, Pageable pageable);
}
