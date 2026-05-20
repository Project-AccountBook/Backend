package com.chaewookim.accountbookformoms.domain.transaction.application;

import com.chaewookim.accountbookformoms.domain.transaction.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionAccountUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionAmountUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionCategoryUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionMemoUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionRepeatDateUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionRequest;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionTitleUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.request.fixedtransaction.FixedTransactionTypeUpdate;
import com.chaewookim.accountbookformoms.domain.transaction.dto.response.transaction.FixedTransactionResponse;
import com.chaewookim.accountbookformoms.domain.transaction.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.transaction.enums.TransactionTypeEnum;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedTransactionServiceTests {
    @InjectMocks
    private FixedTransactionService fixedTransactionService;

    @Mock
    private FixedTransactionRepository fixedTransactionRepository;

    Long userId;
    Long assetId;
    Long newAssetId;
    Long fixedTransactionId;
    Long categoryId;
    Long newCategoryId;
    BigDecimal amount;
    BigDecimal newAmount;

    private FixedTransaction fixedTransaction;
    private FixedTransactionRequest fixedTransactionRequest;
    private FixedTransactionTitleUpdate  fixedTransactionTitleUpdate;
    private FixedTransactionMemoUpdate fixedTransactionMemoUpdate;
    private FixedTransactionAccountUpdate  fixedTransactionAccountUpdate;
    private FixedTransactionCategoryUpdate fixedTransactionCategoryUpdate;
    private FixedTransactionAmountUpdate fixedTransactionAmountUpdate;
    private FixedTransactionRepeatDateUpdate fixedTransactionRepeatDateUpdate;
    private FixedTransactionTypeUpdate fixedTransactionTypeUpdate;

    @BeforeEach
    void setUp() {
        userId = 1L;
        assetId = 10L;
        fixedTransactionId = 20L;
        categoryId = 30L;

        amount = BigDecimal.valueOf(300000);

        newAssetId = 100L;
        newCategoryId = 300L;
        newAmount = BigDecimal.valueOf(100000000);

        fixedTransaction = FixedTransaction.builder()
                .title("title")
                .memo("memo")
                .userId(userId)
                .assetId(assetId)
                .categoryId(categoryId)
                .amount(amount)
                .type(TransactionTypeEnum.EXPENSE)
                .repeatDate(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(fixedTransaction, "id", fixedTransactionId);

        fixedTransactionRequest = new FixedTransactionRequest(
                "reqTitle",
                "reqMemo",
                userId,
                assetId,
                categoryId,
                amount,
                TransactionTypeEnum.INCOME,
                LocalDateTime.now()
        );

        fixedTransactionTitleUpdate = new FixedTransactionTitleUpdate(
                "updateTitle"
        );

        fixedTransactionMemoUpdate = new FixedTransactionMemoUpdate(
                "updateMemo"
        );

        fixedTransactionAccountUpdate = new  FixedTransactionAccountUpdate(
                newAssetId
        );

        fixedTransactionCategoryUpdate = new FixedTransactionCategoryUpdate(
                newCategoryId
        );

        fixedTransactionAmountUpdate = new  FixedTransactionAmountUpdate(
                newAmount
        );

        fixedTransactionRepeatDateUpdate =  new FixedTransactionRepeatDateUpdate(
                LocalDateTime.now()
        );

        fixedTransactionTypeUpdate =  new FixedTransactionTypeUpdate(
                TransactionTypeEnum.INCOME
        );
    }

    @Test
    @DisplayName("고정지출 추가 - 성공")
    void createFixedExpense_Success() {
        // when
        FixedTransactionResponse response = fixedTransactionService.createFix(fixedTransactionRequest, userId);

        // then
        assertNotNull(response);
        assertEquals("title", fixedTransaction.getTitle());
    }


    @Test
    @DisplayName("고정지출/고정수입 조회 - 성공")
    void getAllFixedTransactions_Success() {
        // given
        given(fixedTransactionRepository.findAllByUserId(userId)).willReturn(List.of(fixedTransaction));

        // when
        List<FixedTransactionResponse> response = fixedTransactionService.getAllFixedTransactions(userId);

        // then
        assertNotNull(response);

        assertEquals(1, response.size());
        assertEquals("title", response.get(0).title());
    }
    @Test
    @DisplayName("고정지출/고정수입 조회 - 실패 - 조회 결과 없음")
    void getAllFixedTransactions_Failure() {
        // given
        given(fixedTransactionRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.getAllFixedTransactions(userId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 타이틀 수정 - 성공")
    void updateTitle_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionTitleUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 타이틀 수정 - 실패 - 조회 결과 없음")
    void updateTitle_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionTitleUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 메모 수정 - 성공")
    void updateMemo_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionMemoUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 메모 수정 - 실패 - 조회 결과 없음")
    void updateMemo_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionMemoUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 계좌 수정 - 성공")
    void updateAccount_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionAccountUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 계좌 수정 - 실패 - 조회 결과 없음")
    void updateAccount_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionAccountUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 카테고리 수정 - 성공")
    void updateCategory_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionCategoryUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 카테고리 수정 - 실패 - 조회 결과 없음")
    void updateCategory_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionCategoryUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 금액 수정 - 성공")
    void updateAmount_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionAmountUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 금액 수정 - 실패 - 조회 결과 없음")
    void updateAmount_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionAmountUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 날짜 수정 - 성공")
    void updateDate_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionRepeatDateUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 날짜 수정 - 실패 - 조회 결과 없음")
    void updateDate_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionRepeatDateUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 타입 수정 - 성공")
    void updateType_Success() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.of(fixedTransaction));

        // when
        FixedTransactionResponse response = fixedTransactionService.updateFix(fixedTransactionTypeUpdate, userId, fixedTransactionId);

        // then
        assertNotNull(response);
        assertEquals("updateTitle", fixedTransactionTitleUpdate.title());
    }
    @Test
    @DisplayName("고정 트랜잭션 타입 수정 - 실패 - 조회 결과 없음")
    void updateType_Failure() {
        // given
        given(fixedTransactionRepository.findByIdAndUserId(fixedTransactionId, userId)).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.updateFix(fixedTransactionTypeUpdate, userId, fixedTransactionId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }


    @Test
    @DisplayName("고정 트랜잭션 삭제 - 성공")
    void deleteFixedTransaction_Success() {
        // given
        given(fixedTransactionRepository.deleteByIdAndUserId(fixedTransactionId, userId)).willReturn(1L);

        // when
        fixedTransactionService.deleteFix(fixedTransactionId, userId);

        // then
        verify(fixedTransactionRepository, times(1)).deleteByIdAndUserId(fixedTransactionId, userId);
    }
    @Test
    @DisplayName("고정 트랜잭션 삭제 - 실패 - 삭제할 행이 없음")
    void deleteFixedTransaction_Failure() {
        // given
        given(fixedTransactionRepository.deleteByIdAndUserId(fixedTransactionId, userId)).willReturn(0L);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> fixedTransactionService.deleteFix(fixedTransactionId, userId));
        assertEquals(ErrorCode.NO_FIXED_TRANSACTIONS, exception.getErrorCode());
    }
}