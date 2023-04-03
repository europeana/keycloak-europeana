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

# Copy addons to Quarkus providers dir
COPY addon-jars /opt/keycloak/providers/
# Copy addon dependencies to Quarkus providers dir
COPY dependencies /opt/keycloak/providers/
# Copy theme from stage custom-theme into builder stage
COPY --from=custom-theme /keycloak-theme/theme /opt/keycloak/themes/europeana
# create intermediary build
RUN /opt/keycloak/bin/kc.sh build


FROM quay.io/keycloak/keycloak:20.0.5
# see https://github.com/keycloak/keycloak/discussions/10502?sort=new why copying providers in such
# a cumbersome way this is needed
COPY --from=builder /opt/keycloak/providers/ /opt/keycloak/providers/
COPY --from=builder /opt/keycloak/lib/quarkus/ /opt/keycloak/lib/quarkus/
COPY --from=builder /opt/keycloak/themes/europeana /opt/keycloak/themes/europeana

# Configure APM and add APM agent
# NOT YET, we've never done this on Keycloak, and wget does not work in this image anyhow
#ENV ELASTIC_APM_VERSION 1.34.1
#RUN wget https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar -O /usr/local/elastic-apm-agent.jar