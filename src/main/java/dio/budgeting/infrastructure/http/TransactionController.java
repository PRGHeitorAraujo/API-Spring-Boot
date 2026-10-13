package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListRecentTransactionsUseCase;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST "clássicos" (sem IA). Usam exatamente os mesmos use cases que a IA usa via tool calling.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final ListRecentTransactionsUseCase listRecentTransactionsUseCase;
    private final SummarizeExpensesUseCase summarizeExpensesUseCase;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 ListRecentTransactionsUseCase listRecentTransactionsUseCase,
                                 SummarizeExpensesUseCase summarizeExpensesUseCase) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.listRecentTransactionsUseCase = listRecentTransactionsUseCase;
        this.summarizeExpensesUseCase = summarizeExpensesUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionRequest request) {
        return TransactionResponse.from(persistTransactionUseCase.execute(request.toInput()));
    }

    /** Últimas transações de todas as categorias. */
    @GetMapping
    public List<TransactionResponse> listRecent(@RequestParam(required = false) Integer limit) {
        return listRecentTransactionsUseCase.execute(limit).stream().map(TransactionResponse::from).toList();
    }

    /** Resumo de gastos (total e por categoria). Ex.: GET /transactions/summary?lastDays=30 */
    @GetMapping("/summary")
    public ExpenseSummaryOutput summary(@RequestParam(required = false) Integer lastDays) {
        return summarizeExpensesUseCase.execute(lastDays);
    }

    @GetMapping("/categories")
    public List<CategoryInfo> categories() {
        return java.util.Arrays.stream(Category.values())
                .map(c -> new CategoryInfo(c.name(), c.getLabel()))
                .toList();
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream().map(TransactionResponse::from).toList();
    }

    public record CategoryInfo(String name, String label) {
    }
}
