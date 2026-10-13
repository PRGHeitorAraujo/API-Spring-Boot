package dio.budgeting.application.output;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo de gastos de um período. Valores em REAIS.
 *
 * @param period           descrição do período ("últimos 7 dias" ou "todo o período")
 * @param transactionCount quantidade de transações no período
 * @param total            soma de todos os gastos
 * @param byCategory       totais por categoria, do maior para o menor
 */
public record ExpenseSummaryOutput(String period,
                                   int transactionCount,
                                   BigDecimal total,
                                   List<CategoryTotal> byCategory) {

    public record CategoryTotal(String category, int transactionCount, BigDecimal total) {
    }
}
