package com.chaewookim.accountbookformoms.domain.notification.dao;

import com.chaewookim.accountbookformoms.domain.notification.entity.UserDevice;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUser(User user);
}
