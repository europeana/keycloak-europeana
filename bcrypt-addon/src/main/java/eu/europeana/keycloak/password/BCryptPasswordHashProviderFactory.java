package eu.europeana.keycloak.password;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Factory for creating BCrypt password hashing provider
 * Created by luthien on 27/05/2020.
 */
public class BCryptPasswordHashProviderFactory implements PasswordHashProviderFactory {

    private static final Logger LOG = Logger.getLogger(BCryptPasswordHashProviderFactory.class);

    private static final String ID                 = "BCrypt";
    private static final String PROPERTY_FILE      = "/bcrypt.properties";
    private static final String PROPERTY_USER_FILE = "bcrypt-user.properties";

    private static int    defaultIterations = 13;
    private static String pepper            = "";

    static {
        try {
            Properties  properties = new Properties();
            InputStream is;
            is = BCryptPasswordHashProviderFactory.class.getResourceAsStream(PROPERTY_FILE);
            if (null == is) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTY_USER_FILE);
                if (null == is) {
                    throw new FileNotFoundException(PROPERTY_FILE + " nor " + PROPERTY_USER_FILE +
                                                    " found; therefore, BCrypt will use an empty pepper!");
                }
            }
            properties.load(is);
            pepper = properties.getProperty("bcrypt.pepper");
            defaultIterations = Integer.parseInt(properties.getProperty("bcrypt.default-iterations"));
        } catch (Exception e) {
            LOG.error("Exception reading BCrypt properties", e);
        }
    }

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        LOG.debug("Creating BCryptPasswordHashProvider ...");
        return new BCryptPasswordHashProvider(ID, defaultIterations, pepper);
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
