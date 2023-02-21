# 0. Clone and build theme from GitHub
FROM node:14-alpine AS custom-theme

ARG KEYCLOAK_THEME_GIT_REF=master

RUN apk add --no-cache git

RUN git clone https://github.com/europeana/keycloak-theme.git

WORKDIR /keycloak-theme
RUN git checkout ${KEYCLOAK_THEME_GIT_REF}
RUN npm install
RUN npm run build


# configure, add add-ons + dependencies and build custom image

FROM quay.io/keycloak/keycloak:20.0.5 as builder
# TODO look into metrics
ENV KC_METRICS_ENABLED=true
ENV KC_HEALTH_ENABLED=true
ENV KC_DB=postgres
ENV KC_HTTP_RELATIVE_PATH=/auth

# test new schema
ENV KEYCLOAK_USER=admin
ENV KEYCLOAK_PASSWORD=whoopteedoo

# Copy addons to Quarkus providers dir
COPY addon-jars /opt/keycloak/providers/
# Copy addon dependencies to Quarkus providers dir
COPY dependencies /opt/keycloak/providers/
# create intermediary build
RUN /opt/keycloak/bin/kc.sh build

# Copy theme from stage custom-theme
COPY --from=custom-theme /keycloak-theme/theme /opt/keycloak/themes/europeana

FROM quay.io/keycloak/keycloak:20.0.5
# see https://github.com/keycloak/keycloak/discussions/10502?sort=new why copying providers twice is needed
COPY --from=builder /opt/keycloak/providers/ /opt/keycloak/providers/
COPY --from=builder /opt/keycloak/lib/quarkus/ /opt/keycloak/lib/quarkus/

# comment out for local deployment
#RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore

# Configure APM and add APM agent
# NOT YET, we've never done this on Keycloak, and wget does not work in this image anyhow
#ENV ELASTIC_APM_VERSION 1.34.1
#RUN wget https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar -O /usr/local/elastic-apm-agent.jar

# set entry point, comment out for local deployment
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]
# for local deployment  use this instead when running the image
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]
#start-dev --hostname=localhost