# Check that this version matches the one in the pom.xml!
FROM jboss/keycloak:12.0.4

# Set workdir to jboss home
WORKDIR /opt/jboss/

ARG KEYCLOAK_THEME_GIT_REF=master \
    NODEJS_VERSION=14.16.1

# Set environment variables
ENV DB_VENDOR=postgres \
    # Note: credentials are used only when initialising a new empty DB
    KEYCLOAK_USER=admin \
    KEYCLOAK_PASSWORD=change-this-into-something-useful

# Build theme from GitHub
USER root
RUN microdnf install git-core xz && \
    curl -O https://nodejs.org/dist/v${NODEJS_VERSION}/node-v${NODEJS_VERSION}-linux-x64.tar.xz && \
    tar -xf node-v${NODEJS_VERSION}-linux-x64.tar.xz && \
    export PATH=/opt/jboss/node-v${NODEJS_VERSION}-linux-x64/bin:${PATH} && \
    git clone https://github.com/europeana/keycloak-theme.git && \
    cd keycloak-theme && \
    git checkout ${KEYCLOAK_THEME_GIT_REF} && \
    npm install && \
    npm run build && \
    mkdir -p /opt/jboss/keycloak/themes/europeana && \
    chown -R jboss /opt/jboss/keycloak/themes/europeana && \
    mv theme /opt/jboss/keycloak/themes/europeana && \
    cd .. && \
    rm -rf keycloak-theme node-v${NODEJS_VERSION}-linux-x64* && \
    microdnf remove git-core xz && \
    microdnf clean all
USER jboss

# create user for accessing Wildfly metrics.
# only used if EUROPEANA_JBOSS_ADMIN_PASSWORD is set (see custom-scripts/add-wildfly-mgmt-user.sh)
# ENV EUROPEANA_JBOSS_ADMIN_USER admin

# Copy commons-codec, favre-crypto & -bytes (BCrypt dependencies) to keycloak/modules
COPY bcrypt-dependencies keycloak/modules

# Copy addons to the Wildfly deployment directory
COPY addon-jars keycloak/standalone/deployments

# Copy log formatter script
COPY custom-scripts/ /opt/jboss/startup-scripts/
