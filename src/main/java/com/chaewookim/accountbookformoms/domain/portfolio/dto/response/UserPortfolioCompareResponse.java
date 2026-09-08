package com.chaewookim.accountbookformoms.domain.portfolio.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.enums.PortfolioCompareType;

public record UserPortfolioCompareResponse(
        PortfolioCompareType type,
        String yearMonth,
        Long myUserId,
        Long targetUserId,
        UserIncomeCompareResponse income,
        UserExpenseCompareResponse expense,
        UserBudgetCompareResponse budget
) {
}
