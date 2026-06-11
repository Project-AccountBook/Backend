package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.ReportRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseAdminResponse;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
}
