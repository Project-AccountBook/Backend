package com.chaewookim.accountbookformoms.domain.asset.event;

public record GoalAchievedCheckEvent(

        Long userId,
        Long accountId
) {
}
