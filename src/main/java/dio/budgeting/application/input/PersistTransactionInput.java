package dio.budgeting.application.input;

import dio.budgeting.domain.Category;
import org.springframework.ai.tool.annotation.ToolParam;

public record PersistTransactionInput(
        @ToolParam(description = "Descrição curta do gasto, por exemplo: 'Compras no mercado'") String description,
        @ToolParam(description = "Valor do gasto em CENTAVOS. Ex.: 80 reais = 8000; 12 reais e 50 centavos = 1250") long amount,
        @ToolParam(description = "Categoria que melhor descreve o gasto") Category category) {
}
