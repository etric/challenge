import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.HashMap;

// This map will store account locks. It should be a concurrent map in production.
private final Map<String, Lock> locks = new HashMap<>();

public void transferMoney(final MoneyTransfer moneyTransfer) {
    validateMoneyTransfer(moneyTransfer);

    final Account fromAccount = this.accountsService.getAccount(moneyTransfer.getFromAccountId());
    final Account toAccount = this.accountsService.getAccount(moneyTransfer.getToAccountId());

    final Lock firstLock = getLockForAccount(fromAccount.getAccountId());
    final Lock secondLock = getLockForAccount(toAccount.getAccountId());

    try {
        // We need to lock both accounts, in a fixed order to prevent deadlocks
        lockAccounts(firstLock, secondLock);

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
    } finally {
        // Make sure to unlock both locks, regardless of what happens in the try block
        firstLock.unlock();
        secondLock.unlock();
    }
}

// Get a lock for an account. This will create a new lock if necessary.
private Lock getLockForAccount(String accountId) {
    Lock lock = locks.get(accountId);
    if (lock == null) {
        synchronized (locks) {
            lock = locks.get(accountId);
            if (lock == null) {
                lock = new ReentrantLock();
                locks.put(accountId, lock);
            }
        }
    }
    return lock;
}

private void lockAccounts(Lock firstLock, Lock secondLock) {
    while (true) {
        // Try to get the first lock
        boolean gotFirstLock = false;
        try {
            gotFirstLock = firstLock.tryLock();
            if (gotFirstLock) {
                // If we got the first lock, try to get the second
                if (secondLock.tryLock()) {
                    return;
                }
            }
        } finally {
            // If we only got the first lock and failed to get the second, release the first
            if (gotFirstLock) {
                firstLock.unlock();
            }
        }
        // If we couldn't get both locks, wait a bit and try again
        Thread.sleep(1);
    }
}
