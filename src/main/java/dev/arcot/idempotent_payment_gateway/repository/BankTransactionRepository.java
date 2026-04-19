package dev.gabriel.idempotent_payment_gateway.repository;

import dev.gabriel.idempotent_payment_gateway.model.entities.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Transaction header persistence for idempotent payment processing and webhook-driven credits.
 */
@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {
}
