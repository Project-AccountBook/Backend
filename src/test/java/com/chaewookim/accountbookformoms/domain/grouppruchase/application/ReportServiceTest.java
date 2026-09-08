package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.ReportRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Report;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportProcessResult;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ReportCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ReportProcessRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.ReportResponse;
import com.chaewookim.accountbookformoms.domain.notification.application.NotificationService;
import com.chaewookim.accountbookformoms.domain.notification.enums.NotificationType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private GroupPurchaseRepository groupPurchaseRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportService reportService;

    private static final Long REPORTER_ID = 1L;
    private static final Long TARGET_ID = 10L;
    private static final Long REPORT_ID = 100L;
    private static final Long CREATOR_ID = 2L;

    @Test
    @DisplayName("신고 등록 성공 — 공동구매 글")
    void createReport_groupPurchase_success() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.GROUP_PURCHASE, TARGET_ID, "부적절한 게시글입니다.");
        given(groupPurchaseRepository.existsById(TARGET_ID)).willReturn(true);

        Report report = Report.builder()
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.GROUP_PURCHASE)
                .targetId(TARGET_ID)
                .reason(request.reason())
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        given(reportRepository.save(any(Report.class))).willReturn(report);

        // when
        Long savedId = reportService.createReport(REPORTER_ID, request);

        // then
        assertThat(savedId).isEqualTo(REPORT_ID);
        verify(groupPurchaseRepository).existsById(TARGET_ID);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("신고 등록 실패 — 존재하지 않는 공동구매 글인 경우")
    void createReport_groupPurchase_fail_not_found() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.GROUP_PURCHASE, TARGET_ID, "부적절한 게시글입니다.");
        given(groupPurchaseRepository.existsById(TARGET_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reportService.createReport(REPORTER_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REPORT_TARGET);
    }

    @Test
    @DisplayName("신고 목록 조회 성공")
    void getReports_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Report report = Report.builder()
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.GROUP_PURCHASE)
                .targetId(TARGET_ID)
                .reason("이유")
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now());

        Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 1);
        given(reportRepository.findReports(eq(false), eq(ReportTargetType.GROUP_PURCHASE), eq(pageable)))
                .willReturn(reportPage);

        // when
        Page<ReportResponse> result = reportService.getReports(false, ReportTargetType.GROUP_PURCHASE, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(REPORT_ID);
        assertThat(result.getContent().get(0).reporterId()).isEqualTo(REPORTER_ID);
    }

    @Test
    @DisplayName("신고 처리 성공 — 경고 조치 및 알림 발송")
    void processReport_warn_success() {
        // given
        ReportProcessRequest request = new ReportProcessRequest(ReportProcessResult.WARNED, "경고 사유");
        Report report = Report.builder()
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.GROUP_PURCHASE)
                .targetId(TARGET_ID)
                .reason("신고 사유")
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);

        GroupPurchase gp = GroupPurchase.builder()
                .id(TARGET_ID)
                .creatorId(CREATOR_ID)
                .title("공구 게시글 제목")
                .build();

        User creator = User.forTestBuilder()
                .id(CREATOR_ID)
                .email("creator@test.com")
                .username("작성자")
                .build();

        given(reportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(groupPurchaseRepository.findById(TARGET_ID)).willReturn(Optional.of(gp));
        given(userRepository.findById(CREATOR_ID)).willReturn(Optional.of(creator));

        // when
        Long processedId = reportService.processReport(REPORT_ID, request);

        // then
        assertThat(processedId).isEqualTo(REPORT_ID);
        assertThat(report.isProcessed()).isTrue();
        assertThat(report.getProcessResult()).isEqualTo(ReportProcessResult.WARNED);
        assertThat(report.getProcessReason()).isEqualTo("경고 사유");

        verify(notificationService).createNotification(
                eq(creator),
                eq(NotificationType.SYSTEM),
                eq("공동구매 글 경고 알림"),
                contains("경고 사유"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("신고 처리 성공 — 공동구매 삭제 조치, 소프트 삭제 및 알림 발송")
    void processReport_delete_groupPurchase_success() {
        // given
        ReportProcessRequest request = new ReportProcessRequest(ReportProcessResult.DELETED, "규정 위반 삭제");
        Report report = Report.builder()
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.GROUP_PURCHASE)
                .targetId(TARGET_ID)
                .reason("신고 사유")
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);

        GroupPurchase gp = GroupPurchase.builder()
                .id(TARGET_ID)
                .creatorId(CREATOR_ID)
                .title("공구 게시글 제목")
                .build();

        User creator = User.forTestBuilder()
                .id(CREATOR_ID)
                .email("creator@test.com")
                .username("작성자")
                .build();

        given(reportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(groupPurchaseRepository.findById(TARGET_ID)).willReturn(Optional.of(gp));
        given(userRepository.findById(CREATOR_ID)).willReturn(Optional.of(creator));

        // when
        Long processedId = reportService.processReport(REPORT_ID, request);

        // then
        assertThat(processedId).isEqualTo(REPORT_ID);
        assertThat(report.isProcessed()).isTrue();
        assertThat(report.getProcessResult()).isEqualTo(ReportProcessResult.DELETED);

        verify(groupPurchaseRepository).delete(gp);
        verify(notificationService).createNotification(
                eq(creator),
                eq(NotificationType.SYSTEM),
                eq("공동구매 글 삭제 알림"),
                contains("규정 위반 삭제"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("신고 처리 성공 — 댓글 삭제 조치, markAsAdminDeleted, 소프트 삭제 및 알림 발송")
    void processReport_delete_comment_success() {
        // given
        ReportProcessRequest request = new ReportProcessRequest(ReportProcessResult.DELETED, "욕설로 인한 삭제");
        Report report = Report.builder()
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.COMMENT)
                .targetId(TARGET_ID)
                .reason("신고 사유")
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);

        Comment comment = Comment.builder()
                .userId(CREATOR_ID)
                .referenceId(101L)
                .referenceType(ReferenceType.GROUPPURCHASE)
                .content("나쁜 댓글 내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", TARGET_ID);

        User author = User.forTestBuilder()
                .id(CREATOR_ID)
                .email("author@test.com")
                .username("작성자")
                .build();

        given(reportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
        given(commentRepository.findById(TARGET_ID)).willReturn(Optional.of(comment));
        given(userRepository.findById(CREATOR_ID)).willReturn(Optional.of(author));

        // when
        Long processedId = reportService.processReport(REPORT_ID, request);

        // then
        assertThat(processedId).isEqualTo(REPORT_ID);
        assertThat(report.isProcessed()).isTrue();
        assertThat(report.getProcessResult()).isEqualTo(ReportProcessResult.DELETED);
        assertThat(comment.isAdminDeleted()).isTrue();

        verify(commentRepository).delete(comment);
        verify(notificationService).createNotification(
                eq(author),
                eq(NotificationType.SYSTEM),
                eq("댓글 삭제 알림"),
                contains("욕설로 인한 삭제"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("신고 처리 실패 — 이미 처리 완료된 신고인 경우")
    void processReport_fail_already_processed() {
        // given
        ReportProcessRequest request = new ReportProcessRequest(ReportProcessResult.WARNED, "중복 처리 테스트");
        Report report = Report.builder()
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.GROUP_PURCHASE)
                .targetId(TARGET_ID)
                .reason("신고 사유")
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        // 이미 처리 완료된 상태로 강제 설정
        report.process(ReportProcessResult.DISMISSED, "기존 처리 완료");

        given(reportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));

        // when & then
        assertThatThrownBy(() -> reportService.processReport(REPORT_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_ALREADY_PROCESSED);
    }
}
