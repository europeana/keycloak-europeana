FROM jboss/keycloak:10.0.2

# Set workdir to jboss home
WORKDIR /opt/jboss/

# Set environment variables
ENV DB_VENDOR postgres

# Set Europeana theme
#ENV KEYCLOAK_DEFAULT_THEME europeana

# Note: credentials are used only when initialising a new empty DB
ENV KEYCLOAK_USER admin

ENV KEYCLOAK_PASSWORD: change-this-into-something-useful

# create user for accessing Wildfly metrics.
# only used if EUROPEANA_JBOSS_ADMIN_PASSWORD is set (see custom-scripts/add-wildfly-mgmt-user.sh)
ENV EUROPEANA_JBOSS_ADMIN_USER admin

# Copy commons-codec, favre-crypto & -bytes (BCrypt dependencies) to keycloak/modules
COPY bcrypt-dependencies keycloak/modules

# Copy addons to the Wildfly deployment directory
COPY addon-jars keycloak/standalone/deployments

# Copy Europeana theme to keycloak/themes
RUN mkdir -p keycloak/themes/europeana
COPY keycloak-theme keycloak/themes/europeana

# Copy log formatter script
COPY custom-scripts/ /opt/jboss/startup-scripts/

USER jboss