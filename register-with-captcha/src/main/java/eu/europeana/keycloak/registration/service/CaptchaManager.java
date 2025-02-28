package eu.europeana.keycloak.registration.service;

import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.secret;
import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.verificationUrlHost;
import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.verificationUrlPath;
import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.verificationUrlScheme;


import eu.europeana.keycloak.registration.config.CaptachaManagerConfig;
import eu.europeana.keycloak.registration.exception.CaptchaException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
/* This service is same as capcha manager From apikey*/
public class CaptchaManager {
    private static final Logger LOG = Logger.getLogger(CaptchaManager.class);

    private final CloseableHttpClient httpClient;

    public CaptchaManager() {
        httpClient = HttpClients.createDefault();
    }

    /**
     * Verify Captcha token by sending request to the verification URL. Response is a JSON with
     * a field "success" indicating true or false. When it's false "error-codes" field contains
     * reason of failure.
     * @param captchaToken Token to be verified.
     * @return true when verification successful, false when there was problem with verification response
     */
    public boolean verifyCaptchaToken(String captchaToken)  {
        String verificationResponse = getVerificationResponse(captchaToken);
        LOG.debug("Captcha verification response = " + verificationResponse);
        if (verificationResponse != null) {
            JSONObject jsonObject = new JSONObject(verificationResponse);
            if (!jsonObject.getBoolean("success")) {
                JSONArray jsonArray = jsonObject.getJSONArray("error-codes");
                LOG.error("Captcha verification error: " + jsonArray.get(0));
                 throw new CaptchaException(CaptachaManagerConfig.RESUBMIT_CAPTCHA);
            }
            return true;
        }
        return false;
    }

    /**
     * Post token in the verification request to the verification URL. Return JSON response or null in case of any exception.
     *
     * @param captchaToken token to be verified
     * @return JSON response from the verification URL or null in case of any exception
     */
    private String getVerificationResponse(String captchaToken) {
        LOG.debug("Sending captcha verification...");
        try (CloseableHttpResponse response = httpClient.execute(new HttpPost(getVerificationURI(captchaToken)))) {
            LOG.debug("Received captcha verification");
            if (response.getStatusLine().getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
                return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            }
        } catch (URISyntaxException e) {
            LOG.error("Wrong URI syntax.", e);
        } catch (IOException e) {
            LOG.error("Captcha verification request failed.", e);
        }
        return null;
    }


    /**
     * Prepare URI for the request to the verification URL.
     *
     * @param captchaToken token to be used as the parameter.
     * @return URI for the request
     * @throws URISyntaxException -
     */
    private URI getVerificationURI(String captchaToken) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(verificationUrlScheme).setHost(verificationUrlHost).setPath(verificationUrlPath)
                .setParameter("secret", secret)
                .setParameter("response", captchaToken);
        return builder.build();
    }
}
