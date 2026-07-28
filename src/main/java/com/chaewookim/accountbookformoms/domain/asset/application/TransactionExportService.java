package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionExportService {

    static final int MAX_EXPORT_MONTHS = 12;

    private static final String SHEET_DETAILS = "거래 내역";
    private static final String SHEET_SUMMARY = "기간 요약";
    private static final String[] DETAIL_HEADERS = {"날짜", "유형", "계좌", "카테고리", "금액", "상대 계좌", "메모"};

    private final TransactionRepository transactionRepository;

    public static String buildFileName(LocalDate startDate, LocalDate endDate) {
        return String.format("MODI_거래내역_%s_%s.xlsx", startDate, endDate);
    }

    public Resource exportToCsv(Long userId, LocalDate startDate, LocalDate endDate) {
        validateExportPeriod(startDate, endDate);

        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            ExportStyles styles = ExportStyles.create(workbook);
            writeDetailSheet(workbook, styles, transactions, startDate, endDate);
            writeSummarySheet(workbook, styles, transactions, startDate, endDate);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return new ByteArrayResource(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("엑셀 생성 실패", e);
        }
    }

    private void writeDetailSheet(
            Workbook workbook,
            ExportStyles styles,
            List<Transaction> transactions,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sheet sheet = workbook.createSheet(SHEET_DETAILS);
        sheet.createFreezePane(0, 4);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("MODI 거래 내역");
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, DETAIL_HEADERS.length - 1));

        Row periodRow = sheet.createRow(1);
        Cell periodCell = periodRow.createCell(0);
        periodCell.setCellValue("기간: " + startDate + " ~ " + endDate);
        periodCell.setCellStyle(styles.subtitleStyle());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, DETAIL_HEADERS.length - 1));

        Row headerRow = sheet.createRow(3);
        for (int i = 0; i < DETAIL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(DETAIL_HEADERS[i]);
            cell.setCellStyle(styles.headerStyle());
        }

        int rowIdx = 4;
        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowIdx++);
            setDateCell(row, 0, transaction.getTransactionDate(), styles.textStyle());
            setTextCell(row, 1, formatType(transaction.getType()), styles.textStyle());
            setTextCell(row, 2, resolveAccountName(transaction), styles.textStyle());
            setTextCell(row, 3, resolveCategoryName(transaction), styles.textStyle());
            setAmountCell(row, 4, transaction.getAmount(), styles.amountStyle());
            setTextCell(row, 5, resolveTargetAccountName(transaction), styles.textStyle());
            setTextCell(row, 6, nullToEmpty(transaction.getDescription()), styles.textStyle());
        }

        autosizeColumns(sheet, DETAIL_HEADERS.length);
        sheet.setColumnWidth(6, Math.max(sheet.getColumnWidth(6), 8000));
    }

    private void writeSummarySheet(
            Workbook workbook,
            ExportStyles styles,
            List<Transaction> transactions,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sheet sheet = workbook.createSheet(SHEET_SUMMARY);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("MODI 기간 요약");
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        Row periodRow = sheet.createRow(1);
        Cell periodCell = periodRow.createCell(0);
        periodCell.setCellValue("기간: " + startDate + " ~ " + endDate);
        periodCell.setCellStyle(styles.subtitleStyle());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal netIncome = totalIncome.subtract(totalExpense);
        long transferCount = transactions.stream().filter(t -> t.getType() == TransactionType.TRANSFER).count();

        Row summaryHeaderRow = sheet.createRow(3);
        setTextCell(summaryHeaderRow, 0, "항목", styles.headerStyle());
        setTextCell(summaryHeaderRow, 1, "금액 / 건수", styles.headerStyle());

        int rowIdx = 4;
        rowIdx = writeSummaryRow(sheet, rowIdx, "총 수입", totalIncome, styles);
        rowIdx = writeSummaryRow(sheet, rowIdx, "총 지출", totalExpense, styles);
        rowIdx = writeSummaryRow(sheet, rowIdx, "순수입 (수입 - 지출)", netIncome, styles);

        Row transferRow = sheet.createRow(rowIdx++);
        setTextCell(transferRow, 0, "이체 건수", styles.textStyle());
        Cell transferCountCell = transferRow.createCell(1);
        transferCountCell.setCellValue(transferCount);
        transferCountCell.setCellStyle(styles.textStyle());

        rowIdx++;

        rowIdx = writeCategoryBreakdownSection(
                sheet, rowIdx, styles, transactions, TransactionType.INCOME, "카테고리별 수입", "수입 합계", "수입 내역 없음"
        );

        rowIdx++;

        rowIdx = writeCategoryBreakdownSection(
                sheet, rowIdx, styles, transactions, TransactionType.EXPENSE, "카테고리별 지출", "지출 합계", "지출 내역 없음"
        );

        autosizeColumns(sheet, 2);
    }

    private int writeCategoryBreakdownSection(
            Sheet sheet,
            int rowIdx,
            ExportStyles styles,
            List<Transaction> transactions,
            TransactionType type,
            String sectionTitle,
            String amountHeader,
            String emptyMessage
    ) {
        Row sectionRow = sheet.createRow(rowIdx++);
        setTextCell(sectionRow, 0, sectionTitle, styles.headerStyle());
        setTextCell(sectionRow, 1, amountHeader, styles.headerStyle());

        Map<String, BigDecimal> amountByCategory = summarizeByCategory(transactions, type);

        if (amountByCategory.isEmpty()) {
            Row emptyRow = sheet.createRow(rowIdx++);
            setTextCell(emptyRow, 0, emptyMessage, styles.textStyle());
            return rowIdx;
        }

        for (Map.Entry<String, BigDecimal> entry : amountByCategory.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            setTextCell(row, 0, entry.getKey(), styles.textStyle());
            setAmountCell(row, 1, entry.getValue(), styles.amountStyle());
        }

        return rowIdx;
    }

    private Map<String, BigDecimal> summarizeByCategory(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(
                        this::resolveCategoryName,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private int writeSummaryRow(Sheet sheet, int rowIdx, String label, BigDecimal amount, ExportStyles styles) {
        Row row = sheet.createRow(rowIdx);
        setTextCell(row, 0, label, styles.textStyle());
        setAmountCell(row, 1, amount, styles.amountStyle());
        return rowIdx + 1;
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatType(TransactionType type) {
        return switch (type) {
            case INCOME -> "수입";
            case EXPENSE -> "지출";
            case TRANSFER -> "이체";
        };
    }

    private String resolveAccountName(Transaction transaction) {
        if (transaction.getSnapshotAccountName() != null && !transaction.getSnapshotAccountName().isBlank()) {
            return transaction.getSnapshotAccountName();
        }
        if (transaction.getAccount() != null) {
            return transaction.getAccount().getAccountName();
        }
        return "-";
    }

    private String resolveTargetAccountName(Transaction transaction) {
        if (transaction.getType() != TransactionType.TRANSFER) {
            return "-";
        }
        if (transaction.getSnapshotTargetAccountName() != null && !transaction.getSnapshotTargetAccountName().isBlank()) {
            return transaction.getSnapshotTargetAccountName();
        }
        if (transaction.getTargetAccount() != null) {
            return transaction.getTargetAccount().getAccountName();
        }
        return "-";
    }

    private String resolveCategoryName(Transaction transaction) {
        if (transaction.getTransactionCategory() != null) {
            return transaction.getTransactionCategory().getName();
        }
        return "기타";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void setTextCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setDateCell(Row row, int column, LocalDate date, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(date.toString());
        cell.setCellStyle(style);
    }

    private void setAmountCell(Row row, int column, BigDecimal amount, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(amount.doubleValue());
        cell.setCellStyle(style);
    }

    private void autosizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

    private void validateExportPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CustomException(AssetErrorCode.INVALID_EXPORT_DATE_RANGE);
        }
        LocalDate maxEndDate = startDate.plusMonths(MAX_EXPORT_MONTHS).minusDays(1);
        if (endDate.isAfter(maxEndDate)) {
            throw new CustomException(AssetErrorCode.EXPORT_PERIOD_TOO_LONG);
        }
    }

    private record ExportStyles(
            CellStyle titleStyle,
            CellStyle subtitleStyle,
            CellStyle headerStyle,
            CellStyle amountStyle,
            CellStyle textStyle
    ) {
        static ExportStyles create(Workbook workbook) {
            DataFormat dataFormat = workbook.createDataFormat();

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            Font subtitleFont = workbook.createFont();
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitleStyle.setFont(subtitleFont);

            CellStyle headerStyle = bordered(workbook.createCellStyle());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle amountStyle = bordered(workbook.createCellStyle());
            amountStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            amountStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle textStyle = bordered(workbook.createCellStyle());
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            return new ExportStyles(titleStyle, subtitleStyle, headerStyle, amountStyle, textStyle);
        }

        private static CellStyle bordered(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
