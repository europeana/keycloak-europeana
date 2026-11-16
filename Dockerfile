ARG keycloak_version=26.8.1
ARG theme_version=0.12.1

# 1 get theme from GitHub
FROM europeana/keycloak-theme:${theme_version} AS theme

# 2 get base Keycloak image
FROM quay.io/keycloak/keycloak:${keycloak_version} AS builder
WORKDIR /opt/keycloak

ENV KC_DB=postgres
ENV KC_HTTP_RELATIVE_PATH=/auth

# 3 copy addons + dependencies to Quarkus providers dir
COPY addon-jars ./providers/
COPY dependencies ./providers/

# 4 copy quarkus configuration with custom jdbc settings + otel-javaagent apm jar to conf
COPY config/cache-ispn-impl.xml ./conf/
COPY config/opentelemetry-javaagent.jar ./conf/

# 5 copy theme(s)
COPY --from=theme /opt/keycloak/themes/europeana ./themes/europeana
COPY --from=theme /opt/keycloak/themes/europeana-v2 ./themes/europeana-v2

# 6 build the optimized Keycloak installation
RUN /opt/keycloak/bin/kc.sh build

# 7 get the final image and transfer entire optimized installation to it
FROM quay.io/keycloak/keycloak:${keycloak_version}
WORKDIR /opt/keycloak
COPY --from=builder /opt/keycloak/ /opt/keycloak/

