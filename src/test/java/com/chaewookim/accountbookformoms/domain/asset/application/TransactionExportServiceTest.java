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
import static org.mockito.BDDMockito.given;

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
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("가계부 내역");
            assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("월간 요약");
        }
    }
}