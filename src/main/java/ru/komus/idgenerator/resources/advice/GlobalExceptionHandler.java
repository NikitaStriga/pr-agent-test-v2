package ru.komus.idgenerator.resources.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.komus.idgenerator.data.dto.ErrorDto;
import ru.komus.idgenerator.data.exceptions.AlreadyExistsException;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Exception handler for response.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler
{

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle all NoSuchElementExceptions.
     */
    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<Object> handleNoSuchElementException(NoSuchElementException ex, WebRequest request)
    {
        return createErrorResponse(ex, request, NOT_FOUND);
    }

    /**
     * Handle all NoSuchElementExceptions.
     */
    @ExceptionHandler(AlreadyExistsException.class)
    protected ResponseEntity<Object> handleAlreadyExistsException(AlreadyExistsException ex, WebRequest request)
    {
        return createErrorResponse(ex, request, BAD_REQUEST);
    }

    /**
     * Handle all MethodArgumentNotValidException.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request)
    {
        ErrorDto errorDto;
        logger.info("error: " + ex.getMessage());
        if (ex.getFieldError() != null)
        {
            errorDto = new ErrorDto(ex.getFieldError().getDefaultMessage(), getPath(request));
        }
        else
        {
            errorDto = new ErrorDto(ex.getMessage(), getPath(request));
        }
        return handleExceptionInternal(ex, errorDto, headers, status, request);
    }

    /**
     * Return ResponseEntity with data of exception, request URI, HttpStatus.
     */
    private ResponseEntity<Object> createErrorResponse(RuntimeException ex, WebRequest request, HttpStatus status)
    {
        logger.info("error: " + ex.getMessage());
        ErrorDto errorDto = new ErrorDto(ex.getMessage(), getPath(request));
        return new ResponseEntity<>(errorDto, status);
    }

    /**
     * Return request URI.
     */
    private String getPath(WebRequest request)
    {
        return (request instanceof ServletWebRequest)
               ? ((ServletWebRequest) request).getRequest().getRequestURI()
               : request.getContextPath();
    }
}