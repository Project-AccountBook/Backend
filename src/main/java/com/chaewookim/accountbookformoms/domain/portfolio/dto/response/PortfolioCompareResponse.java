package com.chaewookim.accountbookformoms.domain.portfolio.dto.response;

import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.ExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.IncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.enums.PortfolioCompareType;

public record PortfolioCompareResponse(
        PortfolioCompareType type,
        String yearMonth,
        IncomeCompareResponse income,
        ExpenseCompareResponse expense,
        BudgetCompareResponse budget
) {
}
