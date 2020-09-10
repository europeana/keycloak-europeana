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

    private static int defaultLogRounds = 13;
    private static int minLogRounds     = 4;
    private static int maxlogRounds     = 31;
    private static String pepper = "";

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
            defaultLogRounds = Integer.parseInt(properties.getProperty("bcrypt.default-log-rounds"));
            minLogRounds = Integer.parseInt(properties.getProperty("bcrypt.min-log-rounds"));
            maxlogRounds = Integer.parseInt(properties.getProperty("bcrypt.max-log-rounds"));
        } catch (Exception e) {
            LOG.error("Exception reading BCrypt properties", e);
        }
    }

    private int configuredLogRounds = defaultLogRounds;

    @Override
    public PasswordHashProvider create(KeycloakSession keycloakSession) {
        LOG.debug("Creating BCryptPasswordHashProvider ...");
        return new BCryptPasswordHashProvider(ID, configuredLogRounds, pepper);
    }

    @Override
    public void init(Config.Scope scope) {
        LOG.debug("Initialising BCryptPasswordHashProviderFactory ...");
        Integer configLogRounds = scope.getInt("log-rounds");
        if (configLogRounds != null && configLogRounds >= minLogRounds && configLogRounds <= maxlogRounds) {
            configuredLogRounds = configLogRounds;
        }
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
