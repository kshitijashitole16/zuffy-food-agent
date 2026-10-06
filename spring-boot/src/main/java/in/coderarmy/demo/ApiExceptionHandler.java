package in.coderarmy.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static in.coderarmy.demo.ApiModels.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(new ErrorBody("REQUEST_FAILED",
                error.getReason() == null ? "Request failed." : error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException error) {
        var field = error.getBindingResult().getFieldErrors().stream().findFirst();
        String message = field.map(value -> value.getField() + ": " + value.getDefaultMessage()).orElse("Invalid request.");
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_INPUT", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> malformed(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_JSON", "Send a valid JSON request body."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorBody> missingParameter(MissingServletRequestParameterException error) {
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_INPUT", "Missing required parameter: " + error.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> unexpected(Exception error) {
        log.error("Unexpected API error", error);
        return ResponseEntity.internalServerError().body(new ErrorBody("SERVER_ERROR", "Something went wrong. Check the backend console and try again."));
    }
}
