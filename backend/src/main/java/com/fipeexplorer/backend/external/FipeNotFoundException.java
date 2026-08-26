package com.fipeexplorer.backend.external;

/** A API pública da FIPE respondeu 404 — veículo/ano não encontrado na base deles. */
public class FipeNotFoundException extends RuntimeException {

    public FipeNotFoundException(Throwable cause) {
        super(cause);
    }
}
