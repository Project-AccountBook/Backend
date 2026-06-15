package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ProductCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ProductUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.ProductResponse;
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

class ProductControllerTest extends ControllerTestSupport {

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private ProductResponse mockResponse;

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

        mockResponse = new ProductResponse(
                301L, "맛있는 사과", 9900, "청송 꿀사과 1kg", "http://image.com/apple.jpg",
                3L, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("상품 등록 성공 — ROLE_ADMIN 권한 소유")
    void createProduct_admin_success() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest("맛있는 사과", 9900, "청송 꿀사과 1kg", "http://image.com/apple.jpg", 3L, null, null);
        given(productService.createProduct(any(ProductCreateRequest.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(301L))
                .andExpect(jsonPath("$.data.name").value("맛있는 사과"));
    }

    @Test
    @DisplayName("상품 등록 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void createProduct_user_forbidden() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest("맛있는 사과", 9900, "청송 꿀사과 1kg", "http://image.com/apple.jpg", 3L, null, null);

        // when & then
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void getOneProduct_success() throws Exception {
        // given
        given(productService.getProduct(301L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/products/301")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(301L));
    }

    @Test
    @DisplayName("상품 목록 조회 성공")
    void getAllProducts_success() throws Exception {
        // given
        given(productService.getAllProducts(3L)).willReturn(List.of(mockResponse));

        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .with(user(userPrincipal))
                        .param("categoryId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(301L));
    }

    @Test
    @DisplayName("상품 수정 성공 — ROLE_ADMIN 권한 소유")
    void updateProduct_admin_success() throws Exception {
        // given
        ProductUpdateRequest request = new ProductUpdateRequest("수정된 사과", 18900, "청송 꿀사과 2kg", "http://image.com/apple2.jpg", 3L, null, null);
        ProductResponse updatedResponse = new ProductResponse(
                301L, "수정된 사과", 18900, "청송 꿀사과 2kg", "http://image.com/apple2.jpg",
                3L, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        given(productService.updateProduct(eq(301L), any(ProductUpdateRequest.class))).willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/v1/products/301")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("수정된 사과"));
    }

    @Test
    @DisplayName("상품 수정 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void updateProduct_user_forbidden() throws Exception {
        // given
        ProductUpdateRequest request = new ProductUpdateRequest("수정된 사과", 18900, "청송 꿀사과 2kg", "http://image.com/apple2.jpg", 3L, null, null);

        // when & then
        mockMvc.perform(put("/api/v1/products/301")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("상품 삭제 성공 — ROLE_ADMIN 권한 소유")
    void deleteProduct_admin_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/products/301")
                        .with(csrf())
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("상품이 성공적으로 삭제되었습니다."));
    }

    @Test
    @DisplayName("상품 삭제 실패 — ROLE_USER 권한 소유 (403 Forbidden)")
    void deleteProduct_user_forbidden() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/products/301")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }
}
