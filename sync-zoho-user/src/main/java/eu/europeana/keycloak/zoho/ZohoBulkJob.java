package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.ActionHandler;
import com.zoho.crm.api.bulkread.ActionResponse;
import com.zoho.crm.api.bulkread.ActionWrapper;
import com.zoho.crm.api.bulkread.BodyWrapper;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.CallBack;
import com.zoho.crm.api.bulkread.Query;
import com.zoho.crm.api.bulkread.SuccessResponse;
import com.zoho.crm.api.modules.MinifiedModule;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Choice;
import com.zoho.crm.api.util.Model;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBulkJob {


    public String ZohoBulkCreateJob(String moduleAPIName) throws Exception {

        String jobID = "";

        BulkReadOperations bulkReadOperations = new BulkReadOperations();
        BodyWrapper        bodyWrapper        = new BodyWrapper();
        MinifiedModule     module             = new MinifiedModule();
        module.setAPIName(moduleAPIName);
//        CallBack callback = new CallBack();
//        callback.setUrl("https://www.example.com/callback");
//        callback.setMethod(new Choice<String>("post"));
//        bodyWrapper.setCallback(callback);

        Query query = new Query();
        query.setModule(module);
//		query.setCvid(347706108701l);
//        List<String> fieldAPINames = new ArrayList<String>();
//        fieldAPINames.add("Last_Name");
//        query.setFields(fieldAPINames);
        query.setPage(1);


        bodyWrapper.setQuery(query);
        // requestWrapper.setFileType(new Choice<String>("ics"));

        APIResponse<ActionHandler> response = bulkReadOperations.createBulkReadJob(bodyWrapper);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (response.isExpected()) {
                ActionHandler actionHandler = response.getObject();
                if (actionHandler instanceof ActionWrapper) {
                    ActionWrapper        actionWrapper   = (ActionWrapper) actionHandler;
                    List<ActionResponse> actionResponses = actionWrapper.getData();

                    for (ActionResponse actionResponse : actionResponses) {
                        if (actionResponse instanceof SuccessResponse) {
                            SuccessResponse successResponse = (SuccessResponse) actionResponse;
                            System.out.println("Status: " + successResponse.getStatus().getValue());
                            System.out.println("Code: " + successResponse.getCode().getValue());
                            System.out.println("Details: ");
                            for (Entry<String, Object> entry : successResponse.getDetails().entrySet()) {
                                System.out.println(entry.getKey() + ": " + entry.getValue());
                                if (entry.getKey().equalsIgnoreCase("id")){
                                    jobID = entry.getValue().toString();
                                }
                            }
                            System.out.println("Message: " + successResponse.getMessage().getValue());
                            return jobID;

                        } else if (actionResponse instanceof APIException) {

                            APIException exception = (APIException) actionResponse;
                            System.out.println("Status: " + exception.getStatus().getValue());
                            System.out.println("Code: " + exception.getCode().getValue());
                            System.out.println("Details: ");
                            for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
                                System.out.println(entry.getKey() + ": " + entry.getValue());
                            }
                            System.out.println("Message: " + exception.getMessage());

                        }
                    }
                } else if (actionHandler instanceof APIException) {
                    APIException exception = (APIException) actionHandler;
                    System.out.println("Status: " + exception.getStatus().getValue());
                    System.out.println("Code: " + exception.getCode().getValue());
                    System.out.println("Details: ");
                    for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    System.out.println("Message: " + exception.getMessage());
                }
            } else {
                Model                  responseObject = response.getModel();
                Class<? extends Model> clas           = responseObject.getClass();
                Field[]                fields         = clas.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    System.out.println(field.getName() + ":" + field.get(responseObject));
                }
            }
        }
        return jobID;
    }


}
