package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private final TransactionEntityRepository transactionEntityRepository;

    public JpaTransactionRepository(TransactionEntityRepository transactionEntityRepository) {
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        var entity = TransactionEntity.from(transaction);
        return transactionEntityRepository.save(entity).toDomain();
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return toDomain(transactionEntityRepository.findAllByCategoryOrderByCreatedAtDesc(category));
    }

    @Override
    public List<Transaction> findAll() {
        return toDomain(transactionEntityRepository.findAllByOrderByCreatedAtDesc());
    }

    @Override
    public List<Transaction> findAllCreatedSince(Instant since) {
        return toDomain(transactionEntityRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(since));
    }

    private static List<Transaction> toDomain(List<TransactionEntity> entities) {
        return entities.stream().map(TransactionEntity::toDomain).toList();
    }
}
