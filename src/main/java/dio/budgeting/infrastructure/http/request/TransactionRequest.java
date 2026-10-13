package dio.budgeting.infrastructure.http.request;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /transactions. {@code amount} é em CENTAVOS (8000 = R$ 80,00).
 * A validação aqui dá respostas 400 claras na borda HTTP; o domínio valida de novo,
 * porque a IA também cria transações sem passar por este request.
 */
public record TransactionRequest(
        @NotBlank(message = "é obrigatória")
        @Size(max = Transaction.MAX_DESCRIPTION_LENGTH, message = "deve ter no máximo 255 caracteres")
        String description,

        @NotNull(message = "é obrigatória")
        Category category,

        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que zero")
        @Max(value = Transaction.MAX_AMOUNT_IN_CENTS, message = "passa do limite de R$ 1.000.000,00")
        Long amount) {

    public PersistTransactionInput toInput() {
        return new PersistTransactionInput(description, amount, category);
    }
}
