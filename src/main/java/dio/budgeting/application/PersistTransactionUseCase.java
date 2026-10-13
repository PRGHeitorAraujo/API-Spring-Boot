package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class PersistTransactionUseCase {
    private final TransactionRepository transactionRepository;

    public PersistTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * As regras de validação ficam no domínio ({@link Transaction}). Se algo for inválido,
     * uma InvalidTransactionException é lançada antes de salvar. Quando a chamada vem da IA,
     * o Spring AI devolve a mensagem do erro para o modelo, que explica o problema ao usuário.
     */
    @Tool(name = "persist-transaction",
            description = "Registra um novo gasto. Chame uma vez para cada gasto mencionado pelo usuário.")
    public TransactionOutput execute(PersistTransactionInput input) {
        var transaction = new Transaction(input.description(), input.amount(), input.category());
        return TransactionOutput.from(transactionRepository.save(transaction));
    }
}
