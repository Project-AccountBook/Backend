package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final GroupPurchaseRepository groupPurchaseRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Long createReport(Long reporterId, ReportCreateRequest request) {
        validateReportTarget(request.targetType(), request.targetId());

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .build();

        return reportRepository.save(report).getId();
    }

    public Page<ReportResponse> getReports(Boolean isProcessed, ReportTargetType targetType, Pageable pageable) {
        return reportRepository.findReports(isProcessed, targetType, pageable)
                .map(ReportResponse::from);
    }

    @Transactional
    public Long processReport(Long reportId, ReportProcessRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (report.isProcessed()) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        // 신고 처리 사유 기록 및 상태 변경
        report.process(request.action(), request.reason());

        // 조치 사항에 따른 엔티티 삭제 및 알림 발송 처리
        if (request.action() == ReportProcessResult.WARNED || request.action() == ReportProcessResult.DELETED) {
            handleActionOnTarget(report, request.action(), request.reason());
        }

        return report.getId();
    }

    private void validateReportTarget(ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.GROUP_PURCHASE) {
            if (!groupPurchaseRepository.existsById(targetId)) {
                throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
            }
        } else if (targetType == ReportTargetType.COMMENT) {
            if (!commentRepository.existsById(targetId)) {
                throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
            }
        } else {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }
    }

    private void handleActionOnTarget(Report report, ReportProcessResult action, String reason) {
        if (report.getTargetType() == ReportTargetType.GROUP_PURCHASE) {
            GroupPurchase gp = groupPurchaseRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REPORT_TARGET));

            User creator = userRepository.findById(gp.getCreatorId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String title;
            String message;

            if (action == ReportProcessResult.DELETED) {
                title = "공동구매 글 삭제 알림";
                message = String.format("[공동구매 글 삭제] 귀하가 개설한 공동구매 글('%s')이 규정 위반으로 인해 삭제되었습니다. 사유: %s", gp.getTitle(), reason);
                groupPurchaseRepository.delete(gp);
            } else {
                title = "공동구매 글 경고 알림";
                message = String.format("[공동구매 글 경고] 귀하가 개설한 공동구매 글('%s')에 대해 신고가 접수되어 경고 조치되었습니다. 사유: %s", gp.getTitle(), reason);
            }

            notificationService.createNotification(creator, NotificationType.SYSTEM, title, message, null, null);

        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            Comment comment = commentRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REPORT_TARGET));

            User author = userRepository.findById(comment.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String title;
            String message;

            if (action == ReportProcessResult.DELETED) {
                title = "댓글 삭제 알림";
                message = String.format("[댓글 삭제] 귀하가 작성한 댓글('%s')이 규정 위반으로 인해 삭제되었습니다. 사유: %s", getContentPreview(comment.getContent()), reason);
                comment.markAsAdminDeleted();
                commentRepository.delete(comment);
            } else {
                title = "댓글 경고 알림";
                message = String.format("[댓글 경고] 귀하가 작성한 댓글('%s')에 대해 신고가 접수되어 경고 조치되었습니다. 사유: %s", getContentPreview(comment.getContent()), reason);
            }

            notificationService.createNotification(author, NotificationType.SYSTEM, title, message, null, null);
        }
    }

    private String getContentPreview(String content) {
        if (content == null) return "";
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
    }
}
