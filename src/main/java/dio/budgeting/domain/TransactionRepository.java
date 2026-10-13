package dio.budgeting.domain;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    /** Transações da categoria, mais recentes primeiro. */
    List<Transaction> findAllByCategory(Category category);

    /** Todas as transações, mais recentes primeiro. */
    List<Transaction> findAll();

    /** Transações criadas a partir do instante informado (inclusive), mais recentes primeiro. */
    List<Transaction> findAllCreatedSince(Instant since);
}
