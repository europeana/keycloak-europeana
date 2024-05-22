ARG theme_version=0.9.1

# 1 get theme from GitHub
FROM europeana/keycloak-theme:${theme_version} AS theme

# 2 get base Keycloak image
FROM quay.io/keycloak/keycloak:20.0.5 as builder
WORKDIR /opt/keycloak
ENV KC_DB=postgres

# 3 copy addons to Quarkus providers dir
COPY addon-jars ./providers/

# 4 copy addon dependencies to Quarkus providers dir
COPY dependencies ./providers/

# 5 copy quarkus configuration with custom jdbc settings
COPY config ./conf/

# 6 copy theme
COPY --from=theme /opt/keycloak/themes/europeana ./themes/europeana

# 7 create intermediary build
RUN /opt/keycloak/bin/kc.sh build

# 8 get another copy of Keycloak image and apply changes there again
# see https://github.com/keycloak/keycloak/discussions/10502?sort=new why
FROM quay.io/keycloak/keycloak:20.0.5
WORKDIR /opt/keycloak

# 9 copy add-ons, dependencies, optimised libs and theme again to new copy
COPY --from=builder /opt/keycloak/providers/ ./providers/
COPY --from=builder /opt/keycloak/conf/ ./conf/
COPY --from=builder /opt/keycloak/lib/quarkus/ ./lib/quarkus/
COPY --from=builder /opt/keycloak/themes/europeana ./themes/europeana

# 10 start command / entry point was moved to Kustomizer deployment-patch.yaml.template
# fix for redirect issue.
# CMD ["start", "--optimized", "--spi-login-protocol-openid-connect-legacy-logout-redirect-uri=true"]

