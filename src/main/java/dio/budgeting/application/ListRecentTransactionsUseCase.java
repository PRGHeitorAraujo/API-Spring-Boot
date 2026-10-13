package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListRecentTransactionsUseCase {
    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 50;

    private final TransactionRepository transactionRepository;

    public ListRecentTransactionsUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "list-recent-transactions",
            description = "Lista os últimos gastos registrados, de qualquer categoria, do mais recente para o mais antigo. Valores em reais.")
    public List<TransactionOutput> execute(
            @ToolParam(description = "Quantidade máxima de gastos a retornar (padrão 5, máximo 50)", required = false)
            Integer limit) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        return transactionRepository.findAll().stream()
                .limit(effectiveLimit)
                .map(TransactionOutput::from)
                .toList();
    }
}
