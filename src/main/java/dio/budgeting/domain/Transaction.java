package dio.budgeting.domain;

import java.time.Instant;

/**
 * Transação financeira (um gasto). O valor é sempre guardado em centavos
 * para evitar problemas de arredondamento com ponto flutuante.
 */
public class Transaction {
    public static final int MAX_DESCRIPTION_LENGTH = 255;
    /** R$ 1.000.000,00 — teto de segurança contra valores absurdos (ex.: erro de conversão para centavos). */
    public static final long MAX_AMOUNT_IN_CENTS = 100_000_000L;

    private final TransactionId id;
    private final String description;
    private final long amount;
    private final Category category;
    private final Instant createdAt;

    /** Reconstitui uma transação já existente (ex.: vinda do banco). Não revalida. */
    public Transaction(TransactionId id, String description, long amount, Category category, Instant createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
    }

    /** Cria uma nova transação aplicando as regras de negócio. */
    public Transaction(String description, long amount, Category category) {
        validate(description, amount, category);
        this.id = new TransactionId();
        this.description = description.strip();
        this.amount = amount;
        this.category = category;
        this.createdAt = Instant.now();
    }

    private static void validate(String description, long amount, Category category) {
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("A descrição do gasto é obrigatória.");
        }
        if (description.strip().length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidTransactionException(
                    "A descrição do gasto deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException("O valor do gasto deve ser maior que zero.");
        }
        if (amount > MAX_AMOUNT_IN_CENTS) {
            throw new InvalidTransactionException(
                    "O valor do gasto passa do limite permitido de um milhão de reais. Confira o valor informado.");
        }
        if (category == null) {
            throw new InvalidTransactionException("A categoria do gasto é obrigatória.");
        }
    }

    public TransactionId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
