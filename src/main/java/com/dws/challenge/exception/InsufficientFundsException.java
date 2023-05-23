package com.dws.challenge.exception;

public class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException(final String accountId) {
    super("Account " + accountId + " has insufficient funds");
  }
}
