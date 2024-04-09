package eu.europeana.api.common.zoho;

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
import java.util.List;
import java.util.Map;

/**
 * Created by luthien on 03/04/2024.
 */
public class ZohoContacts {

    public static void getContacts() throws Exception {
        RecordOperations recordOperations = new RecordOperations("Contacts");
        ParameterMap     paramInstance    = new ParameterMap();
        List<String>     fieldNames       = new ArrayList<String>(Arrays.asList("Account_Name", "Email"));
        paramInstance.add(GetRecordsParam.FIELDS, String.join(",", fieldNames));
        paramInstance.add(GetRecordsParam.APPROVED, "both");
        paramInstance.add(GetRecordsParam.CONVERTED, "both");
//        paramInstance.add(GetRecordsParam.SORT_BY, "Created_Time");
//        paramInstance.add(GetRecordsParam.SORT_ORDER, "desc");
//        paramInstance.add(GetRecordsParam.PAGE, 1);
//        paramInstance.add(GetRecordsParam.PER_PAGE, 10);
        paramInstance.add(GetRecordsParam.INCLUDE_CHILD, "true");
        HeaderMap      headerInstance  = new HeaderMap();
        OffsetDateTime ifmodifiedsince = OffsetDateTime.of(2019, 05, 20, 10, 00, 01, 00, ZoneOffset.of("+01:00"));
        headerInstance.add(GetRecordsHeader.IF_MODIFIED_SINCE, ifmodifiedsince);
        APIResponse<ResponseHandler> response = recordOperations.getRecords(paramInstance, headerInstance);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                System.out.println(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
                return;
            }
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    ResponseWrapper                      responseWrapper = (ResponseWrapper) responseHandler;
                    List<com.zoho.crm.api.record.Record> records         = responseWrapper.getData();
                    for (com.zoho.crm.api.record.Record record : records) {
                        System.out.println("Record ID: " + record.getId());
                        if (null != record.getKeyValue("Email")){
                            System.out.println("Email: " + record.getKeyValue("Email"));
                        }
                        if (null != record.getKeyValue("Account_Name") && record.getKeyValue("Account_Name") instanceof com.zoho.crm.api.record.Record){
                            com.zoho.crm.api.record.Record accountRecord = (com.zoho.crm.api.record.Record) record.getKeyValue("Account_Name");
                            System.out.println("Institution name: " + accountRecord.getKeyValue("name"));
                            System.out.println("Institution ID: " + accountRecord.getKeyValue("id"));
                            ZohoInstitute.getRecord((Long) accountRecord.getKeyValue("id"));
                        } else {
                            System.out.println("No Institute associated with this Contact");
                        }
                    }

                    Info info = responseWrapper.getInfo();
                    if (info != null) {
                        if (info.getPerPage() != null) {
                            System.out.println("Record Info PerPage: " + info.getPerPage().toString());
                        }
                        if (info.getCount() != null) {
                            System.out.println("Record Info Count: " + info.getCount().toString());
                        }
                        if (info.getPage() != null) {
                            System.out.println("Record Info Page: " + info.getPage().toString());
                        }
                        if (info.getMoreRecords() != null) {
                            System.out.println("Record Info MoreRecords: " + info.getMoreRecords().toString());
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
                Model                     responseObject = response.getModel();
                Class<? extends Model>    clas           = responseObject.getClass();
                java.lang.reflect.Field[] fields         = clas.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    System.out.println(field.getName() + ":" + field.get(responseObject));
                }
            }
        }
    }


}
