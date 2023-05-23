package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransfer;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyTransferServiceTest {

  @InjectMocks
  MoneyTransferService moneyTransferService;
  @Mock
  AccountsService accountsService;
  @Mock
  NotificationService notificationService;

  @Test
  void transferMoney_failsOnDuplicateId() {
    final MoneyTransfer moneyTransfer = new MoneyTransfer(
            "Id-001", "Id-001", new BigDecimal(100));

    try {
      this.moneyTransferService.transferMoney(moneyTransfer);
      fail();
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Cannot transfer money to the same account: Id-001");
      verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }
  }

  @Test
  void transferMoney_failsOnMissingFromAccount() {
    final MoneyTransfer moneyTransfer = new MoneyTransfer(
            "Id-001", "Id-002", new BigDecimal(100));
    try {
      this.moneyTransferService.transferMoney(moneyTransfer);
      fail();
    } catch (AccountNotFoundException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account Id-001 not found");
      verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }
  }

  @Test
  void transferMoney_failsOnMissingToAccount() {
    final Account fromAccount = mockNewAccount(100);

    final MoneyTransfer moneyTransfer = new MoneyTransfer(
            fromAccount.getAccountId(), "Id-111", new BigDecimal(100));
    try {
      this.moneyTransferService.transferMoney(moneyTransfer);
      fail();
    } catch (AccountNotFoundException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account Id-111 not found");
      verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }
  }

  @Test
  void transferMoney_failsOnInsufficientFunds() {
    final Account fromAccount = mockNewAccount(50);
    final Account toAccount = mockNewAccount(10);

    final MoneyTransfer moneyTransfer = new MoneyTransfer(
            fromAccount.getAccountId(), toAccount.getAccountId(), new BigDecimal(100));
    try {
      this.moneyTransferService.transferMoney(moneyTransfer);
      fail();
    } catch (InsufficientFundsException ex) {
      assertThat(ex.getMessage())
              .isEqualTo("Account " + fromAccount.getAccountId() + " has insufficient funds");
    }
  }

  @Test
  void transferSomeMoney() {
    final Account fromAccount = mockNewAccount(150);
    final Account toAccount = mockNewAccount(10);

    final MoneyTransfer moneyTransfer = new MoneyTransfer(
            fromAccount.getAccountId(), toAccount.getAccountId(), new BigDecimal(100));

    this.moneyTransferService.transferMoney(moneyTransfer);

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("50");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("110");

    verify(this.notificationService)
            .notifyAboutTransfer(eq(fromAccount), eq("sent 100 to " + toAccount.getAccountId()));
    verify(this.notificationService)
            .notifyAboutTransfer(eq(toAccount), eq("received 100 from " + fromAccount.getAccountId()));
  }

  @Test
  void transferAllMoney() {
    final Account fromAccount = mockNewAccount(150);
    final Account toAccount = mockNewAccount(10);

    final MoneyTransfer moneyTransfer = new MoneyTransfer(
      fromAccount.getAccountId(), toAccount.getAccountId(), new BigDecimal(150));

    this.moneyTransferService.transferMoney(moneyTransfer);

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("0");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("160");

    verify(this.notificationService)
      .notifyAboutTransfer(eq(fromAccount), eq("sent 150 to " + toAccount.getAccountId()));
    verify(this.notificationService)
      .notifyAboutTransfer(eq(toAccount), eq("received 150 from " + fromAccount.getAccountId()));
  }

  private Account mockNewAccount(int balance) {
    final String accountId = "Id-" + System.nanoTime();
    final Account account = new Account(accountId, new BigDecimal(balance));
    when(this.accountsService.getAccount(accountId)).thenReturn(account);
    return account;
  }
}
