# 0. Clone and build theme from GitHub
FROM node:14-alpine AS custom-theme

ARG KEYCLOAK_THEME_GIT_REF=master

RUN apk add --no-cache git

RUN git clone https://github.com/europeana/keycloak-theme.git

WORKDIR /keycloak-theme
RUN git checkout ${KEYCLOAK_THEME_GIT_REF}
RUN npm install
RUN npm run build


# build step
#FROM quay.io/keycloak/keycloak:latest as builder
FROM quay.io/keycloak/keycloak:20.0.1 as builder
# TODO investigate how this works later
#ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres
RUN /opt/keycloak/bin/kc.sh build


# run config
#FROM quay.io/keycloak/keycloak:latest
FROM quay.io/keycloak/keycloak:20.0.1
COPY --from=builder /opt/keycloak/lib/quarkus/ /opt/keycloak/lib/quarkus/
WORKDIR /opt/keycloak
# for demonstration purposes only, please make sure to use proper certificates in production instead
#RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore

# Copy theme from stage custom-theme
COPY --from=custom-theme /keycloak-theme/theme /opt/keycloak/themes/europeana

# Note: credentials are used only when initialising a new empty DB
# TODO can these be removed?
ENV KEYCLOAK_USER=admin
ENV KEYCLOAK_PASSWORD=change-this-into-something-useful


# Copy addons to Quarkus providers dir
COPY addon-jars providers
# Copy addon dependencies to Quarkus providers dir
COPY dependencies providers

# TODO investigate how this works later
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]