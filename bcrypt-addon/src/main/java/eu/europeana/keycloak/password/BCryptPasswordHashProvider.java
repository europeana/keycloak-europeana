package eu.europeana.keycloak.password;

import org.apache.commons.codec.binary.Base64;
import org.jboss.logging.Logger;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;

import java.nio.charset.StandardCharsets;

import at.favre.lib.crypto.bcrypt.BCrypt;


/**
 * Created by luthien on 27/05/2020.
 * used the implementation of Leroy Guillaume: https://github.com/leroyguillaume/keycloak-bcrypt
 */
public class BCryptPasswordHashProvider implements PasswordHashProvider {

    private static final Logger LOG = Logger.getLogger(BCryptPasswordHashProvider.class);
    private final        int    defaultIterations;
    private final        String providerId;
    private final        String pepper;

    public BCryptPasswordHashProvider(String providerId, int defaultIterations, String pepper) {
        LOG.debug("BCryptPasswordHashProvider created");
        LOG.debug("using providerID " + providerId + ", defaultIterations " + defaultIterations + ", pepper " + pepper);
        this.providerId = providerId;
        this.defaultIterations = defaultIterations;
        this.pepper = pepper;
    }

    @Override
    public boolean policyCheck(PasswordPolicy passwordPolicy, PasswordCredentialModel credentialModel) {
        LOG.debug("BCryptPasswordHashProvider policy check");
        int policyHashIterations = passwordPolicy.getHashIterations();
        if (policyHashIterations == -1) {
            policyHashIterations = defaultIterations;
        }

        return credentialModel.getPasswordCredentialData().getHashIterations() == policyHashIterations &&
               providerId.equals(credentialModel.getPasswordCredentialData().getAlgorithm());
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        String encodedPassword = encode(rawPassword, iterations);

        LOG.debug("BCryptPasswordHashProvider hashed: " + encodedPassword);

        // bcrypt salt is stored as part of the encoded password so no need to store salt separately
        return PasswordCredentialModel.createFromValues(providerId, new byte[0], iterations, encodedPassword);
    }

    @Override
    public String encode(String rawPassword, int iterations) {
        LOG.debug("BCryptPasswordHashProvider encoding password ..." + rawPassword + ", using " + iterations +
                  " iterations");
        int cost;
        if (iterations == -1) {
            cost = defaultIterations;
        } else {
            cost = iterations;
        }
        LOG.debug("BCryptPasswordHashProvider adding pepper ...");
        String pepperedPassword = rawPassword + pepper;
        String base64PepperedPw = new String(Base64.encodeBase64(pepperedPassword.getBytes(StandardCharsets.UTF_8)),
                                             StandardCharsets.UTF_8);
        LOG.debug("BCryptPasswordHashProvider base64PepperedPW = " + base64PepperedPw);

        return BCrypt.with(BCrypt.Version.VERSION_2Y).hashToString(cost, base64PepperedPw.toCharArray());
    }

    @Override
    public void close() {
        // no action required
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        LOG.debug("BCryptPasswordHashProvider verifying password ...");
        final String  hash     = credential.getPasswordSecretData().getValue();

        LOG.debug("Verifying: hash from cred object is: " + hash);

        BCrypt.Result verifier = BCrypt.verifyer().verify(rawPassword.toCharArray(), hash.toCharArray());

        LOG.debug("Verifier toString: " + verifier.toString());
        return verifier.verified;
    }

}
