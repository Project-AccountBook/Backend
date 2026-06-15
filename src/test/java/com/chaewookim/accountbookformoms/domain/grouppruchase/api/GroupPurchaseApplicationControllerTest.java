package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ApplicationStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseApplicationCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseApplicationResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupPurchaseApplicationControllerTest extends ControllerTestSupport {

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private GroupPurchaseApplicationResponse mockResponse;

    @BeforeEach
    void setUp() {
        User admin = User.forTestBuilder()
                .id(1L)
                .email("admin@test.com")
                .role(UserRole.ROLE_ADMIN)
                .build();
        adminPrincipal = UserPrincipal.create(admin);

        User user = User.forTestBuilder()
                .id(2L)
                .email("user@test.com")
                .role(UserRole.ROLE_USER)
                .build();
        userPrincipal = UserPrincipal.create(user);

        mockResponse = new GroupPurchaseApplicationResponse(
                501L, 2L, "신청 제목", "신청 내용", "http://product.url",
                10000, ApplicationStatus.PENDING, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("공동구매 신청서 등록 성공")
    void createApplication_success() throws Exception {
        // given
        GroupPurchaseApplicationCreateRequest request = new GroupPurchaseApplicationCreateRequest(
                "신청 제목", "신청 내용", "http://product.url", 10000
        );
        given(groupPurchaseApplicationService.createApplication(any(), any(GroupPurchaseApplicationCreateRequest.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchase-applications")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501L))
                .andExpect(jsonPath("$.data.title").value("신청 제목"));
    }

    @Test
    @DisplayName("본인의 공동구매 신청서 목록 조회 성공")
    void getMyApplications_success() throws Exception {
        // given
        given(groupPurchaseApplicationService.getUserApplications(any())).willReturn(List.of(mockResponse));

        // when & then
        mockMvc.perform(get("/api/v1/group-purchase-applications/me")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(501L));
    }

    @Test
    @DisplayName("공동구매 신청서 상세 조회 성공")
    void getApplicationDetail_success() throws Exception {
        // given
        given(groupPurchaseApplicationService.getApplication(any(), any())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/group-purchase-applications/501")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(501L));
    }

    @Test
    @DisplayName("어드민 공동구매 신청서 상태 수정 성공 — ROLE_ADMIN 권한 소유")
    void updateApplicationStatus_admin_success() throws Exception {
        // given
        GroupPurchaseApplicationResponse approvedResponse = new GroupPurchaseApplicationResponse(
                501L, 2L, "신청 제목", "신청 내용", "http://product.url",
                10000, ApplicationStatus.APPROVED, "승인 피드백", LocalDateTime.now(), LocalDateTime.now()
        );
        given(groupPurchaseApplicationService.updateApplicationStatus(eq(501L), eq(ApplicationStatus.APPROVED), eq("승인 피드백")))
                .willReturn(approvedResponse);

        // when & then
        mockMvc.perform(patch("/api/v1/group-purchase-applications/501/status")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .param("status", "APPROVED")
                        .param("feedback", "승인 피드백"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.adminFeedback").value("승인 피드백"));
    }

    @Test
    @DisplayName("어드민 공동구매 신청서 상태 수정 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void updateApplicationStatus_user_forbidden() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/group-purchase-applications/501/status")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .param("status", "APPROVED")
                        .param("feedback", "승인 피드백"))
                .andExpect(status().isForbidden());
    }
}
