package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseJoinResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupPurchaseControllerTest extends ControllerTestSupport {

    private UserPrincipal userPrincipal;
    private GroupPurchaseResponse mockResponse;

    @BeforeEach
    void setUp() {
        User user = User.forTestBuilder()
                .id(1L)
                .email("user@test.com")
                .username("테스트유저")
                .role(UserRole.ROLE_USER)
                .build();
        userPrincipal = UserPrincipal.create(user);

        mockResponse = new GroupPurchaseResponse(
                101L, 1L, "테스트유저", 3L, "맛있는 밀키트",
                "상세 설명", 15000, 3, 5, 2,
                PurchaseStatus.RECRUITING, LocalDateTime.now().plusDays(5),
                "마포역 1번출구", 0, "http://image.com/test.jpg", 40.0,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("공동구매 개설 성공")
    void createGroupPurchase_success() throws Exception {
        // given
        GroupPurchaseCreateRequest request = new GroupPurchaseCreateRequest(
                3L, "맛있는 밀키트", "상세 설명", 15000, 3, 5,
                LocalDateTime.now().plusDays(5), "마포역 1번출구", "http://image.com/test.jpg"
        );
        given(groupPurchaseService.createGroupPurchase(any(), any(GroupPurchaseCreateRequest.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchases")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101L))
                .andExpect(jsonPath("$.data.title").value("맛있는 밀키트"));
    }

    @Test
    @DisplayName("공동구매 단건 조회 성공")
    void getOneGroupPurchase_success() throws Exception {
        // given
        given(groupPurchaseService.getGroupPurchase(101L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/group-purchases/101")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101L))
                .andExpect(jsonPath("$.data.title").value("맛있는 밀키트"));
    }

    @Test
    @DisplayName("공동구매 목록 조회 성공")
    void getAllGroupPurchases_success() throws Exception {
        // given
        given(groupPurchaseService.getAllGroupPurchases(any(), any(), any(), any(), any()))
                .willReturn(List.of(mockResponse));

        // when & then
        mockMvc.perform(get("/api/v1/group-purchases")
                        .with(user(userPrincipal))
                        .param("sortBy", "latest")
                        .param("nearMe", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(101L));
    }

    @Test
    @DisplayName("공동구매 수정 성공")
    void updateGroupPurchase_success() throws Exception {
        // given
        GroupPurchaseUpdateRequest request = new GroupPurchaseUpdateRequest(
                3L, "수정된 밀키트", "수정된 설명", 16000, 3, 5,
                LocalDateTime.now().plusDays(5), "공덕역 2번출구", "http://image.com/update.jpg"
        );
        GroupPurchaseResponse updatedResponse = new GroupPurchaseResponse(
                101L, 1L, "테스트유저", 3L, "수정된 밀키트",
                "수정된 설명", 16000, 3, 5, 2,
                PurchaseStatus.RECRUITING, LocalDateTime.now().plusDays(5),
                "공덕역 2번출구", 0, "http://image.com/update.jpg", 40.0,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(groupPurchaseService.updateGroupPurchase(any(), any(), any(GroupPurchaseUpdateRequest.class)))
                .willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/v1/group-purchases/101")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 밀키트"));
    }

    @Test
    @DisplayName("공동구매 삭제 성공")
    void deleteGroupPurchase_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/group-purchases/101")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("공동구매가 성공적으로 삭제되었습니다."));
    }

    @Test
    @DisplayName("공동구매 찜하기 토글 성공")
    void toggleWish_success() throws Exception {
        // given
        given(groupPurchaseService.toggleWish(any(), any())).willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchases/101/wish")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("찜한 공동구매 목록 페이징 조회 성공")
    void getWishes_success() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<GroupPurchaseResponse> page = new PageImpl<>(List.of(mockResponse), pageable, 1);
        given(groupPurchaseService.getWishedGroupPurchases(any(), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/group-purchases/wishes")
                        .with(user(userPrincipal))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101L));
    }

    @Test
    @DisplayName("공동구매 참여 성공")
    void joinGroupPurchase_success() throws Exception {
        // given
        GroupPurchaseJoinResponse joinResponse = new GroupPurchaseJoinResponse(mockResponse, false, BigDecimal.valueOf(20000));
        given(groupPurchaseService.joinGroupPurchase(any(), any())).willReturn(joinResponse);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchases/101/join")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.budgetWarning").value(false))
                .andExpect(jsonPath("$.data.groupPurchase.id").value(101L));
    }

    @Test
    @DisplayName("공동구매 참여 취소 성공")
    void leaveGroupPurchase_success() throws Exception {
        // given
        given(groupPurchaseService.leaveGroupPurchase(any(), any())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/group-purchases/101/leave")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101L));
    }
}
