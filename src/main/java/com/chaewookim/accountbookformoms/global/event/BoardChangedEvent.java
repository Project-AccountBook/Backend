package com.chaewookim.accountbookformoms.global.event;

public record BoardChangedEvent(Long boardId, Type type) {

    public enum Type {
        UPSERT,
        DELETE
    }

    public static BoardChangedEvent upsert(Long boardId) {
        return new BoardChangedEvent(boardId, Type.UPSERT);
    }

    public static BoardChangedEvent delete(Long boardId) {
        return new BoardChangedEvent(boardId, Type.DELETE);
    }
}
