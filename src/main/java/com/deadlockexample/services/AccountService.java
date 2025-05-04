package com.deadlockexample.services;

import com.deadlockexample.domain.Account;
import com.deadlockexample.repo.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    //this method will cause deadlocks if two threads access the transfer method at the same time.
    //If both threads fetch and lock their first account row and then,
    // attempt to access the other, a deadlock is likely.
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }

    //How to Handle and Prevent Deadlocks

    //1st Method - Consistent Lock Ordering
    //Always access resources in the same order. In the above example,
    //Always load accounts in order of ID:

    @Transactional
    public void transfer1(Long fromId, Long toId, BigDecimal amount) {
        List<Account> accounts = accountRepository.findAllById(List.of(fromId, toId));
        Account from = fromId < toId ? accounts.get(0) : accounts.get(1);
        Account to = fromId < toId ? accounts.get(1) : accounts.get(0);

        accountRepository.save(from);
        accountRepository.save(to);
    }

    //2nd Method - Retry Logic on Deadlocks
    //Use Spring’s @Transactional with retry logic using @Retryable from Spring Retry:

    @Retryable(
            value = {DeadlockLoserDataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200))
    @Transactional
    public void transfer2(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }

    //3rd Method - Locking Strategies
    //Pessimistic locks prevent other transactions from accessing the locked rows until the transaction is complete.

    /*
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Account findByIdForUpdate(@Param("id") Long id);
     */

    @Transactional
    public void transfer3(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findByIdForUpdate(fromId);
        Account to = accountRepository.findByIdForUpdate(toId);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }
}
