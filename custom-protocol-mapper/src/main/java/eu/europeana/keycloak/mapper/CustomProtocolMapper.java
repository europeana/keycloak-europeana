package eu.europeana.keycloak.mapper;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luthien on 07/06/2023.
 */
public class CustomProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper,
                                                                                OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "custom-protocol-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, CustomProtocolMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Client Public ID Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds the public client ID to the claim";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * This adds the client public ID and client name to the token, similarly as previously done with the scripted
     * protocol mapper (which was deprecated since Keycloak v18)
     */
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                            UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        token.getOtherClaims().put("client_public_id", keycloakSession.getContext().getClient().getId());
        token.getOtherClaims().put("client_name", keycloakSession.getContext().getClient().getName());
    }
}
