package eu.europeana.keycloak.registration.config;

/**
 * Provides the static configuration for the reCAPTCHA verification.
 */
public final class CaptachaManagerConfig {
  public static final String VERIFICATION_URL_SCHEME = System.getenv("RECAPTCHA_VERIFICATION_URL_SCHEME");
  public static final String VERIFICATION_URL_HOST = System.getenv("RECAPTCHA_VERIFICATION_URL_HOST");
  public static final String VERIFICATION_URL_PATH = System.getenv("RECAPTCHA_VERIFICATION_URL_PATH");
  public static final String SECRET = System.getenv("RECAPTCHA_SECRET");
  public static final String CAPTCHA_PATTERN = "Bearer\\s+([^\\s]+)";
  public static final String RESUBMIT_CAPTCHA = "Please indicate that you are not a robot (once again)";

  private CaptachaManagerConfig(){}
}