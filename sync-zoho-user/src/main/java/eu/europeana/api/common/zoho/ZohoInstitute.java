package eu.europeana.api.common.zoho;


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

public class ZohoInstitute {

    public static void getRecord(Long recordId) throws Exception {
        RecordOperations recordOperations = new RecordOperations("Accounts");
        ParameterMap paramInstance = new ParameterMap();
        paramInstance.add(RecordOperations.GetRecordsParam.APPROVED, "both");
        paramInstance.add(RecordOperations.GetRecordsParam.CONVERTED, "both");

        HeaderMap      headerInstance  = new HeaderMap();
        OffsetDateTime ifmodifiedsince = OffsetDateTime.of(2019, 05, 20, 10, 00, 01, 00, ZoneOffset.of("+05:30"));
        headerInstance.add(GetRecordHeader.IF_MODIFIED_SINCE, ifmodifiedsince);
        APIResponse<ResponseHandler> response = recordOperations.getRecord(recordId, paramInstance, headerInstance);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                System.out.println(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
                return;
            }
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    ResponseWrapper responseWrapper = (ResponseWrapper) responseHandler;
                    List<com.zoho.crm.api.record.Record> records = responseWrapper.getData();
                    for (com.zoho.crm.api.record.Record record : records) {
                        System.out.println("Record ID: " + record.getId());
                        if (null != record.getKeyValue("Europeana_org_ID")){
                            System.out.println((String) record.getKeyValue("Europeana_org_ID"));
                        } else {
                            System.out.println("Institute has no Europeana Org ID yet");
                        }
                    }

                } else if (responseHandler instanceof APIException) {
                    APIException exception = (APIException) responseHandler;
                    System.out.println("Status: " + exception.getStatus().getValue());
                    System.out.println("Code: " + exception.getCode().getValue());
                    System.out.println("Details: ");
                    for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    System.out.println("Message: " + exception.getMessage().getValue());
                }
            } else {
                Model responseObject = response.getModel();
                Class<? extends Model> clas = responseObject.getClass();
                java.lang.reflect.Field[] fields = clas.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    System.out.println(field.getName() + ":" + field.get(responseObject));
                }
            }
        }
    }
}
