package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCategoryCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCategoryUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseCategoryResponse;
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

class GroupPurchaseCategoryControllerTest extends ControllerTestSupport {

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private GroupPurchaseCategoryResponse mockResponse;

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

        mockResponse = new GroupPurchaseCategoryResponse(3L, "식료품", 1, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("카테고리 등록 성공 — ROLE_ADMIN 권한 소유")
    void createCategory_admin_success() throws Exception {
        // given
        GroupPurchaseCategoryCreateRequest request = new GroupPurchaseCategoryCreateRequest("식료품", 1);
        given(groupPurchaseCategoryService.createCategory(any(GroupPurchaseCategoryCreateRequest.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchase-categories")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3L))
                .andExpect(jsonPath("$.data.name").value("식료품"));
    }

    @Test
    @DisplayName("카테고리 등록 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void createCategory_user_forbidden() throws Exception {
        // given
        GroupPurchaseCategoryCreateRequest request = new GroupPurchaseCategoryCreateRequest("식료품", 1);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchase-categories")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카테고리 단건 조회 성공")
    void getOneCategory_success() throws Exception {
        // given
        given(groupPurchaseCategoryService.getCategory(3L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/group-purchase-categories/3")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3L));
    }

    @Test
    @DisplayName("카테고리 전체 조회 성공")
    void getAllCategories_success() throws Exception {
        // given
        given(groupPurchaseCategoryService.getAllCategories()).willReturn(List.of(mockResponse));

        // when & then
        mockMvc.perform(get("/api/v1/group-purchase-categories")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(3L));
    }

    @Test
    @DisplayName("카테고리 수정 성공 — ROLE_ADMIN 권한 소유")
    void updateCategory_admin_success() throws Exception {
        // given
        GroupPurchaseCategoryUpdateRequest request = new GroupPurchaseCategoryUpdateRequest("수정된 식료품", 2);
        GroupPurchaseCategoryResponse updatedResponse = new GroupPurchaseCategoryResponse(3L, "수정된 식료품", 2, LocalDateTime.now(), LocalDateTime.now());
        given(groupPurchaseCategoryService.updateCategory(eq(3L), any(GroupPurchaseCategoryUpdateRequest.class))).willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/v1/group-purchase-categories/3")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("수정된 식료품"))
                .andExpect(jsonPath("$.data.sortOrder").value(2));
    }

    @Test
    @DisplayName("카테고리 수정 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void updateCategory_user_forbidden() throws Exception {
        // given
        GroupPurchaseCategoryUpdateRequest request = new GroupPurchaseCategoryUpdateRequest("수정된 식료품", 2);

        // when & then
        mockMvc.perform(put("/api/v1/group-purchase-categories/3")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카테고리 삭제 성공 — ROLE_ADMIN 권한 소유")
    void deleteCategory_admin_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/group-purchase-categories/3")
                        .with(csrf())
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("카테고리가 성공적으로 삭제되었습니다."));
    }

    @Test
    @DisplayName("카테고리 삭제 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void deleteCategory_user_forbidden() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/group-purchase-categories/3")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }
}
