package dio.budgeting.domain;

/**
 * Lançada quando uma transação viola alguma regra de negócio.
 * A mensagem é escrita para ser lida por pessoas (e pela IA, que repassa o erro ao usuário).
 */
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
