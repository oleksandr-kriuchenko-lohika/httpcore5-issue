# httpcore5-issue
Reproduces an issue when apache http client hangs forever if a connection reset occurs during an 
asynchronous request execution.

## How to reproduce the issue
Run single test `ApacheHttpClient5TransportIT.connectionResetIsHandledSuccessfully`

## Expected behavior
Test passes successfully

## Actual behavior
Test execution hangs forever

## Test description
- ApacheHttpClient5TransportIT.regularRequestExecutesSuccessfully - successfully completes a test request
- ApacheHttpClient5TransportIT.connectionResetIsHandledSuccessfully - hangs forever for same request as connection is closed by server

## Observations
- Network traffic shows that mock server closes a connection immediately, however the future 
returned by `CloseableHttpAsyncClient.execute()` never completes.