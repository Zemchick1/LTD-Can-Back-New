package com.LTD.ltdWorksAPI.exception;

import com.LTD.ltdWorksAPI.model.entity.ApiError;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleArgumentNotValid(@NotNull MethodArgumentNotValidException ex) {
        ApiError apiError = new ApiError(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value()
        );
        log.error("Validation failed: " + ex.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(@NotNull ResourceNotFoundException ex) {
        ApiError apiError = new ApiError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        log.error(ex.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(@NotNull BadRequestException ex) {
        ApiError apiError = new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        log.error(ex.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({AuthenticationException.class, IllegalArgumentException.class})
    public ResponseEntity<Object> handleAuthenticationException(@NotNull RuntimeException ex) {
        ApiError apiError = new ApiError(
                "Bad Request",
                HttpStatus.BAD_REQUEST.value()
        );
        log.error(ex.getMessage());

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
}
