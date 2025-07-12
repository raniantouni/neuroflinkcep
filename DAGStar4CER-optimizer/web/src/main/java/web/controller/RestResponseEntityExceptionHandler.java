package web.controller;

import com.google.gson.JsonParseException;
import core.exception.OptimizerException;
import lombok.extern.java.Log;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@ControllerAdvice
@ResponseBody
@Log
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    //HANDLERS
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleConstraintViolationException(RepositoryConstraintViolationException ex, WebRequest request) {
        return ex.getErrors().getAllErrors().stream()
                .map(ObjectError::toString)
                .collect(Collectors.joining("\n"));
    }

    @ExceptionHandler(value = {IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    protected String handleConflict(Exception ex, WebRequest request) {
        return ex.getMessage();
    }

    @ExceptionHandler(value = OptimizerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected String handleOptimizerException(OptimizerException ex, WebRequest request) {
        return ex.getMessage();
    }

    @ExceptionHandler(value = NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected String handleNullPointerException(OptimizerException ex, WebRequest request) {
        return ex.getMessage();
    }

    @ExceptionHandler(value = {UncategorizedElasticsearchException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected String handleESConflict(Exception ex, WebRequest request) {
        return "Something went really wrong.\n" + ex.getMessage() + "\n" + request.toString();
    }

    @ExceptionHandler(value = DataAccessResourceFailureException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected String handleDataAccessResourceFailureException(DataAccessResourceFailureException ex, WebRequest request) {
        return ex.getMessage();
    }

    @ExceptionHandler(value = {ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected String handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return ex.getMessage();
    }

    @ExceptionHandler(value = {com.google.gson.JsonParseException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected String handleJsonParseException(JsonParseException ex, WebRequest request) {
        return ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleException1(Exception ex, WebRequest request) {
        String cause = ex.getMessage();
        log.warning(String.format("Exception %s.", cause));
        return cause;
    }

    //Overridden METHODS
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        return new ResponseEntity<>(ex.getMessage(), status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers,
                                                                         HttpStatus status, WebRequest request) {
        return new ResponseEntity<>(ex.getMessage(), status);
    }

    //For class validation
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<ObjectError> globalErrors = ex.getBindingResult().getGlobalErrors();
        List<String> errors = new ArrayList<>(fieldErrors.size() + globalErrors.size());
        String error;
        for (FieldError fieldError : fieldErrors) {
            error = fieldError.getField() + ", " + fieldError.getDefaultMessage();
            errors.add(error);
        }
        for (ObjectError objectError : globalErrors) {
            error = objectError.getObjectName() + ", " + objectError.getDefaultMessage();
            errors.add(error);
        }
        return new ResponseEntity<>(String.join("\n", errors), HttpStatus.BAD_REQUEST);
    }
}