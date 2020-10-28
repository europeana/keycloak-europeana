# keycloak-cf-docker

This project contains: 

- the Dockerfile needed to build the docker image of Keycloak as used by Europeana

- the java source code needed to build the BCrypt module .jar file that is to be added to the docker image

- placeholder directories for the BCrypt dependencies (to be added to keycloak/modules/)

- a placeholder directory for the bcrypt-addon.jar file (to be added to keycloak/standalone/deployments/) 

### Check the versions in the pom.xml files

The version of Keycloak and all dependencies are set in `bcrypt-addon/pom.xml`. Most important are:

- keycloak: **10.0.2**

- apache commons-codec: **1.14**

- spring-security-crypto: **5.1.4.RELEASE**

### How to build a Docker image in Jenkins:

- pull this project

- provide a properties file containing the settings needed for the BCrypt module 
(see `bcrypt-addon/src/main/resources/bcrypt.properties`) and copy that to the same directory, 
renaming it to `bcrypt-user.properties`

- add a top level Maven target: `clean package -U`; this will compile the project, package it 
and copy the dependency `.jar` files and `bcrypt-addon.jar` to the placeholder directories

- build the docker image in an _Execute Shell_ module, e.g. `docker build -t ${DOCKER_IMAGE_NAME} .`

- and push the image to Docker Hub

### building and running the image locally:

For testing, this project can also be run in a local Docker environment:

- check this project out from Github

- execute `mvn clean package` to create the necessary .jar files

- build the docker image: 

`docker build --no-cache --tag {imagename}:{tag} .`

- run the image: 

`docker run --publish 8080:8080 --detach  -e DB_ADDR={POSTGRES URL} -e DB_PORT={POSTGRES PORT} -e DB_DATABASE={DATABASE NAME}' -e DB_SCHEMA={SCHEMA NAME} -e DB_PASSWORD={DB PASSWORD} -e DB_USER={DB USER} --name {CONTAINER_NAME} {imagename}:{tag}`

Note that the `{imagename}:{tag}` provided in the run command has to match was was specified when 
building the image.


_The password encryption is uses the Bcrypt Library by Patrick Favre-Bulle (https://github.com/patrickfav/bcrypt), licensed under the Apache License, Version 2.0 (see the BCRYPT 3rd party license in the bcrypt-addon module)_