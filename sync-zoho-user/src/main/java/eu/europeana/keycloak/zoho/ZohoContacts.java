package eu.europeana.keycloak.zoho;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 03/04/2024.
 */
public class ZohoContacts {

    private static final Logger LOG          = Logger.getLogger(ZohoContacts.class);
    private static final String RESPONSE_204 = "204 No Content";
    private static final String RESPONSE_304 = "304 Not Modified";
    private static final String DEBUG        = "DEBUG";
    private static final String ACCOUNT_NAME = "Account_Name";
    private static final String EMAIL        = "Email";
    private static final String ID           = "id";
    private static final String BOTH         = "both";


    private final Map<String, String> contactAffiliations = new HashMap<>();


    public Map<String, String> getContacts(String nextPageToken) throws Exception {
        boolean          debug            = false;
        RecordOperations recordOperations = new RecordOperations("Contacts");
        ParameterMap     paramInstance    = new ParameterMap();
        List<String>     fieldNames       = new ArrayList<>(Arrays.asList(ACCOUNT_NAME, EMAIL));
        paramInstance.add(GetRecordsParam.FIELDS, String.join(",", fieldNames));
        paramInstance.add(GetRecordsParam.APPROVED, BOTH);
        paramInstance.add(GetRecordsParam.CONVERTED, BOTH);
        if (isNotBlank(nextPageToken)) {
            if (nextPageToken.equalsIgnoreCase(DEBUG)) {
                LOG.info("Get 20 contacts for developing purposes");
                debug = true;
            } else {
                LOG.info("Retrieving next batch of contacts");
                paramInstance.add(GetRecordsParam.PAGE_TOKEN, nextPageToken);
            }
        }
        if (debug) {
            paramInstance.add(GetRecordsParam.PER_PAGE, 20);
        } else {
            paramInstance.add(GetRecordsParam.PER_PAGE, 200);
        }
        paramInstance.add(GetRecordsParam.INCLUDE_CHILD, "true");
        HeaderMap      headerInstance  = new HeaderMap();
        OffsetDateTime ifmodifiedsince = OffsetDateTime.of(2019, 05, 20, 10, 00, 01, 00, ZoneOffset.of("+01:00"));
        headerInstance.add(GetRecordsHeader.IF_MODIFIED_SINCE, ifmodifiedsince);
        APIResponse<ResponseHandler> response = recordOperations.getRecords(paramInstance, headerInstance);
        if (response != null) {
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                LOG.error("Zoho response: HTTP " + (response.getStatusCode() == 204 ? RESPONSE_204 : RESPONSE_304));
                return Collections.emptyMap();
            }
            StringBuilder errorDetails;
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    ResponseWrapper                      responseWrapper = (ResponseWrapper) responseHandler;
                    List<com.zoho.crm.api.record.Record> contactRecords  = responseWrapper.getData();
                    // get first 200 results and process them
                    for (com.zoho.crm.api.record.Record contactRecord : contactRecords) {
                        if (contactRecord.getKeyValue(ACCOUNT_NAME) instanceof com.zoho.crm.api.record.Record) {
                            com.zoho.crm.api.record.Record accountRecord = (com.zoho.crm.api.record.Record) contactRecord.getKeyValue(
                                ACCOUNT_NAME);
                            ZohoInstitute.getRecord((Long) accountRecord.getKeyValue(ID));
                            LOG.info("Institute " + accountRecord.getKeyValue("name") +
                                     " associated with Contact with email: " + contactRecord.getKeyValue(EMAIL));
                            contactAffiliations.put((String) contactRecord.getKeyValue(EMAIL),
                                                    ZohoInstitute.getRecord((Long) accountRecord.getKeyValue(ID)));
                        } else {
                            LOG.info(
                                "No Institute associated with Contact with email: " + contactRecord.getKeyValue(EMAIL));
                        }
                    }

                    Info    info        = responseWrapper.getInfo();
                    boolean thereIsMore = info.getMoreRecords();
                    if (!debug && thereIsMore) {
                        LOG.info("Retrieved 200 contacts, moving to the next batch ...");
                        getContacts(info.getNextPageToken());
                    } else {
                        LOG.info("... processed all contacts, returning to main method.");
                        return contactAffiliations;
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
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
        return Collections.emptyMap();
    }
}

