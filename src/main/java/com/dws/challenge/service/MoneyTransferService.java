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

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MoneyTransferService {

  AccountsService accountsService;
  NotificationService notificationService;

  public void transferMoney(final MoneyTransfer moneyTransfer) {
    if (moneyTransfer.getFromAccountId().equals(moneyTransfer.getToAccountId())) {
      throw new DuplicateAccountIdException("Cannot transfer money to the same account: "
        + moneyTransfer.getFromAccountId());
    }

    final Account fromAccount = ofNullable(this.accountsService.getAccount(moneyTransfer.getFromAccountId()))
      .orElseThrow(() -> new AccountNotFoundException(moneyTransfer.getFromAccountId()));

    final Account toAccount = ofNullable(this.accountsService.getAccount(moneyTransfer.getToAccountId()))
      .orElseThrow(() -> new AccountNotFoundException(moneyTransfer.getToAccountId()));

    final Object firstLock;
    final Object secondLock;

    /*
    Locking should be done on both accounts to perform safe transfer.
    More over the order of locking is important to avoid deadlock.

    If the condition below is commended out and locks aren't ordered:
      firstLock = fromAccount.getAccountId().intern();
      secondLock = toAccount.getAccountId().intern();
    Then the code is deadlock-prone.
    */
    if (fromAccount.getAccountId().compareTo(toAccount.getAccountId()) > 0) {
      firstLock = fromAccount.getAccountId().intern();
      secondLock = toAccount.getAccountId().intern();
    } else {
      firstLock = toAccount.getAccountId().intern();
      secondLock = fromAccount.getAccountId().intern();
    }

    synchronized (firstLock) {
      synchronized (secondLock) {
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
}
