package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransfer;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MoneyTransferService {

  AccountsService accountsService;
  NotificationService notificationService;

  public void transferMoney(final MoneyTransfer moneyTransfer) {
    validateMoneyTransfer(moneyTransfer);

    final Account fromAccount = this.accountsService.getAccount(moneyTransfer.getFromAccountId());
    final Account toAccount = this.accountsService.getAccount(moneyTransfer.getToAccountId());

    final Object firstLock;
    final Object secondLock;

    /*
    Locking should be done on both accounts to perform safe transfer.
    More over the order of locking is important to avoid deadlock.

    If the condition below is commended out and locks aren't ordered:
      firstLock = fromAccount.getAccountId().intern();
      secondLock = toAccount.getAccountId().intern();
    Then existing tests will hang due to the deadlock.
    */
    if (fromAccount.getAccountId().compareTo(toAccount.getAccountId()) > 0) {
      firstLock = fromAccount.getAccountId().intern();
      secondLock = toAccount.getAccountId().intern();
    } else {
      firstLock = toAccount.getAccountId().intern();
      secondLock = fromAccount.getAccountId().intern();
    }

    synchronized (firstLock) {
      delay();
      synchronized (secondLock) {
        delay();
        final BigDecimal fromAccountNewBalance = fromAccount.getBalance().subtract(moneyTransfer.getAmount());
        if (fromAccountNewBalance.compareTo(BigDecimal.ZERO) < 0) {
          throw new InsufficientFundsException(fromAccount.getAccountId());
        }
        fromAccount.setBalance(fromAccountNewBalance);
        this.notificationService.notifyAboutTransfer(fromAccount,
                "sent " + moneyTransfer.getAmount() + " to " + toAccount.getAccountId());

        final BigDecimal toAccountNewBalance = toAccount.getBalance().add(moneyTransfer.getAmount());
        toAccount.setBalance(toAccountNewBalance);
        this.notificationService.notifyAboutTransfer(toAccount,
                "received " + moneyTransfer.getAmount() + " from " + fromAccount.getAccountId());
      }
    }
  }

  private void validateMoneyTransfer(final MoneyTransfer moneyTransfer) {
    if (moneyTransfer.getFromAccountId().equals(moneyTransfer.getToAccountId())) {
      throw new DuplicateAccountIdException("Cannot transfer money to the same account: "
              + moneyTransfer.getFromAccountId());
    }

    final Account fromAccount = this.accountsService.getAccount(moneyTransfer.getFromAccountId());
    if (fromAccount == null) {
      throw new AccountNotFoundException(moneyTransfer.getFromAccountId());
    }

    final Account toAccount = this.accountsService.getAccount(moneyTransfer.getToAccountId());
    if (toAccount == null) {
      throw new AccountNotFoundException(moneyTransfer.getToAccountId());
    }
  }

  // artificial delay to verify deadlock
  private void delay() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      // ignore
    }
  }
}
