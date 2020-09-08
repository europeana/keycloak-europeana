package eu.europeana.keycloak.password;

import org.apache.commons.codec.binary.Base64;

import org.jboss.logging.Logger;

import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCrypt;


/**
 * Created by luthien on 27/05/2020.
 */
public class BCryptPasswordHashProvider implements PasswordHashProvider  {

    private static final Logger LOG = Logger.getLogger(BCryptPasswordHashProvider.class);
    private final int    logRounds;
    private final String providerId;
    private final String pepper;


    public BCryptPasswordHashProvider(String providerId, int configuredLogRounds, String pepper) {
        LOG.debug("BCryptPasswordHashProvider created");
        System.out.println("BCryptPasswordHashProvider created");
        this.providerId     = providerId;
        this.logRounds      = configuredLogRounds;
        this.pepper         = pepper;
    }

    @Override
    public boolean policyCheck(PasswordPolicy passwordPolicy, PasswordCredentialModel credentialModel) {
        LOG.debug("BCryptPasswordHashProvider policy check");
        System.out.println("BCryptPasswordHashProvider policy check");
        int policyHashIterations = passwordPolicy.getHashIterations();
        if (policyHashIterations == -1) {
            policyHashIterations = logRounds;
        }

        return credentialModel.getPasswordCredentialData().getHashIterations() == policyHashIterations
               && providerId.equals(credentialModel.getPasswordCredentialData().getAlgorithm());
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        String encodedPassword = encode(rawPassword, iterations);

        // bcrypt salt is stored as part of the encoded password so no need to store salt separately
        return PasswordCredentialModel.createFromValues(providerId, new byte[0], iterations, encodedPassword);
    }

    @Override
    public String encode(String rawPassword, int iterations) {
        LOG.debug("BCryptPasswordHashProvider encoding password ...");
        System.out.println("BCryptPasswordHashProvider encoding password ...");
        String salt     = BCrypt.gensalt(logRounds);
        return getHash(rawPassword, salt);
    }

    private String getHash(String rawPassword, String salt) {
        LOG.debug("BCryptPasswordHashProvider adding salt and pepper ...");
        System.out.println("BCryptPasswordHashProvider adding salt and pepper ...");
        String pepperedPassword = rawPassword + pepper;
        String base64PepperedPw = new String(Base64.encodeBase64(pepperedPassword.getBytes(StandardCharsets.UTF_8)),
                                             StandardCharsets.UTF_8);
        return BCrypt.hashpw(base64PepperedPw, salt);
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credentialModel) {
        LOG.debug("BCryptPasswordHashProvider verifying password ...");
        System.out.println("BCryptPasswordHashProvider verifying password ...");
        return BCrypt.checkpw(rawPassword, credentialModel.getPasswordSecretData().getValue());
    }

    @Override
    public void close() {
        // no need to do anything
    }
}
