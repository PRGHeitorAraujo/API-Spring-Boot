package dio.budgeting.application.output;

import dio.budgeting.application.Money;
import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;

/**
 * Transação pronta para ser exibida. {@code value} está em REAIS (ex.: 80.50).
 * No projeto base o valor em centavos era exibido como se fosse reais (8000 virava 8000.0);
 * aqui a conversão é feita explicitamente.
 */
public record TransactionOutput(String id,
                                String description,
                                String category,
                                BigDecimal value,
                                String createdAt) {

    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                Money.centsToReais(transaction.getAmount()),
                transaction.getCreatedAt() == null ? null : transaction.getCreatedAt().toString());
    }
}
