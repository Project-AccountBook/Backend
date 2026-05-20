package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AssetRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.Asset;
import com.chaewookim.accountbookformoms.domain.asset.entity.BankEnum;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountUpdateRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {
    @InjectMocks
    private AssetService assetService;

    @Mock
    private AssetRepository assetRepository;

    private String username;
    private String accountNumber;
    private Asset asset;
    private AccountRequest accountRequest;
    private List<Asset> assetList;

    @BeforeEach
    void setUp() {
        Long userId = 1L;
        Long assetId = 10L;
        username = "username";
        accountNumber = "111112222222";

        asset = Asset.builder()
                .username(username)
                .balance(BigDecimal.valueOf(1000000))
                .bank(BankEnum.KB)
                .build();
        ReflectionTestUtils.setField(asset, "id", assetId);

        accountRequest = new AccountRequest(
                BankEnum.KB,
                accountNumber,
                BigDecimal.valueOf(1000000),
                username
        );

        assetList = List.of(
                Asset.builder()
                        .username(username)
                        .balance(BigDecimal.valueOf(1000000))
                        .bank(BankEnum.KB)
                        .build(),
                Asset.builder()
                        .username(username)
                        .balance(BigDecimal.valueOf(1000000))
                        .bank(BankEnum.KB)
                        .build(),
                Asset.builder()
                        .username(username)
                        .balance(BigDecimal.valueOf(1000000))
                        .bank(BankEnum.KB)
                        .build(),
                Asset.builder()
                        .username(username)
                        .balance(BigDecimal.valueOf(1000000))
                        .bank(BankEnum.KB)
                        .build()
        );
    }

    @Test
    @DisplayName("요청 정보로 계좌가 생성되는지 확인")
    void createAccount() {
        // given
        given(assetRepository.save(any(Asset.class))).willReturn(asset);

        // when
        AccountResponse response = assetService.registAccount(username, accountRequest);

        // then
        assertNotNull(response);

        verify(assetRepository, times(1)).save(any(Asset.class));
    }


    @Test
    @DisplayName("계좌 삭제 - 성공")
    void deleteAccount() {
        // given
        given(assetRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(asset));

        // when
        assetService.deleteAccount(username, accountNumber);

        // then
        verify(assetRepository, times(1)).findByAccountNumber(accountNumber);
        verify(assetRepository, times(1)).deleteByAccountNumber(accountNumber);
    }
    @Test
    @DisplayName("계좌 삭제 - 사용자 불일치로 실패")
    void deleteAccount_AccessDenied() {
        // given
        String unAuthUser = "unAuthUser";

        given(assetRepository.findByAccountNumber(accountNumber)).willReturn(Optional.of(asset));

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> assetService.deleteAccount(unAuthUser, accountNumber)
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_ACCESS_DENIED, exception.getErrorCode());
        verify(assetRepository, times(1)).findByAccountNumber(accountNumber);
        verify(assetRepository, never()).deleteByAccountNumber(accountNumber);
    }


    @Test
    @DisplayName("계좌 전체 조회")
    void selectAllAccounts() {
        // given
        given(assetRepository.findAllByUsername(username)).willReturn(assetList);

        // when
        List<AccountResponse> responses = assetService.getAllAccounts(username);

        // then
        assertNotNull(responses);
    }


    @Test
    @DisplayName("계좌 잔액 수정")
    void updateAccount() {
        // given
        AccountUpdateRequest request = new AccountUpdateRequest(
                BigDecimal.valueOf(10000),
                username,
                10L
        );

        given(assetRepository.findByIdAndUsername(any(), any())).willReturn(Optional.of(asset));

        // when
        AccountResponse response = assetService.updateAccount(username, request);

        // then
        assertNotNull(response);

        assertEquals(10000, response.balance().intValue());

        verify(assetRepository, times(1)).findByIdAndUsername(any(), any());
    }
}