# keycloak-cf-docker

This project prepares all necessary resources needed to build a Keycloak docker container
to deploy on Cloud Foundry:

- compile the Bcrypt addon module as a jar file; this is copied to the Wildfly deployment
directory in the Docker image;

- copy the two necessary dependencies to the Keycloak Modules directory;

- copy the PostgreSQL driver jar to the Keycloak Modules directory;

- patch the Keycloak configuration to use PostgreSQL

The above is all done by issuing a mvn clean package on the root pom.
After that is done, the Docker image can be built with (example)

docker build --no-cache --tag keycloak-cf-docker:0.12 .

The docker image thus produced can be run when DB connection settings are provided as environment
variables like this: 

docker run --publish 8080:8080 --detach  \
-e DB_ADDR='weird-ibm-db-url' \
-e DB_PORT='16080' \
-e DB_DATABASE='keycloak4cf' \
-e DB_SCHEMA='public' \
-e DB_PASSWORD='now-way-jose' \
-e DB_USER='admin' \
--name kccf keycloak-cf-docker:0.12

This connects to the small test DB prepared earlier for the first test with Keycloak on Docker
and it works.

Note that the BCrypt pepper is for now stored in the keycloak.user.properties (not uploaded), and
needs to be substituted in the bcryptpasswordhashproviderfactory class. This will be fixed asap.