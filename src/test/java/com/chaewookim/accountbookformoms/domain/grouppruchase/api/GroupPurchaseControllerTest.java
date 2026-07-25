package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseParticipantRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GroupPurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupPurchaseCategoryRepository categoryRepository;

    @Autowired
    private GroupPurchaseRepository groupPurchaseRepository;

    @Autowired
    private GroupPurchaseParticipantRepository participantRepository;

    private User savedUser;
    private UserPrincipal userPrincipal;
    private Category savedCategory;
    private GroupPurchase savedGroupPurchase;

    @BeforeEach
    void setUp() {
        // 실제 데이터 세팅 (외래 키 충돌 방지를 위해 고유한 이메일 사용)
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        User user = User.builder()
                .email("test_" + uniqueSuffix + "@test.com")
                .password("1234")
                .username("테스트유저")
                .role(UserRole.ROLE_USER)
                .build();
        savedUser = userRepository.save(user);
        userPrincipal = UserPrincipal.create(savedUser);

        Category category = Category.builder().name("테스트카테고리_" + uniqueSuffix).build();
        savedCategory = categoryRepository.save(category);

        GroupPurchase gp = GroupPurchase.builder()
                .creatorId(savedUser.getId())
                .categoryId(savedCategory.getId())
                .title("테스트 공동구매")
                .content("상세 설명")
                .price(15000)
                .minParticipants(3)
                .maxParticipants(5)
                .deadline(LocalDateTime.now().plusDays(5))
                .pickupLocation("테스트 장소")
                .build();
        savedGroupPurchase = groupPurchaseRepository.save(gp);
    }

    @AfterEach
    void tearDown() {
        // FK 제약 조건 때문에 일괄 삭제 생략 또는 필요한 부분만 정리
    }

    @Test
    @DisplayName("공동구매 개설 성공")
    void createGroupPurchase_success() throws Exception {
        GroupPurchaseCreateRequest request = new GroupPurchaseCreateRequest(
                savedCategory.getId(), "새로운 공구", "새로운 설명", 20000, 2, 10,
                LocalDateTime.now().plusDays(5), "새로운 장소", "http://image.com/test.jpg"
        );

        mockMvc.perform(post("/api/v1/group-purchases")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("새로운 공구"));
    }

    @Test
    @DisplayName("공동구매 단건 조회 성공")
    void getOneGroupPurchase_success() throws Exception {
        mockMvc.perform(get("/api/v1/group-purchases/" + savedGroupPurchase.getId())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(savedGroupPurchase.getId()))
                .andExpect(jsonPath("$.data.title").value("테스트 공동구매"));
    }

    @Test
    @DisplayName("공동구매 목록 조회 성공")
    void getAllGroupPurchases_success() throws Exception {
        mockMvc.perform(get("/api/v1/group-purchases")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("공동구매 수정 성공")
    void updateGroupPurchase_success() throws Exception {
        GroupPurchaseUpdateRequest request = new GroupPurchaseUpdateRequest(
                savedCategory.getId(), "수정된 공구", "수정된 설명", 18000, 4, 8,
                LocalDateTime.now().plusDays(7), "수정된 장소", "http://image.com/updated.jpg"
        );

        mockMvc.perform(put("/api/v1/group-purchases/" + savedGroupPurchase.getId())
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 공구"));
    }

    @Test
    @DisplayName("공동구매 삭제 성공")
    void deleteGroupPurchase_success() throws Exception {
        mockMvc.perform(delete("/api/v1/group-purchases/" + savedGroupPurchase.getId())
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("공동구매 찜하기 토글 성공")
    void toggleWish_success() throws Exception {
        mockMvc.perform(post("/api/v1/group-purchases/" + savedGroupPurchase.getId() + "/wish")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true)); // 처음 누르면 true
    }

    @Test
    @DisplayName("찜한 공동구매 목록 페이징 조회 성공")
    void getWishes_success() throws Exception {
        // 미리 찜하기
        mockMvc.perform(post("/api/v1/group-purchases/" + savedGroupPurchase.getId() + "/wish")
                        .with(csrf())
                        .with(user(userPrincipal)));

        mockMvc.perform(get("/api/v1/group-purchases/wishes")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("공동구매 참여 성공")
    void joinGroupPurchase_success() throws Exception {
        mockMvc.perform(post("/api/v1/group-purchases/" + savedGroupPurchase.getId() + "/join")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupPurchase.id").value(savedGroupPurchase.getId()));
    }

    @Test
    @DisplayName("공동구매 참여 취소 성공")
    void leaveGroupPurchase_success() throws Exception {
        // 미리 참여
        mockMvc.perform(post("/api/v1/group-purchases/" + savedGroupPurchase.getId() + "/join")
                        .with(csrf())
                        .with(user(userPrincipal)));

        // 취소
        mockMvc.perform(post("/api/v1/group-purchases/" + savedGroupPurchase.getId() + "/leave")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
