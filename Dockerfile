# 0. Clone and build theme from GitHub
FROM node:14-alpine

ARG KEYCLOAK_THEME_GIT_REF=master

RUN apk add --no-cache git

RUN git clone https://github.com/europeana/keycloak-theme.git

WORKDIR /keycloak-theme
RUN git checkout ${KEYCLOAK_THEME_GIT_REF}
RUN npm install
RUN npm run build

# 1. Build Keycloak
# Check that this version matches the one in the pom.xml!
FROM jboss/keycloak:12.0.4

# Set workdir to jboss home
WORKDIR /opt/jboss/

# Set environment variables
ENV DB_VENDOR=postgres \
    # Note: credentials are used only when initialising a new empty DB
    KEYCLOAK_USER=admin \
    KEYCLOAK_PASSWORD=change-this-into-something-useful

# create user for accessing Wildfly metrics.
# only used if EUROPEANA_JBOSS_ADMIN_PASSWORD is set (see custom-scripts/add-wildfly-mgmt-user.sh)
# ENV EUROPEANA_JBOSS_ADMIN_USER admin

# Copy theme from stage 0
COPY --from=0 /keycloak-theme/theme /opt/jboss/keycloak/themes/europeana

# Copy commons-codec, favre-crypto & -bytes (BCrypt dependencies) to keycloak/modules
COPY bcrypt-dependencies keycloak/modules

# Copy addons to the Wildfly deployment directory
COPY addon-jars keycloak/standalone/deployments

# Copy log formatter script
COPY custom-scripts/ /opt/jboss/startup-scripts/
