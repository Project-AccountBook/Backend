package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseAdminResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminGroupPurchaseControllerTest extends ControllerTestSupport {

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;

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
    }

    @Test
    @DisplayName("어드민 요약 정보 조회 성공 — ROLE_ADMIN 권한 소유")
    void getSummary_admin_success() throws Exception {
        // given
        GroupPurchaseDashboardResponse response = new GroupPurchaseDashboardResponse(
                5L, 10L, 3L, 2L, 0L, 60.0, 40.0, 0.0
        );
        given(groupPurchaseService.getDashboardSummary()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/admin/group-purchases/summary")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todayCreatedCount").value(5L))
                .andExpect(jsonPath("$.data.recruitingRatio").value(60.0));
    }

    @Test
    @DisplayName("어드민 요약 정보 조회 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void getSummary_user_forbidden() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/admin/group-purchases/summary")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("어드민용 전체 공구 목록 모니터링 조회 성공 — ROLE_ADMIN 권한 소유")
    void getGroupPurchases_admin_success() throws Exception {
        // given
        GroupPurchaseAdminResponse mockAdminResponse = new GroupPurchaseAdminResponse(
                101L, 2L, "개설자닉네임", 3L, "식료품", "맛있는 밀키트 공구",
                15000, 3, 5, 2, PurchaseStatus.RECRUITING,
                LocalDateTime.now().plusDays(2), LocalDateTime.now(), 0L
        );
        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<GroupPurchaseAdminResponse> page = new PageImpl<>(List.of(mockAdminResponse), pageable, 1);

        given(groupPurchaseService.getGroupPurchasesForAdmin(eq("RECRUITING"), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/admin/group-purchases")
                        .with(user(adminPrincipal))
                        .param("status", "RECRUITING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101L))
                .andExpect(jsonPath("$.data.content[0].creatorUsername").value("개설자닉네임"));
    }

    @Test
    @DisplayName("어드민용 전체 공구 목록 모니터링 조회 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void getGroupPurchases_user_forbidden() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/admin/group-purchases")
                        .with(user(userPrincipal))
                        .param("status", "RECRUITING"))
                .andExpect(status().isForbidden());
    }
}
