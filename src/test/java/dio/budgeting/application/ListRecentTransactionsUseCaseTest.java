package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ListRecentTransactionsUseCaseTest {
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final ListRecentTransactionsUseCase useCase = new ListRecentTransactionsUseCase(repository);

    @Test
    void should_returnNewestFirst_andRespectLimit() {
        for (int i = 1; i <= 8; i++) {
            repository.save(new Transaction(new TransactionId(), "Gasto " + i, i * 100L, Category.OTHER,
                    Instant.parse("2026-10-01T00:00:00Z").plusSeconds(i)));
        }

        assertThat(useCase.execute(null)).hasSize(ListRecentTransactionsUseCase.DEFAULT_LIMIT)
                .first().extracting(TransactionOutput::description).isEqualTo("Gasto 8");
        assertThat(useCase.execute(2)).extracting(TransactionOutput::description).containsExactly("Gasto 8", "Gasto 7");
        assertThat(useCase.execute(0)).hasSize(1); // limite mínimo é 1
        assertThat(useCase.execute(1000)).hasSize(8);
    }
}
