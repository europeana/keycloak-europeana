package eu.europeana.keycloak.zoho;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.record.APIException;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.ResponseHandler;
import com.zoho.crm.api.record.ResponseWrapper;
import com.zoho.crm.api.record.RecordOperations.GetRecordHeader;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;
import org.jboss.logging.Logger;

public class ZohoInstitute {
    private static final Logger LOG = Logger.getLogger(ZohoInstitute.class);
    private static final String BOTH = "both";
    private static final String RESPONSE_204 = "204 No Content";
    private static final String RESPONSE_304 = "304 Not Modified";
    private static final String EUROPEANA_ORG_ID = "Europeana_org_ID";

    private ZohoInstitute(){}

    public static String getRecord(Long recordId) throws Exception {
        RecordOperations recordOperations = new RecordOperations("Accounts");
        ParameterMap paramInstance = new ParameterMap();
        paramInstance.add(RecordOperations.GetRecordsParam.APPROVED, BOTH);
        paramInstance.add(RecordOperations.GetRecordsParam.CONVERTED, BOTH);

        HeaderMap      headerInstance  = new HeaderMap();
        OffsetDateTime ifmodifiedsince = OffsetDateTime.of(2019, 05, 20, 10, 00, 01, 00, ZoneOffset.of("+05:30"));
        headerInstance.add(GetRecordHeader.IF_MODIFIED_SINCE, ifmodifiedsince);
        APIResponse<ResponseHandler> response = recordOperations.getRecord(recordId, paramInstance, headerInstance);
        if (response != null) {
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                LOG.error("Zoho response: HTTP " + (response.getStatusCode() == 204 ? RESPONSE_204 : RESPONSE_304));
                return null;
            }
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    ResponseWrapper                      responseWrapper  = (ResponseWrapper) responseHandler;
                    List<com.zoho.crm.api.record.Record> instituteRecords = responseWrapper.getData();
                    for (com.zoho.crm.api.record.Record instituteRecord : instituteRecords) {
                        if (null != instituteRecord.getKeyValue(EUROPEANA_ORG_ID)){
                            return (String) instituteRecord.getKeyValue(EUROPEANA_ORG_ID);
                        } else {
                            return null;
                        }
                    }

                } else if (responseHandler instanceof APIException) {
                    APIException exception = (APIException) responseHandler;
                    StringBuilder errorDetails = new StringBuilder("Zoho Exception details: /n");
                    LOG.error("Zoho Exception status: " + exception.getStatus().getValue());
                    LOG.error("Zoho Exception code: " + exception.getCode().getValue());
                    for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        errorDetails.append(entry.getKey()).append(": ").append(entry.getValue()).append("/n");
                    }
                    LOG.error(errorDetails);
                    LOG.error("Zoho Exception message: " + exception.getMessage().getValue());
                }
            } else {
                Model responseObject = response.getModel();
                Class<? extends Model> clas = responseObject.getClass();
                java.lang.reflect.Field[] fields = clas.getDeclaredFields();
                StringBuilder errorDetails = new StringBuilder("Unexpected response from Zoho: /n");
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    errorDetails.append(field.getName()).append(": ").append(field.get(responseObject)).append("/n");
                }
                LOG.error(errorDetails);
            }
        }
        return null;
    }
}
