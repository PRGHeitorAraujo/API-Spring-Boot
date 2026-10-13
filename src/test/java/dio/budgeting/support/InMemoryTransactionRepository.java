package dio.budgeting.support;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Repositório fake para testar use cases e controllers sem banco de dados. */
public class InMemoryTransactionRepository implements TransactionRepository {
    private static final Comparator<Transaction> NEWEST_FIRST =
            Comparator.comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactions.stream().filter(t -> t.getCategory() == category).sorted(NEWEST_FIRST).toList();
    }

    @Override
    public List<Transaction> findAll() {
        return transactions.stream().sorted(NEWEST_FIRST).toList();
    }

    @Override
    public List<Transaction> findAllCreatedSince(Instant since) {
        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(since))
                .sorted(NEWEST_FIRST)
                .toList();
    }

    public int size() {
        return transactions.size();
    }
}
