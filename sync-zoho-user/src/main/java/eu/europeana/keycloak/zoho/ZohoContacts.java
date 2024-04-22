package eu.europeana.keycloak.zoho;

import static org.keycloak.utils.StringUtil.isBlank;
import static org.keycloak.utils.StringUtil.isNotBlank;

import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.record.APIException;
import com.zoho.crm.api.record.Info;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.RecordOperations.GetRecordsHeader;
import com.zoho.crm.api.record.RecordOperations.GetRecordsParam;
import com.zoho.crm.api.record.ResponseHandler;
import com.zoho.crm.api.record.ResponseWrapper;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 03/04/2024.
 */
public class ZohoContacts {

    private static final Logger LOG             = Logger.getLogger(ZohoContacts.class);
    private static final String RESPONSE_204    = "204 No Content";
    private static final String RESPONSE_304    = "304 Not Modified";
    private static final String ACCOUNT_NAME    = "Account_Name";
    private static final String EMAIL           = "Email";
    private static final String ID              = "id";
    private static final String BOTH            = "both";
    private static final int    CONTACTSDEBUG   = 20;
    private static final int    CONTACTSPERPAGE = 200;


    private Map<String, String> contactAffiliations = new HashMap<>();

    private int page = 1;
//    private KeycloakSession session;
//    private RealmModel      realm;
//    private UserProvider    userProvider;
//    private UserManager     userManager;

//    public ZohoContacts(KeycloakSession session) {
//        this.session      = session;
//        this.realm        = session.getContext().getRealm();
//        this.userProvider = session.users();
//        this.userManager  = new UserManager(session);
//    }

    // mode 0 = debug
    public Map<String, String> getContacts(int from, int to) throws Exception {
        String pageToken = null;
        if (from == 0) {
            LOG.info("Get " + CONTACTSDEBUG + " contacts for debugging");
            getContactPage(CONTACTSDEBUG, 1, null);
        } else {
            LOG.info("Get pages " + from + " to " + to + ", each " + CONTACTSPERPAGE + " contacts");
            pageToken = getContactPage(CONTACTSPERPAGE, from, null);
            for (int i = from + 1; i <= to; i++) {
                pageToken = getContactPage(CONTACTSPERPAGE, i, pageToken);
                if (isBlank(pageToken)) {
                    break;
                }
            }
        }
        return contactAffiliations;
    }

    private String getContactPage(int contactsPerPage, int page, String nextPageToken) throws Exception {

        boolean debug   = contactsPerPage == CONTACTSDEBUG;
        int     contact = 1;
        String  europeanaOrgID;

        RecordOperations recordOperations = new RecordOperations("Contacts");
        ParameterMap     paramInstance    = new ParameterMap();
        List<String>     fieldNames       = new ArrayList<>(Arrays.asList(ACCOUNT_NAME, EMAIL));
        paramInstance.add(GetRecordsParam.FIELDS, String.join(",", fieldNames));
        paramInstance.add(GetRecordsParam.APPROVED, BOTH);
        paramInstance.add(GetRecordsParam.CONVERTED, BOTH);
        paramInstance.add(GetRecordsParam.PER_PAGE, contactsPerPage);

        if (isNotBlank(nextPageToken)) {
            paramInstance.add(GetRecordsParam.PAGE_TOKEN, nextPageToken);
        } else {
            paramInstance.add(GetRecordsParam.PAGE, page);
        }

        paramInstance.add(GetRecordsParam.INCLUDE_CHILD, "true");
        HeaderMap      headerInstance  = new HeaderMap();
        OffsetDateTime ifmodifiedsince = OffsetDateTime.of(2019, 05, 20, 10, 00, 01, 00, ZoneOffset.of("+01:00"));
        headerInstance.add(GetRecordsHeader.IF_MODIFIED_SINCE, ifmodifiedsince);
        APIResponse<ResponseHandler> response = recordOperations.getRecords(paramInstance, headerInstance);
        if (response != null) {
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                LOG.error("Zoho response: HTTP " + (response.getStatusCode() == 204 ? RESPONSE_204 : RESPONSE_304));
                return "aborting because of empty response";
            }
            StringBuilder errorDetails;
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    ResponseWrapper                      responseWrapper = (ResponseWrapper) responseHandler;
                    List<com.zoho.crm.api.record.Record> contactRecords  = responseWrapper.getData();

                    // get 200 results and process them
                    for (com.zoho.crm.api.record.Record contactRecord : contactRecords) {
                        // check if there is an Account (Institute) associated with this Contact
                        if (contactRecord.getKeyValue(ACCOUNT_NAME) instanceof com.zoho.crm.api.record.Record) {
                            // get linked Institute
                            com.zoho.crm.api.record.Record accountRecord = (com.zoho.crm.api.record.Record) contactRecord.getKeyValue(
                                ACCOUNT_NAME);
                            // get europeana org.ID from Institute (if any)
                            europeanaOrgID = ZohoInstitute.getRecord((Long) accountRecord.getKeyValue(ID));
                            // check if it is present: yes, add to map
                            if (isNotBlank(europeanaOrgID)) {
                                contactAffiliations.put((String) contactRecord.getKeyValue(EMAIL),
                                                        isNotBlank(europeanaOrgID) ? europeanaOrgID
                                                                                   : "Institute has no org.ID");
                            }
                            // log message if this contact has no associated Institute
                        } else {
                            LOG.info(
                                "No Institute associated with Contact email: " + contactRecord.getKeyValue(EMAIL));
                        }
                        LOG.info("#" + contact + " of page# " + page);
                        contact++;
                    }

                    Info info = responseWrapper.getInfo();

                    if (!debug && info.getMoreRecords()) {
                        LOG.info("Retrieved page# " + this.page + " with " + contact +
                                 " contacts, moving to the next page ...");
                        return info.getNextPageToken();
                    } else {
                        LOG.info("All done.");
                        return null;
                    }

                } else if (responseHandler instanceof APIException) {
                    APIException exception = (APIException) responseHandler;
                    errorDetails = new StringBuilder("Zoho Exception details: /n");
                    LOG.error("Zoho Exception status: " + exception.getStatus().getValue());
                    LOG.error("Zoho Exception code: " + exception.getCode().getValue());
                    for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        errorDetails.append(entry.getKey()).append(": ").append(entry.getValue()).append("/n");
                    }
                    LOG.error(errorDetails);
                    LOG.error("Zoho Exception message: " + exception.getMessage().getValue());
                }
            } else {
                Model                     responseObject = response.getModel();
                Class<? extends Model>    clas           = responseObject.getClass();
                java.lang.reflect.Field[] fields         = clas.getDeclaredFields();
                errorDetails = new StringBuilder("Unexpected response from Zoho: /n");
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    errorDetails.append(field.getName()).append(": ").append(field.get(responseObject)).append("/n");
                }
                LOG.error(errorDetails);
                return "Vette pech.";
            }
        } else {
            return "Hey, good job.";
        }
        return "Yeah.";
    }

}

