package com.chaewookim.accountbookformoms.domain.image.dao;

import com.chaewookim.accountbookformoms.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByReferenceTypeAndReferenceIdOrderBySortOrderAsc(
            Image.ReferenceType referenceType, Long referenceId);

    List<Image> findByReferenceTypeAndReferenceIdInOrderBySortOrderAsc(
            Image.ReferenceType referenceType, Collection<Long> referenceIds);

    void deleteByReferenceTypeAndReferenceId(Image.ReferenceType referenceType, Long referenceId);
}
