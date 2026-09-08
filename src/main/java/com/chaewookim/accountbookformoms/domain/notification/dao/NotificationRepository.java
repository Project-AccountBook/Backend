package com.chaewookim.accountbookformoms.domain.notification.dao;

import com.chaewookim.accountbookformoms.domain.notification.entity.Notification;
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

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE n.user.id = :userId AND n.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Long userId);
}
