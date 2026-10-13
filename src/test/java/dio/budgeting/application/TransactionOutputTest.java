package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Teste de regressão do bug do projeto base: 8000 centavos aparecia como 8000.0 reais. */
class TransactionOutputTest {

    @ParameterizedTest
    @CsvSource({
            "8000, 80.00",
            "1250, 12.50",
            "1, 0.01",
            "100000, 1000.00",
    })
    void should_convertCentsToReais(long cents, String expectedReais) {
        var output = TransactionOutput.from(new Transaction("Teste", cents, Category.OTHER));

        assertThat(output.value()).isEqualByComparingTo(new BigDecimal(expectedReais));
    }
}
