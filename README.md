# httpcore5-issue
Reproduces an issue when apache http client hangs forever if a connection reset occurs during an 
asynchronous request execution.

## How to reproduce the issue
Run single test `ApacheHttpClient5TransportIT.connectionResetIsHandledSuccessfully`

## Expected behavior
Test passes successfully

## Actual behavior
Test execution hangs forever

## Observations
- If both tests of the test suits are run the issue doesn't reproduce and all tests complete successfully
- Network traffic shows that mock server closes a connection immediately, however the future 
returned by `CloseableHttpAsyncClient.execute()` never completes.