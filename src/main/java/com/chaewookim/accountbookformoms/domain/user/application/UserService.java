package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.request.SignupRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdatePasswordRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateProfileRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.SignupResponse;
import com.chaewookim.accountbookformoms.domain.user.dto.response.UserProfileResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.entity.UserNotificationSetting;
import com.chaewookim.accountbookformoms.domain.user.entity.UserSetting;
import com.chaewookim.accountbookformoms.domain.user.enums.SocialProvider;
import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserCommonService userCommonService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signUp(SignupRequest request) {

        if (!emailVerificationService.isVerified(request.email(), VerificationType.SIGNUP)) {
            throw new CustomException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }

        String encoded = passwordEncoder.encode(request.password());

        SignupResponse response = userRepository.findByEmailIncludingDeleted(request.email())
                .map(user -> {
                    if (user.getDeletedAt() == null) {
                        throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
                    }
                    userCommonService.restoreUser(user, request.username(), encoded, request.birthDate(), request.address());
                    return new SignupResponse(user.getId(), user.getEmail(), user.getUsername());
                })
                .orElseGet(() -> {
                    User savedUser = userCommonService.saveLocalUser(request.email(), encoded, request.username(), SocialProvider.LOCAL, request.birthDate(), request.address());
                    return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
                });

        emailVerificationService.deleteVerification(request.email(), VerificationType.SIGNUP);
        return response;
    }

    public UserProfileResponse getMyProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        UserSetting settings = user.getUserSetting();
        UserNotificationSetting notificationSetting = user.getUserNotificationSetting();

        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getUsername(), user.getRole().name(),
                user.getBirthDate(), user.getAddress(),
                user.getPassword() != null,
                settings.getBudgetAlertThreshold(), settings.getIsPortfolioPublic(),
                notificationSetting.getIsBudgetAlertEnabled(), notificationSetting.getIsInterestCategoryEnabled(), notificationSetting.getIsSystemAlertEnabled()
        );
    }

    @Transactional
    public void updateMyProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.username(), request.birthDate(), request.address());
        user.getUserSetting().updateSettings(request.budgetAlertThreshold(), request.isPortfolioPublic());
        user.getUserNotificationSetting().updateNotificationSettings(
                request.isBudgetAlertEnabled(),
                request.isInterestCategoryEnabled(),
                request.isSystemAlertEnabled()
        );
        user.updateLastBudgetAlertMonth(null);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (user.getPassword() != null) {
            if (request.currentPassword() == null ||
                    !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new CustomException(UserErrorCode.PASSWORD_NOT_MATCH);
            }
        }

        String newEncodedPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(newEncodedPassword);
    }

    @Transactional
    public void withdraw(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }
}
