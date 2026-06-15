package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportProcessResult;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ReportProcessRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.ReportResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminReportControllerTest extends ControllerTestSupport {

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private ReportResponse mockResponse;

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

        mockResponse = new ReportResponse(
                901L, 2L, ReportTargetType.GROUP_PURCHASE, 101L, "스팸성 광고 글",
                false, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("어드민용 신고 목록 조회 성공 — ROLE_ADMIN 권한 소유")
    void getReports_admin_success() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<ReportResponse> page = new PageImpl<>(List.of(mockResponse), pageable, 1);
        given(reportService.getReports(eq(false), eq(ReportTargetType.GROUP_PURCHASE), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user(adminPrincipal))
                        .param("isProcessed", "false")
                        .param("targetType", "GROUP_PURCHASE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(901L))
                .andExpect(jsonPath("$.data.content[0].reason").value("스팸성 광고 글"));
    }

    @Test
    @DisplayName("어드민용 신고 목록 조회 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void getReports_user_forbidden() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user(userPrincipal))
                        .param("isProcessed", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("어드민용 신고 처리 성공 — ROLE_ADMIN 권한 소유")
    void processReport_admin_success() throws Exception {
        // given
        ReportProcessRequest request = new ReportProcessRequest(ReportProcessResult.WARNED, "경고 조치 취함");
        given(reportService.processReport(eq(901L), any(ReportProcessRequest.class))).willReturn(901L);

        // when & then
        mockMvc.perform(post("/api/v1/admin/reports/901/process")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(901L));
    }

    @Test
    @DisplayName("어드민용 신고 처리 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void processReport_user_forbidden() throws Exception {
        // given
        ReportProcessRequest request = new ReportProcessRequest(ReportProcessResult.WARNED, "경고 조치 취함");

        // when & then
        mockMvc.perform(post("/api/v1/admin/reports/901/process")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
