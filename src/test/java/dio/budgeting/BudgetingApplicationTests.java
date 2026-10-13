package dio.budgeting;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe o contexto completo com H2 (perfil "test") e valida a persistência JPA de ponta a ponta. */
@SpringBootTest
class BudgetingApplicationTests {
    @Autowired
    PersistTransactionUseCase persistTransactionUseCase;

    @Autowired
    TransactionRepository transactionRepository;

    @Test
    void contextLoads_andPersistsWithJpa() {
        persistTransactionUseCase.execute(new PersistTransactionInput("Uber para o trabalho", 2390, Category.TRANSPORT));

        var saved = transactionRepository.findAllByCategory(Category.TRANSPORT);
        assertThat(saved).isNotEmpty();
        assertThat(saved.getFirst().getAmount()).isEqualTo(2390);
        assertThat(saved.getFirst().getCreatedAt()).isNotNull();
    }
}
