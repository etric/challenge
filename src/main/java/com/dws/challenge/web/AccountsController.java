package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransfer;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.MoneyTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountsController {

  private final AccountsService accountsService;
  private final MoneyTransferService moneyTransferService;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
      this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping(path = "/transferMoney", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferMoney(@RequestBody @Valid MoneyTransfer moneyTransfer) {
    log.info("Transferring {} from account {} to account {}", moneyTransfer.getAmount(),
            moneyTransfer.getFromAccountId(), moneyTransfer.getToAccountId());

    try {
      this.moneyTransferService.transferMoney(moneyTransfer);
    } catch (AccountNotFoundException nfe) {
      return new ResponseEntity<>(nfe.getMessage(), HttpStatus.NOT_FOUND);
    } catch (DuplicateAccountIdException | InsufficientFundsException ex) {
      return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }
}
