import static org.mockito.Mockito.when;

import eu.europeana.keycloak.utils.Constants;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wildfly.common.Assert;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationServiceTest {

    private ApiKeyValidationService service;
    @Mock
    private KeycloakSession session;

    @Mock
    private KeycloakContext context;

    @Mock
    private RealmModel realm;

    @BeforeEach
    void setUp() {
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        service = new ApiKeyValidationService(session);
    }
    @Test
    void whenCurrentTimeIsAfterLastTimeRateLimitReached(){
        Assertions.assertTrue(service.isRateLimitAlreadyExhausted
            (LocalDateTime.now(),"2026-06-05T16:46:26Z",0));
    }

    @ParameterizedTest
    @CsvSource({
        ", 0",       // null string
        "'', 0",     // empty string
        "' ', 0"     // blank string
    })
    @DisplayName("Should return false when lastReachedTime is empty or null")
    void testWhenEmptyOrNullLastReachedTime(String lastReachedTime, int sessionCount) {
        Assert.assertFalse(service.isRateLimitAlreadyExhausted(LocalDateTime.now(), lastReachedTime, sessionCount));
    }

    @Test
    @DisplayName("Should return false when session count is greater than zero")
    void testSessionCountGreaterThanZero() {
        Assertions.assertFalse(service.isRateLimitAlreadyExhausted
            (LocalDateTime.now(),"2026-06-05T16:46:26Z",1));
    }


    @Test
    @DisplayName("Should return false  If current time and the rateLimit reaching time are same ,"
        + " that means the rate limit was exhausted during current calculation and 429 should not be sent to user")
    void whenCurrentTimeIsEqualToLastTimeRateLimitReached() {
        LocalDateTime now = LocalDateTime.parse("2026-06-05T16:46:26Z", Constants.FORMATTER);
        Assertions.assertFalse(service.isRateLimitAlreadyExhausted
            (now, "2026-06-05T16:46:26Z", 0));
    }

}
