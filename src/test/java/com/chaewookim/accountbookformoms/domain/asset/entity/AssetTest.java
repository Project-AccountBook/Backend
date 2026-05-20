package com.chaewookim.accountbookformoms.domain.asset.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AssetTest {

    private Asset asset;
    private String username;

    @BeforeEach
    void setUp() {
        username = "username";

        asset = Asset.builder()
                .bank(BankEnum.KB)
                .accountNumber("123")
                .username(username)
                .build();
        ReflectionTestUtils.setField(asset, "id", 1L);
    }

    @Test
    @DisplayName("Asset 객체 생성 시 기본값의 정상 적용")
    void createAsset_DefaultValues() {

        assertEquals(asset.getBalance(), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Asset 객체 생성 후 updateBalance 정상 작동 확인")
    void createAsset_subtractBalance() {
        asset.subtractBalance(BigDecimal.TEN);

        assertEquals(asset.getBalance(), BigDecimal.TEN);
    }
}