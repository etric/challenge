package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MoneyTransfer {

  @NotNull
  @NotEmpty
  String fromAccountId;

  @NotNull
  @NotEmpty
  String toAccountId;

  @NotNull
  @Positive(message = "Transfer amount must be positive.")
  BigDecimal amount;

  @JsonCreator
  public MoneyTransfer(@JsonProperty("fromAccountId") String fromAccountId,
                       @JsonProperty("toAccountId") String toAccountId,
                       @JsonProperty("amount") BigDecimal amount) {
    this.fromAccountId = fromAccountId;
    this.toAccountId = toAccountId;
    this.amount = amount;
  }

}
