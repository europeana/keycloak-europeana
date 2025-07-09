package eu.europeana.keycloak;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;

public class SlackConnection {
  private static final Logger LOG         = Logger.getLogger(SlackConnection.class);

  private String slackWebhook;

  /**
   * Construct slack connection instance with webhook
   */
  public SlackConnection(String slackWebhook) {
    this.slackWebhook = slackWebhook;
  }
  /**
   * Sends the message to configured slack channel.
   * @param message - message body
   */
  public  void publishStatusReport(String message) {
    LOG.info("Sending Slack Message : " + message);
    try {
      String slackWebhookApiAutomation = System.getenv(slackWebhook);
      if (StringUtils.isBlank(slackWebhookApiAutomation)) {
        LOG.error("Slack webhook not configured, status report will not be published over Slack.");
        return;
      }
      HttpPost httpPost = new HttpPost(slackWebhookApiAutomation);
      httpPost.setEntity(new StringEntity(message));
      httpPost.setHeader("Accept", "application/json");
      httpPost.setHeader("Content-type", "application/json");
      try (CloseableHttpClient httpClient = HttpClients.createDefault();
          CloseableHttpResponse response = httpClient.execute(httpPost)) {
        LOG.info("Received status " + response.getStatusLine().getStatusCode()
            + " while calling slack!");
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          LOG.info(" Successfully sent slack message !");
        }
      }
    } catch (IOException e) {
      LOG.error("Exception occurred while sending slack message !! " + e.getMessage());
    }
  }
}