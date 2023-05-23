# Things to improve

- Instead of using in-memory storage, an external one should be used (e.g. DB) 
- Despite locking mechanism works with a single JVM, it's not reliable when running multiple instances of the service (distributed lock)
- Add rollback functionality to avoid partial update if the failure happens in the middle of transferMoney transaction
- Since current locking implementation doesn't guarantee that lock will be acquired by the waiting thread in the reasonable amount of time,
an expiration to acquire the lock might be introduced (e.g. lock.tryLock())
- Exception handling logic in Controllers should be moved to the Spring's ControllerAdvice with corresponding ExceptionHandlers
- I'd consider having parametrized test for AccountsControllerTest when it comes to the input validation (empty/missing fields, invalid content) 
since the number of similar tests grows with each new endpoint added (having request body to be validated)   
- When possible, I try to cover service layer with unit test separately mocking all dependencies, including Repositories (AccountsServiceTest)
- It's often not a good idea to expose Entities in representation layer (e.g. Account) 
So when Account become an Entity then Controller shouldn't work with it directly, but via some AccountDTO
- Increase observability (tracing, logging, app metrics)
- Add authentication/authorization
- Externalize required application-specific properties
