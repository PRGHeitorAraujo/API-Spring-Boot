package dio.budgeting;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.infrastructure.ai.BudgetAssistant;
import dio.budgeting.infrastructure.persistence.repository.TransactionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de ponta a ponta com a OpenAI de verdade (custam alguns centavos).
 * Só rodam quando OPENAI_API_KEY está definida.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class BudgetAssistantIT {
    @Autowired
    BudgetAssistant assistant;

    @Autowired
    TransactionRepository repository;

    @Autowired
    TransactionEntityRepository entityRepository;

    @BeforeEach
    void cleanDatabase() {
        entityRepository.deleteAll();
    }

    @Test
    void should_persistTransactionInCents_when_userDescribesExpense() {
        var interaction = assistant.handleText("Gastei 80 reais no mercado");

        var saved = repository.findAllByCategory(Category.GROCERIES);
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getAmount()).isEqualTo(8000);
        assertThat(interaction.reply()).isNotBlank();
        System.out.println(interaction.reply());
    }

    @Test
    void should_registerMultipleExpenses_andSummarize() {
        assistant.handleText("Paguei 30 reais de Uber e 45 reais e 50 centavos na farmácia");

        assertThat(repository.findAll()).hasSize(2);

        var interaction = assistant.handleText("Quanto eu gastei essa semana?");
        assertThat(interaction.reply()).contains("75");
        System.out.println(interaction.reply());
    }

    @Test
    void should_notPersist_when_amountIsMissing() {
        var interaction = assistant.handleText("Fui na farmácia hoje");

        assertThat(repository.findAll()).isEmpty();
        System.out.println(interaction.reply());
    }

    @Test
    void should_runFullAudioFlow() {
        var interaction = assistant.handleAudio(new ClassPathResource("audio/recording-1.m4a"));

        assertThat(interaction.transcription()).contains("80 reais");
        assertThat(repository.findAll()).hasSize(1);
        assertThat(assistant.speak(interaction.reply())).hasSizeGreaterThan(1024);
    }
}
