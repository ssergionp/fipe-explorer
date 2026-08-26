package com.fipeexplorer.backend.external;

/** Falha de rede ou erro 5xx da API pública da FIPE — condição transitória, não cacheável. */
public class FipeUnavailableException extends RuntimeException {

    public FipeUnavailableException(Throwable cause) {
        super(cause);
    }
}
