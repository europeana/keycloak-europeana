#!/bin/bash
# This script creates a Wildfly management user for accessing metrics on the server
# The user is only created if EUROPEANA_JBOSS_ADMIN_USER and EUROPEANA_JBOSS_ADMIN_PASSWORD env variables are set

# Exit with error if jboss add-user.sh script fails
set -e

if [[ -n ${EUROPEANA_JBOSS_ADMIN_USER:-} && -n ${EUROPEANA_JBOSS_ADMIN_PASSWORD:-} ]]; then
    echo "Creating JBOSS management user account: $EUROPEANA_JBOSS_ADMIN_USER"
    /opt/jboss/keycloak/bin/add-user.sh --user "$EUROPEANA_JBOSS_ADMIN_USER" --password "$EUROPEANA_JBOSS_ADMIN_PASSWORD"
fi


# Let keycloak startup process handle its own errors
set +e