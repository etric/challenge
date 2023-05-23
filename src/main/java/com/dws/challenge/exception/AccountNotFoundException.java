package com.dws.challenge.exception;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(final String accountId) {
    super("Account " + accountId + " not found");
  }
}
