package com.chaewookim.accountbookformoms.global.error;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getStatus();

    String getMessage();

    String name();
}
