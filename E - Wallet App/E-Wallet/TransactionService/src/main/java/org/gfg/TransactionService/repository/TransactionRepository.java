package org.gfg.TransactionService.repository;

import jakarta.transaction.Transactional;
import org.gfg.TransactionService.model.Transaction;
import org.gfg.TransactionService.model.TxnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Integer> {

    @Query("update transaction t set t.txnStatus=:txnStatus, t.txnMessage=:txnMessage where t.txnId=:txnId")
    @Transactional
    @Modifying
    void updateTransaction(String txnId, TxnStatus txnStatus, String txnMessage);

    List<Transaction> findBySenderIdOrReceiverId(String sender, String receiver);
}
