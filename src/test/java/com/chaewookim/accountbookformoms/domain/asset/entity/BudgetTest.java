package com.chaewookim.accountbookformoms.domain.asset.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BudgetTest {
    private Budget budget;

    @BeforeEach
    void setUp() {
        budget = Budget.builder()
                .userId(1L)
                .build();
    }

    @Test
    @DisplayName("객체 생성 시 기본값 확인")
    void budgetCreate_DefaultValue() {
        assertEquals(budget.getBudgetAmount(), BigDecimal.ZERO);
        assertEquals(budget.getEstimatedExpense(), BigDecimal.ZERO);
        assertEquals(budget.isPublic(), false);
        assertEquals(budget.getTotalIncome(), BigDecimal.ZERO);
        assertEquals(budget.getTotalExpense(), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("객체 생성 후")
    void budgetCreate_UpdateAll() {
        budget.updateAll(BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(10));

        assertEquals(budget.getBudgetAmount(), BigDecimal.valueOf(10));
        assertEquals(budget.getEstimatedExpense(), BigDecimal.valueOf(10));
        assertEquals(budget.getTotalIncome(), BigDecimal.valueOf(10));
        assertEquals(budget.getTotalExpense(), BigDecimal.valueOf(10));
    }
}