package eu.europeana.keycloak.registration.config;

public class CaptachaManagerConfig {
  public static final String verificationUrlScheme = System.getenv("RECAPTCHA_VERIFICATION_URL_SCHEME");
  public static final String verificationUrlHost  = System.getenv("RECAPTCHA_VERIFICATION_URL_HOST");
  public static final String verificationUrlPath = System.getenv("RECAPTCHA_VERIFICATION_URL_PATH");
  public static final String  secret = System.getenv("RECAPTCHA_SECRET");
  public static final String CAPTCHA_PATTERN                       = "Bearer\\s+([^\\s]+)";
  public static final String CAPTCHA_MISSING                       = "Missing or malformed Captcha token. Correct syntax header is: Authorization: Bearer CAPTCHA_TOKEN";
  public static final String CAPTCHA_VERIFICATION_FAILED           = "Captcha verification failed.";

}
