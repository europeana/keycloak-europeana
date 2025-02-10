package eu.europeana.keycloak.registration.service;

import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

public class MailService {
  private final KeycloakSession session;
  private final UserModel userModel;

  private static final String SEPARATOR = "===========================<br>";
  private static final String APIKEY_USAGE = "Visit <a href=\"https://apis.europeana.eu/\">Europeana’s APIs page</a> to get to know about the APIs and visit our <a href=\"https://europeana.atlassian.net/wiki/spaces/EF/pages/2462351393/Accessing+the+APIs\">documentation pages</a> to learn how to use your key.";

  private static final String MESSAGE_FOOTER =
      "<br><br>" +
          "Please keep a safe record of your individual key and do not share it with third parties or expose it in user " +
          "interfaces or in markup, as the key are confidential and are for use by yourself only." +
          "<br><br>" +
          "If you want to ask questions or give us your feedback, we are available directly via the email <a href=\"mailto:api@europeana.eu\">api@europeana.eu</a> ." +
          "<br><br>" + "Best regards," + "<br>" + "The Europeana API Team</body></html>";

  public MailService(KeycloakSession session, UserModel user) {
    this.session = session;
    this.userModel = user;
  }

  public void sendEmailToUserWithApikey(String apikey) throws EmailException {
    DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
    String messageSubject = "Your Europeana API key";
    String messageBody = generateMessageForSendingApikey(apikey);
    senderProvider.send(session.getContext().getRealm().getSmtpConfig(),userModel,
        messageSubject, null,messageBody);
  }

  public String generateMessageForSendingApikey(String apikey) {
    StringBuilder msg = new StringBuilder();
    msg.append(String.format("<html><body>Dear %s %s,<br><br>Thank you for your interest in the Europeana APIs and registering for a key.",
        userModel.getFirstName(),userModel.getLastName())).append("<br>")
        .append("You can now try out Europeana’s APIs with your very own API key: <br><br>")
        .append(SEPARATOR)
        .append(String.format("&emsp; &emsp; &emsp; %s <br>",apikey))
        .append(SEPARATOR).append("<br>")
        .append(APIKEY_USAGE).append(".")
        .append(MESSAGE_FOOTER);
    return msg.toString();
  }
}
