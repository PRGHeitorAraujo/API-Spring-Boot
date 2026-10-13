package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.TransactionOutput;

import java.math.BigDecimal;

/** {@code amount} em REAIS (ex.: 80.00). */
public record TransactionResponse(String id, String category, String description, BigDecimal amount, String createdAt) {
    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(output.id(), output.category(), output.description(), output.value(), output.createdAt());
    }
}
