package dio.budgeting.application;

import java.math.BigDecimal;

/** Conversões de dinheiro usadas pelas saídas da aplicação. */
public final class Money {
    private Money() {
    }

    /** Converte centavos em reais sem perda de precisão: 8050 -> 80.50 */
    public static BigDecimal centsToReais(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
