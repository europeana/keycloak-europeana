FROM jboss/keycloak:10.0.2

# Set workdir to jboss home
WORKDIR /opt/jboss/

# Set environment variables
ENV DB_VENDOR postgres

ENV KEYCLOAK_USER: admin

ENV KEYCLOAK_PASSWORD: w@hw@hpedal5

# Copy commons-codec & spring-security-crypto (BCrypt dependencies) to keycloak/modules
COPY bcrypt-dependencies keycloak/modules

# Copy BCrypt addon to the Wildfly deployment directory
COPY bcrypt-addon-jar keycloak/standalone/deployments

# port to open
EXPOSE 8080

# Entrypoint
USER jboss