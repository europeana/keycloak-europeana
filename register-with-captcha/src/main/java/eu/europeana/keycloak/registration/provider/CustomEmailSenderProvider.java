package eu.europeana.keycloak.registration.provider;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.utils.StringUtil;
import org.keycloak.vault.VaultStringSecret;

/**
 *This provider class similar to default email provider ,
 * has additional feature to set CC or BCC addresses in the email
 */
public class CustomEmailSenderProvider implements EmailSenderProvider {
  private final KeycloakSession session;

  private String addressForCC;
  private String addressForBCC;

  public String getAddressForCC() {
    return addressForCC;
  }
  public void setAddressForCC(String addressForCC) {
    this.addressForCC = addressForCC;
  }
  public String getAddressForBCC() {
    return addressForBCC;
  }
  public void setAddressForBCC(String addressForBCC) {
    this.addressForBCC = addressForBCC;
  }
  public CustomEmailSenderProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public void send(Map<String, String> config, String address, String subject, String textBody,
      String htmlBody) throws EmailException {
    boolean auth = "true".equals(config.get("auth"));
    Properties props = getMailProperties(config, auth);
    Session sessionInstance = Session.getInstance(props);
    try (Transport transport = sessionInstance.getTransport("smtp")) {
      Multipart multipart = getMultipartMessageBody(textBody, htmlBody);
      Message msg = getMessage(config, address, subject, sessionInstance, multipart);
      if (auth) {
        connectWithAuth(config, transport);
      } else {
        transport.connect();
      }
      transport.sendMessage(msg,msg.getAllRecipients());
    } catch (Exception ex) {
      ServicesLogger.LOGGER.failedToSendEmail(ex);
      throw new EmailException(ex);
    }
  }

  private void connectWithAuth(Map<String, String> config, Transport transport)
      throws MessagingException {
    try(VaultStringSecret vaultStringSecret = this.session.vault().getStringSecret(config.get("password"))) {
        transport.connect(config.get("user"),vaultStringSecret.get().orElse(config.get("password")));
    }
  }

  private static Properties getMailProperties(Map<String, String> config, boolean auth) {
    Properties props = new Properties();
    if (config.containsKey("host")) {
      props.setProperty("mail.smtp.host", config.get("host"));
    }
    if (config.containsKey("port") && config.get("port") != null) {
      props.setProperty("mail.smtp.port", config.get("port"));
    }
    if (auth) {
      props.setProperty("mail.smtp.auth", "true");
    }
    //skipping the processing of 'ssl' & 'starttls'  properties as they are currently not set for production .
    // See table realm_smtp_config for setup

    props.setProperty("mail.smtp.timeout", "10000");
    props.setProperty("mail.smtp.connectiontimeout", "10000");
    String envelopeFrom =  config.get("envelopeFrom");
    if (StringUtil.isNotBlank(envelopeFrom)) {
      props.setProperty("mail.smtp.from", envelopeFrom);
    }
    return props;
  }

  private Message getMessage(Map<String, String> config, String address, String subject,
      Session session, Multipart multipart)
      throws MessagingException, UnsupportedEncodingException, EmailException {
    String from = config.get("from");
    String fromDisplayName = config.get("fromDisplayName");
    String replyTo = config.get("replyTo");
    String replyToDisplayName = config.get("replyToDisplayName");

    Message msg = new MimeMessage(session);
    msg.setFrom(this.toInternetAddress(from, fromDisplayName));
    msg.setReplyTo(new Address[]{this.toInternetAddress(from, fromDisplayName)});
    if (StringUtil.isNotBlank(replyTo)) {
      msg.setReplyTo(new Address[]{this.toInternetAddress(replyTo, replyToDisplayName)});
    }
    msg.setSubject(MimeUtility.encodeText(subject, StandardCharsets.UTF_8.name(), null));
    msg.setContent(multipart);
    msg.setSentDate(new Date());
    msg.setRecipient(RecipientType.TO,this.toInternetAddress(address,null));
    if(this.getAddressForCC()!=null && !this.getAddressForCC().isBlank())
       msg.setRecipient(RecipientType.BCC,this.toInternetAddress(getAddressForCC(),null));
    if(this.getAddressForBCC()!=null && !this.getAddressForBCC().isBlank())
      msg.setRecipient(RecipientType.BCC,this.toInternetAddress(getAddressForBCC(),null));
    return msg;
  }

  private static Multipart getMultipartMessageBody(String textBody, String htmlBody)
      throws MessagingException {
    Multipart multipart = new MimeMultipart("alternative");
    MimeBodyPart htmlPart;

    if (textBody != null) {
      htmlPart = new MimeBodyPart();
      htmlPart.setText(textBody, "UTF-8");
      multipart.addBodyPart(htmlPart);
    }

    if (htmlBody != null) {
      htmlPart = new MimeBodyPart();
      htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);
    }
    return multipart;
  }

  protected InternetAddress toInternetAddress(String email, String displayName) throws UnsupportedEncodingException, AddressException, EmailException {
    if (email != null && !email.trim().isEmpty()) {
      return displayName != null && !displayName.trim().isEmpty() ? new InternetAddress(email, displayName, "utf-8") : new InternetAddress(email);
    } else {
      throw new EmailException("Please provide a valid address", null);
    }
  }

  @Override
  public void close() {
    // not required
  }
}
