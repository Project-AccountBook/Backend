package com.chaewookim.accountbookformoms.domain.portfolio.dto.response;

public record PairPortfolioDetailResponse(
        UserPortfolioDetailResponse me,
        UserPortfolioDetailResponse target
) {
}
