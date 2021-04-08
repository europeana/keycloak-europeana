package eu.europeana.keycloak.user;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Created by luthien on 20/01/2021.
 */
public class UserDeleteRequestHandler {
    private final CloseableHttpClient                  httpClient;

    public UserDeleteRequestHandler(){
        httpClient = HttpClients.createDefault();
    }


}
