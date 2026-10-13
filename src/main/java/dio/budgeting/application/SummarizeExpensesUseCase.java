package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Nova consulta financeira: quanto foi gasto no total e por categoria,
 * opcionalmente filtrando pelos últimos N dias.
 */
@Service
public class SummarizeExpensesUseCase {
    public static final int MAX_DAYS = 3650;

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public SummarizeExpensesUseCase(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Tool(name = "summarize-expenses",
            description = "Calcula o total gasto e o total por categoria. Use para perguntas como "
                    + "'quanto gastei esta semana?' (lastDays=7), 'quanto gastei este mês?' (lastDays=30) "
                    + "ou 'quanto gastei no total?' (sem lastDays). Valores retornados em reais.")
    public ExpenseSummaryOutput execute(
            @ToolParam(description = "Considerar apenas os últimos N dias. Omita para considerar todos os gastos.",
                    required = false)
            Integer lastDays) {

        List<Transaction> transactions;
        String period;
        if (lastDays == null) {
            transactions = transactionRepository.findAll();
            period = "todo o período";
        } else {
            if (lastDays < 1 || lastDays > MAX_DAYS) {
                throw new IllegalArgumentException("O período deve ser entre 1 e " + MAX_DAYS + " dias.");
            }
            transactions = transactionRepository.findAllCreatedSince(clock.instant().minus(Duration.ofDays(lastDays)));
            period = lastDays == 1 ? "último dia" : "últimos " + lastDays + " dias";
        }

        Map<Category, long[]> totals = new EnumMap<>(Category.class); // [0] = centavos, [1] = quantidade
        long grandTotal = 0;
        for (Transaction t : transactions) {
            long[] acc = totals.computeIfAbsent(t.getCategory(), c -> new long[2]);
            acc[0] += t.getAmount();
            acc[1]++;
            grandTotal += t.getAmount();
        }

        List<CategoryTotal> byCategory = totals.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<Category, long[]> e) -> e.getValue()[0]).reversed())
                .map(e -> new CategoryTotal(e.getKey().name(), (int) e.getValue()[1], Money.centsToReais(e.getValue()[0])))
                .toList();

        return new ExpenseSummaryOutput(period, transactions.size(), Money.centsToReais(grandTotal), byCategory);
    }
}
