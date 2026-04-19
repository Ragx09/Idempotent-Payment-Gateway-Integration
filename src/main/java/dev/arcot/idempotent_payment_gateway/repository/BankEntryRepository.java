package dev.gabriel.idempotent_payment_gateway.repository;

import dev.gabriel.idempotent_payment_gateway.model.entities.BankEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Ledger line persistence for the payment gateway: debit/credit entries tied to transactions and accounts.
 */
@Repository
public interface BankEntryRepository extends JpaRepository<BankEntry, UUID> {
}
