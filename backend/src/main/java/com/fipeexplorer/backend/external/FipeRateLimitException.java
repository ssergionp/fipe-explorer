package com.fipeexplorer.backend.external;

/** A API pública da FIPE respondeu 429 — cota diária (500 sem token / 1000 com token) estourada. */
public class FipeRateLimitException extends RuntimeException {

    public FipeRateLimitException(Throwable cause) {
        super(cause);
    }
}
