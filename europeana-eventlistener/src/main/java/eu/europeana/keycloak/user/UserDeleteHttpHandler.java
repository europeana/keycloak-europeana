package eu.europeana.keycloak.user;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Created by luthien on 20/01/2021.
 */
public class UserDeleteHttpHandler {
    private final CloseableHttpClient                  httpClient;

    public UserDeleteHttpHandler(){
        httpClient = HttpClients.createDefault();
    }


}
