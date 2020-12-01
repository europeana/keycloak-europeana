#!/bin/bash

# Exit with error if jboss add-user.sh script fails
set -e

if [[ -n ${EUROPEANA_JBOSS_ADMIN_USER:-} && -n ${EUROPEANA_JBOSS_ADMIN_PASSWORD:-} ]]; then
    echo "Creating JBOSS management user account: $EUROPEANA_JBOSS_ADMIN_USER"
    /opt/jboss/keycloak/bin/add-user.sh --user "$EUROPEANA_JBOSS_ADMIN_USER" --password "$EUROPEANA_JBOSS_ADMIN_PASSWORD"
fi


# Let the default keycloak startup script handle its own errors
set +e

/opt/jboss/tools/docker-entrypoint.sh