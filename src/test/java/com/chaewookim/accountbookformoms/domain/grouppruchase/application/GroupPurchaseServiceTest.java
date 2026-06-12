package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.ReportRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.WishlistRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Wishlist;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseAdminResponse;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupPurchaseServiceTest {

    @Mock
    private GroupPurchaseRepository groupPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupPurchaseCategoryRepository groupPurchaseCategoryRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @InjectMocks
    private GroupPurchaseService groupPurchaseService;

    @Test
    @DisplayName("대시보드 요약 조회 성공 — 정확한 비율 연산 확인")
    void getDashboardSummary_success() {
        // given
        given(groupPurchaseRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(5L); // 오늘 개설된 공구 수 5개

        given(groupPurchaseRepository.sumCurrentParticipantsByStatus(PurchaseStatus.RECRUITING))
                .willReturn(15L); // 실시간 참여 중인 인원 수 15명

        // 상태별 개수 모킹 (총 9개: RECRUITING=3, SUCCESS=2, CLOSED=1, FAILED=3)
        // 진행 비율: 3/9 = 33.33%
        // 성공 비율: (2+1)/9 = 33.33%
        // 무산 비율: 3/9 = 33.33%
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.RECRUITING)).willReturn(3L);
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.SUCCESS)).willReturn(2L);
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.CLOSED)).willReturn(1L);
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.FAILED)).willReturn(3L);

        // when
        GroupPurchaseDashboardResponse response = groupPurchaseService.getDashboardSummary();

        // then
        assertThat(response.todayCreatedCount()).isEqualTo(5L);
        assertThat(response.activeParticipantsCount()).isEqualTo(15L);
        assertThat(response.recruitingCount()).isEqualTo(3L);
        assertThat(response.successCount()).isEqualTo(3L); // SUCCESS(2) + CLOSED(1) = 3
        assertThat(response.failedCount()).isEqualTo(3L);
        assertThat(response.recruitingRatio()).isEqualTo(33.33);
        assertThat(response.successRatio()).isEqualTo(33.33);
        assertThat(response.failedRatio()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("대시보드 요약 조회 성공 — 등록된 공구가 하나도 없을 때 (0 나누기 예방)")
    void getDashboardSummary_empty() {
        // given
        given(groupPurchaseRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(0L);
        given(groupPurchaseRepository.sumCurrentParticipantsByStatus(PurchaseStatus.RECRUITING))
                .willReturn(0L);

        given(groupPurchaseRepository.countByStatus(PurchaseStatus.RECRUITING)).willReturn(0L);
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.SUCCESS)).willReturn(0L);
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.CLOSED)).willReturn(0L);
        given(groupPurchaseRepository.countByStatus(PurchaseStatus.FAILED)).willReturn(0L);

        // when
        GroupPurchaseDashboardResponse response = groupPurchaseService.getDashboardSummary();

        // then
        assertThat(response.todayCreatedCount()).isZero();
        assertThat(response.activeParticipantsCount()).isZero();
        assertThat(response.recruitingRatio()).isZero();
        assertThat(response.successRatio()).isZero();
        assertThat(response.failedRatio()).isZero();
    }

    @Test
    @DisplayName("어드민 모니터링 목록 조회 성공 — DTO 매핑 및 조인 정보 검증")
    void getGroupPurchasesForAdmin_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        GroupPurchase gp = GroupPurchase.builder()
                .id(101L)
                .creatorId(2L)
                .categoryId(3L)
                .title("공구 게시글")
                .build();
        ReflectionTestUtils.setField(gp, "createdAt", LocalDateTime.now());

        Page<GroupPurchase> gpPage = new PageImpl<>(List.of(gp), pageable, 1);
        given(groupPurchaseRepository.findAllForAdmin("RECRUITING", pageable)).willReturn(gpPage);

        User user = User.forTestBuilder()
                .id(2L)
                .username("작성자")
                .build();
        given(userRepository.findById(2L)).willReturn(Optional.of(user));

        Category category = Category.builder()
                .id(3L)
                .name("식료품")
                .build();
        given(groupPurchaseCategoryRepository.findById(3L)).willReturn(Optional.of(category));

        given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.GROUP_PURCHASE, 101L)).willReturn(4L);

        // when
        Page<GroupPurchaseAdminResponse> responsePage = groupPurchaseService.getGroupPurchasesForAdmin("RECRUITING", pageable);

        // then
        assertThat(responsePage.getContent()).hasSize(1);
        GroupPurchaseAdminResponse dto = responsePage.getContent().get(0);
        assertThat(dto.id()).isEqualTo(101L);
        assertThat(dto.creatorUsername()).isEqualTo("작성자");
        assertThat(dto.categoryName()).isEqualTo("식료품");
        assertThat(dto.reportCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("찜 토글 성공 — 찜하기 등록")
    void toggleWish_create_success() {
        // given
        given(groupPurchaseRepository.existsById(101L)).willReturn(true);
        given(wishlistRepository.findByUserIdAndGroupPurchaseId(2L, 101L)).willReturn(Optional.empty());

        // when
        boolean result = groupPurchaseService.toggleWish(2L, 101L);

        // then
        assertThat(result).isTrue();
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("찜 토글 성공 — 찜하기 해제")
    void toggleWish_delete_success() {
        // given
        given(groupPurchaseRepository.existsById(101L)).willReturn(true);
        Wishlist wish = Wishlist.builder().userId(2L).groupPurchaseId(101L).build();
        given(wishlistRepository.findByUserIdAndGroupPurchaseId(2L, 101L)).willReturn(Optional.of(wish));

        // when
        boolean result = groupPurchaseService.toggleWish(2L, 101L);

        // then
        assertThat(result).isFalse();
        verify(wishlistRepository).delete(wish);
    }

    @Test
    @DisplayName("찜 토글 실패 — 존재하지 않는 공동구매 글")
    void toggleWish_fail_not_found() {
        // given
        given(groupPurchaseRepository.existsById(101L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> groupPurchaseService.toggleWish(2L, 101L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_PURCHASE_NOT_FOUND);
    }

    @Test
    @DisplayName("찜한 목록 페이징 조회 성공")
    void getWishedGroupPurchases_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        GroupPurchase gp = GroupPurchase.builder()
                .id(101L)
                .creatorId(2L)
                .categoryId(3L)
                .title("공구 게시글")
                .build();
        ReflectionTestUtils.setField(gp, "createdAt", LocalDateTime.now());

        Page<GroupPurchase> gpPage = new PageImpl<>(List.of(gp), pageable, 1);
        given(groupPurchaseRepository.findWishedGroupPurchases(2L, pageable)).willReturn(gpPage);

        // when
        Page<GroupPurchaseResponse> responsePage = groupPurchaseService.getWishedGroupPurchases(2L, pageable);

        // then
        assertThat(responsePage.getContent()).hasSize(1);
        assertThat(responsePage.getContent().get(0).id()).isEqualTo(101L);
    }

    @Test
    @DisplayName("공동구매 상세 단건 조회 성공 — 조회수 증가, 닉네임 및 참여율 계산 확인")
    void getGroupPurchase_success() {
        // given
        GroupPurchase gp = GroupPurchase.builder()
                .id(101L)
                .creatorId(2L)
                .categoryId(3L)
                .title("공구 상세 정보")
                .minParticipants(5)
                .pickupLocation("서울시 마포구")
                .imageUrl("http://image.com/test.jpg")
                .build();
        ReflectionTestUtils.setField(gp, "currentParticipants", 2); // 2/5 = 40.0%
        ReflectionTestUtils.setField(gp, "viewCount", 0);

        given(groupPurchaseRepository.findById(101L)).willReturn(Optional.of(gp));

        User user = User.forTestBuilder()
                .id(2L)
                .username("개설자닉네임")
                .build();
        given(userRepository.findById(2L)).willReturn(Optional.of(user));

        // when
        GroupPurchaseResponse response = groupPurchaseService.getGroupPurchase(101L);

        // then
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.creatorNickname()).isEqualTo("개설자닉네임");
        assertThat(response.achievementRate()).isEqualTo(40.0);
        assertThat(response.imageUrl()).isEqualTo("http://image.com/test.jpg");
        assertThat(gp.getViewCount()).isEqualTo(1); // 엔티티의 조회수가 1 증가했는지 검증
    }

    @Test
    @DisplayName("공동구매 상세 단건 조회 실패 — 존재하지 않는 공동구매 ID")
    void getGroupPurchase_notFound() {
        // given
        given(groupPurchaseRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> groupPurchaseService.getGroupPurchase(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_PURCHASE_NOT_FOUND);
    }
}
