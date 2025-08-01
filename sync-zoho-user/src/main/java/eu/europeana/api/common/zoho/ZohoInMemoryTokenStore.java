package eu.europeana.api.common.zoho;

import com.zoho.api.authenticator.OAuthToken;
import com.zoho.api.authenticator.Token;
import com.zoho.api.authenticator.store.TokenStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** An implementation of {@link TokenStore} that provides custom storage and retrieval
 *  operation for Zoho OAuth tokens.
 * @author Shweta Nazare, Sergiu Gordea, Luthien Dulk Created on 12 feb 2024
 */
public class ZohoInMemoryTokenStore implements TokenStore {

    private final Map<String, Token> tokenStore = new ConcurrentHashMap<>();

    @Override
    public Token findToken(Token token) {
        if(token instanceof OAuthToken oauthToken){
            return tokenStore.get(oauthToken.getUserSignature().getName());
        }
        return null;
    }

    @Override
    public Token findTokenById(String s) {
        return tokenStore.get(s);
    }

    @Override
    public void saveToken(Token token) {
        if(token instanceof OAuthToken outhToken) {
            tokenStore.put(outhToken.getUserSignature().getName(), token);
        }
    }

    @Override
    public void deleteToken(String s) {
        tokenStore.remove(s);
    }

    @Override
    public List<Token> getTokens() {
        return (List<Token>) tokenStore.values();
    }

    @Override
    public void deleteTokens() {
        tokenStore.clear();
    }
}