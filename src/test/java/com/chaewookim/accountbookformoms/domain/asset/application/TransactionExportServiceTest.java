package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chaewookim.accountbookformoms.global.error.CustomException;

@ExtendWith(MockitoExtension.class)
class TransactionExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionExportService transactionExportService;

    @Test
    @DisplayName("거래 내역 엑셀 파일 생성 - 성공")
    void exportToExcel_Success() throws Exception {

        // given
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 30);

        Transaction transaction = Transaction.builder()
                .transactionDate(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10000"))
                .build();

        given(transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate)).willReturn(List.of(transaction));

        // when
        Resource resource = transactionExportService.exportToCsv(userId, startDate, endDate);

        // then
        assertThat(resource).isNotNull();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resource.getInputStream().readAllBytes()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("거래 내역");
            assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("기간 요약");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("MODI 거래 내역");
        }
    }

    @Test
    @DisplayName("거래 내역 엑셀 내보내기 - 시작일이 종료일보다 늦으면 실패")
    void exportToExcel_InvalidDateRange() {
        assertThatThrownBy(() -> transactionExportService.exportToCsv(
                1L,
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 6, 1)
        )).isInstanceOf(CustomException.class);

        verify(transactionRepository, never()).findByUserIdAndDateBetween(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("거래 내역 엑셀 내보내기 - 12개월 초과 기간이면 실패")
    void exportToExcel_PeriodTooLong() {
        assertThatThrownBy(() -> transactionExportService.exportToCsv(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 2)
        )).isInstanceOf(CustomException.class);

        verify(transactionRepository, never()).findByUserIdAndDateBetween(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}