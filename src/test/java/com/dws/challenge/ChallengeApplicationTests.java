package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class ChallengeApplicationTests {

	private MockMvc mockMvc;

	@Autowired
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void concurrentTwoAccountsMutualMoneyTransfer() {
		createAccount("Id-123", "1000");
		createAccount("Id-456", "2000");

		CompletableFuture.allOf(
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "200")),
			runAsync(transferMoneyRunnable("Id-456", "Id-123", "300"))
		).join();

		assertThat(getAccountBalance("Id-123")).isEqualByComparingTo("1100");
		assertThat(getAccountBalance("Id-456")).isEqualByComparingTo("1900");
	}

	@Test
	void concurrentThreeAccountsMutualMoneyTransfer() {
		createAccount("Id-123", "1000");
		createAccount("Id-456", "2000");
		createAccount("Id-789", "3000");

		CompletableFuture.allOf(
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "500")),
			runAsync(transferMoneyRunnable("Id-456", "Id-789", "600")),
			runAsync(transferMoneyRunnable("Id-789", "Id-123", "800"))
		).join();

		assertThat(getAccountBalance("Id-123")).isEqualByComparingTo("1300");
		assertThat(getAccountBalance("Id-456")).isEqualByComparingTo("1900");
		assertThat(getAccountBalance("Id-789")).isEqualByComparingTo("2800");
	}

	@Test
	void concurrentTransferAllMoneyInChunks() {
		createAccount("Id-123", "1000");
		createAccount("Id-456", "2000");
		createAccount("Id-789", "3000");

		CompletableFuture.allOf(
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "100")),
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "300")),
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "200")),
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "150")),
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "50")),
			runAsync(transferMoneyRunnable("Id-123", "Id-456", "200")),
			runAsync(transferMoneyRunnable("Id-789", "Id-456", "1500")),
			runAsync(transferMoneyRunnable("Id-789", "Id-456", "500")),
			runAsync(transferMoneyRunnable("Id-789", "Id-456", "100")),
			runAsync(transferMoneyRunnable("Id-789", "Id-456", "100")),
			runAsync(transferMoneyRunnable("Id-789", "Id-456", "800"))
		).join();

		assertThat(getAccountBalance("Id-123")).isEqualByComparingTo("0");
		assertThat(getAccountBalance("Id-456")).isEqualByComparingTo("6000");
		assertThat(getAccountBalance("Id-789")).isEqualByComparingTo("0");
	}

	private void createAccount(String accountId, String balance) {
		this.accountsService.createAccount(new Account(accountId, new BigDecimal(balance)));
	}

	private BigDecimal getAccountBalance(String accountId) {
		return this.accountsService.getAccount(accountId).getBalance();
	}

	private Runnable transferMoneyRunnable(String fromAccountId, String toAccountId, String amount) {
		return () -> {
			try {
				final String content = "{\"fromAccountId\":\"" + fromAccountId
					+ "\",\"toAccountId\":\"" + toAccountId
					+ "\",\"amount\":" + amount + "}";
				this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
					.content(content))
					.andDo(MockMvcResultHandlers.print())
					.andExpect(status().isOk());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
}
