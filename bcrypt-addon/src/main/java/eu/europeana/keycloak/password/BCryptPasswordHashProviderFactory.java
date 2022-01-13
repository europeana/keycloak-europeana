package eu.europeana.keycloak.password;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for creating BCrypt password hashing provider
 * Created by luthien on 27/05/2020.
 */
public class BCryptPasswordHashProviderFactory implements PasswordHashProviderFactory {

    private static final Logger LOG = Logger.getLogger(BCryptPasswordHashProviderFactory.class);
    private static final String ID                 = "BCrypt";

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        LOG.debug("Creating BCryptPasswordHashProvider ...");
        return new BCryptPasswordHashProvider(ID, LOG);
    }

    @Override
    public void init(Config.Scope scope) {
        // no need to do anything
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // no need to do anything
    }

    @Override
    public void close() {
        // no need to do anything
    }

    @Override
    public String getId() {
        return ID;
    }
}
