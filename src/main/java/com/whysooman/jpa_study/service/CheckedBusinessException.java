package com.whysooman.jpa_study.service;

public class CheckedBusinessException extends Exception {
    public CheckedBusinessException(String message) {
        super(message);
    }
}
