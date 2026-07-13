package com.chaewookim.accountbookformoms.domain.notification.dao;

import com.chaewookim.accountbookformoms.domain.notification.entity.Notification;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUser(User user, Pageable pageable);

    Long countByUserAndIsReadFalse(User user);

    boolean existsByUser_IdAndTypeAndReferenceId(Long userId, NotificationType type, Long referenceId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
