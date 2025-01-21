package eu.europeana.keycloak.registration;

import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

public class MailService {
  private final KeycloakSession session;
  private final UserModel userModel;

  private static final String SEPARATOR = "===========================%n";
  private static final String APIKEY_USAGE = "The API key can be used for regular API request, see https://pro.europeana.eu/resources/apis/intro#access";

  private static final String MESSAGEFOOTER =
      "%n%n" +
          "Please keep a safe record of these key(s) and do not share them with third parties or expose it in user " +
          "interfaces or in markup, as the API key(s) are confidential and are for use by the client or user only." +
          "%n%n" +
          "Our technical documentation for all APIs is available at https://pro.europeana.eu/resources/apis which " +
          "includes an API console for testing and community developed libraries for a variety of programming languages." +
          "%n%n" +
          "Please join us in the Europeana API Forum (https://groups.google.com/forum/?pli=1#!forum/europeanaapi) " +
          "- to ask questions to us and other developers and to give us your feedback on our API. " +
          "You can also contact us directly by mailing api@europeana.eu " +
          "and we would be especially grateful if you would let us know about your implementation so that we can " +
          "feature it in our application gallery on Europeana Pro - https://pro.europeana.eu/resources/apps." +
          "%n%n" + "Best regards," + "%n" + "The Europeana API Team";

  public MailService(KeycloakSession session, UserModel user) {
    this.session = session;
    this.userModel = user;
  }

  public void sendEmailToUserWithApikey(String apikey) throws EmailException {
    DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
    String messageSubject = "Your Europeana API key";
    String messageBody = generateMessageForSendingApikey(apikey);
    senderProvider.send(session.getContext().getRealm().getSmtpConfig(),userModel,
        messageSubject, messageBody,messageBody);
  }

  public String generateMessageForSendingApikey(String apikey) {
    StringBuilder msg = new StringBuilder();
    msg.append(String.format("Dear %s %s,%n%nThank you for registering for the Europeana API.",
        userModel.getFirstName(),userModel.getLastName())).append("%n")
        .append("This is your Europeana API key: %n%n")
        .append(SEPARATOR)
        .append(String.format("API key: \t%s %n",apikey))
        .append(APIKEY_USAGE).append(".")
        .append(MESSAGEFOOTER);
    return msg.toString();
  }
}
