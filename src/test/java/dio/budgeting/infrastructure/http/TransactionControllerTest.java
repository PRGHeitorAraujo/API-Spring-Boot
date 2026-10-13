package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListRecentTransactionsUseCase;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa os endpoints REST com os use cases reais e um repositório em memória
 * (sem banco e sem OpenAI). Usa MockMvc em modo standalone.
 */
class TransactionControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var repository = new InMemoryTransactionRepository();
        var controller = new TransactionController(
                new PersistTransactionUseCase(repository),
                new ListTransactionsByCategoryUseCase(repository),
                new ListRecentTransactionsUseCase(repository),
                new SummarizeExpensesUseCase(repository, Clock.systemUTC()));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void should_createTransaction_andReturnAmountInReais() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Compras no mercado", "category": "GROCERIES", "amount": 8000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.amount").value(80.0))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void should_return400_when_requestIsInvalid() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "", "category": "GROCERIES", "amount": -10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail", containsString("amount deve ser maior que zero")))
                .andExpect(jsonPath("$.detail", containsString("description é obrigatória")));
    }

    @Test
    void should_return400_when_categoryDoesNotExist() throws Exception {
        mockMvc.perform(get("/transactions/PIZZA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("GROCERIES")));
    }

    @Test
    void should_listByCategory_andSummarize() throws Exception {
        create("Mercado", "GROCERIES", 8000);
        create("Padaria", "GROCERIES", 1250);
        create("Farmácia", "PHARMA", 4000);

        mockMvc.perform(get("/transactions/GROCERIES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/transactions").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/transactions/summary").param("lastDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(3))
                .andExpect(jsonPath("$.total").value(132.5))
                .andExpect(jsonPath("$.byCategory[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$.byCategory[0].total").value(92.5));
    }

    @Test
    void should_return400_when_summaryPeriodIsInvalid() throws Exception {
        mockMvc.perform(get("/transactions/summary").param("lastDays", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_listCategories() throws Exception {
        mockMvc.perform(get("/transactions/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("GROCERIES"))
                .andExpect(jsonPath("$[0].label").value("Mercado"));
    }

    private void create(String description, String category, long amount) throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"%s\", \"category\": \"%s\", \"amount\": %d}"
                                .formatted(description, category, amount)))
                .andExpect(status().isCreated());
    }
}
