package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.category.domain.StandardCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StandardCategoryTest {

    private StandardCategory standardCategory;

    @BeforeEach
    void setUp() {
        standardCategory = new StandardCategory();
    }

    @Test
    @DisplayName("객체 생성 시 기본값 확인")
    void createStandardCategory_DefaultValue() {
        assertEquals(standardCategory.isIncome(), false);
        assertEquals(standardCategory.isInterested(), false);
    }
}