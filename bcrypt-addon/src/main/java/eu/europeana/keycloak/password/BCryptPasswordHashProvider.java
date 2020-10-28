package eu.europeana.keycloak.password;

import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategy;
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

        // bcrypt salt is stored as part of the encoded password so no need to store salt separately
        return PasswordCredentialModel.createFromValues(providerId, new byte[0], iterations, encodedPassword);
    }

    @Override
    public String encode(String rawPassword, int iterations) {
        int cost;
        if (iterations == -1) {
            cost = defaultIterations;
        } else {
            cost = iterations;
        }
        String base64PepperedPw = pepperer(rawPassword);
        return BCrypt.with(BCrypt.Version.VERSION_2Y, LongPasswordStrategies.truncate(BCrypt.Version.VERSION_2Y))
                     .hashToString(cost, base64PepperedPw.toCharArray());
    }

    @Override
    public void close() {
        // no action required
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        LOG.debug("BCryptPasswordHashProvider verifying password ...");
        final String  hash             = credential.getPasswordSecretData().getValue();
        String        base64PepperedPw = pepperer(rawPassword);
        BCrypt.Result verifier         = BCrypt.verifyer(BCrypt.Version.VERSION_2Y,
                                                         LongPasswordStrategies.truncate(BCrypt.Version.VERSION_2Y))
                                               .verify(base64PepperedPw.toCharArray(), hash.toCharArray());
        return verifier.verified;
    }

    private String pepperer(String rawPassword) {
        LOG.debug("BCryptPasswordHashProvider adding pepper ...");
        String pepperedPassword = rawPassword + pepper;
        return new String(Base64.encodeBase64(pepperedPassword.getBytes(StandardCharsets.UTF_8)),
                          StandardCharsets.UTF_8);
    }

}
