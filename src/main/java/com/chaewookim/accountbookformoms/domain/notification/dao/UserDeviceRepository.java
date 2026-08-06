package com.chaewookim.accountbookformoms.domain.notification.dao;

import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserId(Long userId);
}
