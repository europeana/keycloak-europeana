import eu.europeana.keycloak.zoho.util.SyncHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ZohoContactNameTests {
    private SyncHelper helper ;
    @BeforeEach
    void setup(){
        helper = new SyncHelper();
    }
    //test null , empty
    @ParameterizedTest
    @CsvSource({
            ",",
            "'',''"
    })
    @DisplayName("truncateFirstNameForZoho - Should return null/empty for null/empty input")
    public void test_first_name_null_or_blank(String value,String expectedValue){
        Assertions.assertEquals(expectedValue,helper.truncateFirstNameForZoho(value));
    }
    @ParameterizedTest
    @CsvSource({
            ",",
            "'',''"
    })
    @DisplayName("truncateLastNameForZoho - Should return null/empty for null/empty input ")
    public void test_last_name_null_or_blank(String value,String expectedValue){
        Assertions.assertEquals(expectedValue,helper.truncateLastNameForZoho(value));
    }


    //test blank string inputs
    @ParameterizedTest
    @CsvSource({
            "'    ','    '",
            "'                                                               ','                                        '"
    })
    @DisplayName("Should truncate the blanks in first name if exceeding the limit")
    public void test_blanks_in_first_name(String value,String expectedValue){
        Assertions.assertEquals(expectedValue,helper.truncateFirstNameForZoho(value));
    }


    @ParameterizedTest
    @CsvSource({
            "'    ','    '",
            "'                                                                                         ','                                                                                '",
    })
    @DisplayName("Should truncate the blanks in last name if exceeding the limit")
    public void test_blanks_in_last_name(String value,String expectedValue){
        Assertions.assertEquals(expectedValue,helper.truncateLastNameForZoho(value));
    }

    @ParameterizedTest
    @CsvSource({"This is the sentence containing more than forty chars.,This is the sentence containing more",
    "Small sentence ending with space ,Small sentence ending with space ",
     "some special strings 你好,some special strings 你好"})
    @DisplayName("Should remove partial last word when truncating if length is more than limit else return as is")
    public void test_first_name(String value, String expectedValue){
        Assertions.assertEquals(expectedValue,helper.truncateFirstNameForZoho(value));
    }

    @ParameterizedTest
    @CsvSource({"This is the one big sentence which is long and containing more than eighty chars.,is the one big sentence which is long and containing more than eighty chars.",
            "Small sentence ending with space ,Small sentence ending with space ",
    "some special strings 你好 こんにちは,some special strings 你好 こんにちは"})
    @DisplayName("Should remove partial first word when truncating if length is more than limit else return as is")
    public void test_last_name(String value, String expectedValue){
        Assertions.assertEquals(expectedValue,helper.truncateLastNameForZoho(value));
    }

}