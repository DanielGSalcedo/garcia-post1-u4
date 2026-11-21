package com.universidad.compras.aprobacion;

import com.universidad.compras.estado.OperacionNoPermitida;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ErroresAprobacionHttp {
    @ExceptionHandler(OperacionNoPermitida.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> transicionInvalida(OperacionNoPermitida ex) {
        return Map.of("detalle", ex.getMessage());
    }
}
