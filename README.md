# keycloak-europeana

This project contains: 

- addon-jars, a placeholder directory for the compiled add-on jars, used in the build job and by the Dockerfile
- source code of the add-on modules:
- - bcrypt-addon: BCrypt custom encryption
- - delete-unverified-users: work in progress
- - eventlistener: catches user delete events, sends delete request to User Sets API and reports user deletion to Slack (backup) via email
- - keycloak-metrics-spi: reports current number of users
- currently-unused-scripts: TODO check if this can be deprecated
- custom-scripts: contains logstash-formatting script (changes Keycloak config at startup)
- dependencies: used by the BCrypt add-on
- k8s: Kustomizer configuration used to create Kubernetes files for different deployments
- Dockerfile, to build a docker image of Keycloak + add-ons that can be deployed in our Kubernetes cluster
- and the usual pom.xml & .ignore files

### Check the versions in the pom.xml files

Keep in mind that the versions of Keycloak specified in the root pom.xml and in the Dockerfile need to match!

- current keycloak version is **20.0.1**

### Build the Docker image in Jenkins; provide the configuration; run the image in Kubernetes
TBD

### building, configuring and running the image on a local Kubernetes cluster:
TBD 

_The password encryption is uses the Bcrypt Library by Patrick Favre-Bulle (https://github.com/patrickfav/bcrypt), licensed under the Apache License, Version 2.0 (see the BCRYPT 3rd party license in the bcrypt-addon module)_