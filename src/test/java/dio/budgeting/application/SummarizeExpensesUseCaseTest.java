package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SummarizeExpensesUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final SummarizeExpensesUseCase useCase =
            new SummarizeExpensesUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        save("Mercado", 8000, Category.GROCERIES, 1);
        save("Padaria", 1250, Category.GROCERIES, 2);
        save("Farmácia", 4000, Category.PHARMA, 3);
        save("Posto", 20000, Category.AUTO, 20); // fora da última semana
    }

    @Test
    void should_summarizeAllExpenses_when_noPeriodIsGiven() {
        var summary = useCase.execute(null);

        assertThat(summary.period()).isEqualTo("todo o período");
        assertThat(summary.transactionCount()).isEqualTo(4);
        assertThat(summary.total()).isEqualByComparingTo(new BigDecimal("332.50"));
        // ordenado do maior para o menor total
        assertThat(summary.byCategory()).extracting(ExpenseSummaryOutput.CategoryTotal::category).containsExactly("AUTO", "GROCERIES", "PHARMA");
    }

    @Test
    void should_onlyConsiderLastDays_when_periodIsGiven() {
        var summary = useCase.execute(7);

        assertThat(summary.period()).isEqualTo("últimos 7 dias");
        assertThat(summary.transactionCount()).isEqualTo(3);
        assertThat(summary.total()).isEqualByComparingTo(new BigDecimal("132.50"));

        var groceries = summary.byCategory().getFirst();
        assertThat(groceries.category()).isEqualTo("GROCERIES");
        assertThat(groceries.transactionCount()).isEqualTo(2);
        assertThat(groceries.total()).isEqualByComparingTo(new BigDecimal("92.50"));
    }

    @Test
    void should_returnZero_when_thereAreNoExpenses() {
        var emptyUseCase = new SummarizeExpensesUseCase(new InMemoryTransactionRepository(), Clock.systemUTC());

        var summary = emptyUseCase.execute(30);

        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.byCategory()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, SummarizeExpensesUseCase.MAX_DAYS + 1})
    void should_reject_when_periodIsInvalid(int lastDays) {
        assertThatThrownBy(() -> useCase.execute(lastDays)).isInstanceOf(IllegalArgumentException.class);
    }

    private void save(String description, long amount, Category category, int daysAgo) {
        repository.save(new Transaction(new TransactionId(), description, amount, category,
                NOW.minus(Duration.ofDays(daysAgo))));
    }
}
