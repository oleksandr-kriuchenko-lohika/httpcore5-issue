import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.Test;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpError;

public class ApacheHttpClient5TransportIT {

  private CloseableHttpAsyncClient buildClient() {
    return HttpAsyncClients.createDefault();
  }

  private <R> R withHttpClient(Function<CloseableHttpAsyncClient, R> f)
      throws IOException {
    try (CloseableHttpAsyncClient httpclient = buildClient()) {
      httpclient.start();
      return f.apply(httpclient);
    }
  }

  private Future<SimpleHttpResponse> sendRequest(String host, int port, String path)
      throws IOException {
    return withHttpClient(httpClient -> {
      final HttpHost target = new HttpHost(host, port);
      final SimpleHttpRequest request = SimpleRequestBuilder.get()
          .setHttpHost(target)
          .setPath(path)
          .build();
      return httpClient.execute(
          SimpleRequestProducer.create(request),
          SimpleResponseConsumer.create(),
          new FutureCallback<SimpleHttpResponse>() {

            @Override
            public void completed(final SimpleHttpResponse response) {
              System.out.println(request + "->" + new StatusLine(response));
              System.out.println(response.getBody());
            }

            @Override
            public void failed(final Exception ex) {
              System.out.println(request + "->" + ex);
            }

            @Override
            public void cancelled() {
              System.out.println(request + " cancelled");
            }

          });
    });
  }

  private ClientAndServer createProxyMockServer(
      final String targetHost, final int targetPort, final int mockServerPort) {
    ConfigurationProperties.logLevel("INFO");
    final ClientAndServer mockServer =
        ClientAndServer.startClientAndServer(targetHost, targetPort, mockServerPort);
    Runtime.getRuntime().addShutdownHook(new Thread(mockServer::stop));
    return mockServer;
  }

  @Test
  public void regularRequestExecutesSuccessfully()
      throws IOException, ExecutionException, InterruptedException {
    Future<SimpleHttpResponse> future = sendRequest("httpbin.org", 80, "/");
    SimpleHttpResponse response = future.get();
    assertEquals("Response status code is 200", 200, response.getCode());
  }

  /**
   * If this test is run alone then execution hangs forever. Otherwise, it passes successfully.
   */
  @Test
  public void connectionResetIsHandledSuccessfully()
      throws IOException, ExecutionException, InterruptedException {
    ClientAndServer dbMockServer = createProxyMockServer("httpbin.org", 80, 1080);
    dbMockServer
        .when(request(), Times.unlimited())
        .error(HttpError.error().withDropConnection(true));

    Future<SimpleHttpResponse> future = sendRequest("httpbin.org", 1080, "/");
    assertThrows(ExecutionException.class, future::get);
  }
}
