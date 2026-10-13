package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.infrastructure.ai.AssistantUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Converte erros em respostas padronizadas (RFC 9457 - Problem Details), sempre em português.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidTransactionException.class)
    ResponseEntity<ProblemDetail> handleInvalidTransaction(InvalidTransactionException e) {
        return problem(HttpStatus.valueOf(422), "Transação inválida", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String detail = "Valor inválido para '" + e.getName() + "': " + e.getValue() + ".";
        if (e.getRequiredType() == Category.class) {
            detail += " Categorias aceitas: " + allCategories() + ".";
        }
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "Não foi possível ler o JSON enviado. Confira os campos e tipos. Categorias aceitas: " + allCategories() + ".");
    }

    @ExceptionHandler({InvalidAudioFileException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.valueOf(413), "Arquivo muito grande", "O áudio deve ter no máximo 10 MB.");
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    ResponseEntity<ProblemDetail> handleAssistantUnavailable(AssistantUnavailableException e) {
        return problem(HttpStatus.BAD_GATEWAY, "Assistente indisponível",
                e.getMessage() + " Verifique a OPENAI_API_KEY e tente novamente.");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }

    private static String allCategories() {
        return Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
