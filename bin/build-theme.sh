#!/bin/sh

# Builds europeana Keycloak theme from GitHub

set -e

# Install dependencies
microdnf install git-core xz
curl -O https://nodejs.org/dist/v${NODEJS_VERSION}/node-v${NODEJS_VERSION}-linux-x64.tar.xz
tar -xf node-v${NODEJS_VERSION}-linux-x64.tar.xz
export PATH=/opt/jboss/node-v${NODEJS_VERSION}-linux-x64/bin:${PATH}

# Clone and build theme
git clone https://github.com/europeana/keycloak-theme.git
cd keycloak-theme
git checkout ${KEYCLOAK_THEME_GIT_REF}
npm install
npm run build

# Install theme
mv theme /opt/jboss/keycloak/themes/europeana
chown -R jboss /opt/jboss/keycloak/themes/europeana

# Clean up
cd ..
rm -rf keycloak-theme node-v${NODEJS_VERSION}-linux-x64*
microdnf remove git-core xz
microdnf clean all
