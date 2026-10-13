package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistTransactionUseCaseTest {
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final PersistTransactionUseCase useCase = new PersistTransactionUseCase(repository);

    @Test
    void should_saveTransaction_when_inputIsValid() {
        var output = useCase.execute(new PersistTransactionInput("Remédio", 4590, Category.PHARMA));

        assertThat(repository.size()).isEqualTo(1);
        assertThat(output.category()).isEqualTo("PHARMA");
        assertThat(output.value()).isEqualByComparingTo(new BigDecimal("45.90"));
    }

    @Test
    void should_notSave_when_inputIsInvalid() {
        assertThatThrownBy(() -> useCase.execute(new PersistTransactionInput("Remédio", 0, Category.PHARMA)))
                .isInstanceOf(InvalidTransactionException.class);

        assertThat(repository.size()).isZero();
    }
}
