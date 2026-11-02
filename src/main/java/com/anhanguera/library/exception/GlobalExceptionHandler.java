package com.anhanguera.library.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converte as exceções da aplicação (e as do Spring MVC) no corpo de erro padrão {@link ApiError}.
 * Estende {@link ResponseEntityExceptionHandler} para que erros do próprio framework
 * (rota inexistente, método não suportado, JSON malformado...) também sigam o mesmo formato.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INVALID_FIELDS_MESSAGE = "Há campos inválidos na requisição. Verifique os dados informados.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Object> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null, null);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Object> handleFieldValidation(FieldValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of(ex.getField(), ex.getMessage()), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade dos dados: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "A operação não pode ser concluída porque viola uma restrição de integridade dos dados.", null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado no servidor. Tente novamente mais tarde.", null, null);
    }

    // 400 - falha de Bean Validation no corpo da requisição (@Valid)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "Valor inválido."));
        }
        String message = fields.size() == 1 ? fields.values().iterator().next() : INVALID_FIELDS_MESSAGE;
        return build(HttpStatus.BAD_REQUEST, message, fields, headers);
    }

    // 400 - JSON malformado, corpo ausente ou valor com tipo/formato inválido (ex.: data fora de AAAA-MM-DD)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        if (ex.getCause() instanceof MismatchedInputException mismatch) {
            String field = fieldPath(mismatch);
            if (!field.isEmpty()) {
                String fieldMessage = describeExpectedType(mismatch.getTargetType());
                return build(HttpStatus.BAD_REQUEST, fieldMessage, Map.of(field, fieldMessage), headers);
            }
        }
        return build(HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou não é um JSON válido.", null, headers);
    }

    // 400 - parâmetro de URL com tipo inválido (ex.: /books/abc ou ?status=XYZ)
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        Class<?> requiredType = ex.getRequiredType();
        String message = "Valor inválido para o parâmetro '" + name + "': '" + ex.getValue() + "'.";
        if (requiredType != null && requiredType.isEnum()) {
            String accepted = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message += " Valores aceitos: " + accepted + ".";
        }
        Map<String, String> fields = name != null ? Map.of(name, message) : null;
        return build(HttpStatus.BAD_REQUEST, message, fields, headers);
    }

    // Demais exceções do Spring MVC (404 de rota, 405, 415, parâmetro ausente...)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        String message = switch (ex) {
            case NoResourceFoundException e -> "Recurso não encontrado.";
            case HttpRequestMethodNotSupportedException e -> "Método HTTP não suportado para este recurso.";
            case HttpMediaTypeNotSupportedException e -> "Tipo de conteúdo não suportado. Envie os dados em JSON.";
            case MissingServletRequestParameterException e ->
                    "O parâmetro obrigatório '" + e.getParameterName() + "' não foi informado.";
            default -> statusCode.is4xxClientError()
                    ? "Requisição inválida."
                    : "Ocorreu um erro inesperado no servidor. Tente novamente mais tarde.";
        };
        return build(statusCode, message, null, headers);
    }

    private ResponseEntity<Object> build(HttpStatusCode status, String message,
                                         Map<String, String> fields, HttpHeaders headers) {
        return ResponseEntity.status(status)
                .headers(headers)
                .body(ApiError.of(status, message, fields));
    }

    private static String fieldPath(JacksonException ex) {
        return ex.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private static String describeExpectedType(Class<?> targetType) {
        if (targetType != null && Temporal.class.isAssignableFrom(targetType)) {
            return "Data inválida. Use o formato AAAA-MM-DD.";
        }
        if (targetType != null && (Number.class.isAssignableFrom(targetType)
                || (targetType.isPrimitive() && targetType != boolean.class))) {
            return "O valor informado deve ser numérico.";
        }
        return "Valor inválido.";
    }
}
