FROM jboss/keycloak:10.0.2

# Set workdir to jboss home
WORKDIR /opt/jboss/

# Set environment variables
ENV DB_VENDOR postgres

# Set Europeana theme
#ENV KEYCLOAK_DEFAULT_THEME europeana

# Note: credentials are used only when initialising a new empty DB
ENV KEYCLOAK_USER: admin

ENV KEYCLOAK_PASSWORD: change-this-into-something-useful

# Copy commons-codec, favre-crypto & -bytes (BCrypt dependencies) to keycloak/modules
COPY bcrypt-dependencies keycloak/modules

# Copy addons to the Wildfly deployment directory
COPY addon-jars keycloak/standalone/deployments

# Copy Europeana theme to keycloak/themes
RUN mkdir -p keycloak/themes/europeana
COPY keycloak-theme keycloak/themes/europeana

# Copy translated message files into the theme keycloak/themes/europeana/account/messages ~/login/messages
RUN rm keycloak/themes/europeana/account/messages/messages_en.properties
RUN rm keycloak/themes/europeana/login/messages/messages_en.properties
COPY internationalisation/values-en/strings.properties keycloak/themes/europeana/account/messages/messages_en.properties
COPY internationalisation/values-en/strings.properties keycloak/themes/login/account/messages/messages_en.properties
COPY internationalisation/values-nl/strings.properties keycloak/themes/europeana/account/messages/messages_nl.properties
COPY internationalisation/values-nl/strings.properties keycloak/themes/login/account/messagesmessages_nl.properties

# Copy log formatter script
COPY custom-scripts/ /opt/jboss/startup-scripts/

# port to open DISABLED FOR USE WITH CF
#EXPOSE 8080

# Entrypoint
USER jboss