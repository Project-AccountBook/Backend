package com.chaewookim.accountbookformoms.domain.asset.entity;

import com.chaewookim.accountbookformoms.domain.category.domain.UserCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserCreatedCategoryTest {

    @Test
    @DisplayName("객체 생성 시 기본값 정상 삽입 확인")
    void createUserCategory() {
        UserCategory userCategory = new UserCategory();

        assertEquals(userCategory.isIncome(), false);
        assertEquals(userCategory.isInterested(), false);
    }
}