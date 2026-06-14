package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionExportService {

    private final TransactionRepository transactionRepository;

    public Resource exportToCsv(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            // 1. 상세 내역 시트
            Sheet sheet1 = workbook.createSheet("가계부 내역");
            String[] headers = {"날짜", "유형", "카테고리", "금액", "내용"};
            Row headerRow1 = sheet1.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow1.createCell(i).setCellValue(headers[i]);

            int rowIdx = 1;
            for (Transaction t : transactions) {
                Row row = sheet1.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getTransactionDate().toString());
                row.createCell(1).setCellValue(t.getType().toString());
                row.createCell(2).setCellValue(t.getTransactionCategory() != null ? t.getTransactionCategory().getName() : "미분류");
                row.createCell(3).setCellValue(t.getAmount().doubleValue());
                row.createCell(4).setCellValue(t.getDescription());
            }

            Sheet sheet2 = workbook.createSheet("월간 요약");

            Map<String, BigDecimal> summaryData = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionCategory() != null ? t.getTransactionCategory().getName() : "미분류",
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            Row sumHeaderRow = sheet2.createRow(0);
            sumHeaderRow.createCell(0).setCellValue("카테고리");
            sumHeaderRow.createCell(1).setCellValue("지출 합계");

            int sumRowIdx = 1;
            for (Map.Entry<String, BigDecimal> entry : summaryData.entrySet()) {
                Row row = sheet2.createRow(sumRowIdx++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().doubleValue());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return new ByteArrayResource(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("엑셀 생성 실패", e);
        }
    }
}
