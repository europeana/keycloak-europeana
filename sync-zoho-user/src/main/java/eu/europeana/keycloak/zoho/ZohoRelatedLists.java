package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.relatedlists.APIException;
import com.zoho.crm.api.relatedlists.RelatedList;
import com.zoho.crm.api.relatedlists.RelatedListsOperations;
import com.zoho.crm.api.relatedlists.RelatedListsOperations.GetRelatedListsParam;
import com.zoho.crm.api.relatedlists.ResponseHandler;
import com.zoho.crm.api.relatedlists.ResponseWrapper;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by luthien on 17/04/2024.
 */
public class ZohoRelatedLists {

    private static final Long   INSTLAYOUTID    = 486281000000032035L;

    public void getRelatedLists() throws Exception {

        RelatedListsOperations relatedListsOperations = new RelatedListsOperations(INSTLAYOUTID);
        ParameterMap           paramInstance          = new ParameterMap();
        paramInstance.add(GetRelatedListsParam.MODULE, "Accounts");

        APIResponse<ResponseHandler> response = relatedListsOperations.getRelatedLists(paramInstance);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                System.out.println(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
                return;
            }
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    ResponseWrapper   responseWrapper = (ResponseWrapper) responseHandler;
                    List<RelatedList> relatedLists    = responseWrapper.getRelatedLists();
                    for (com.zoho.crm.api.relatedlists.RelatedList relatedList : relatedLists) {
                        System.out.println("RelatedList SequenceNumber: " + relatedList.getSequenceNumber());
                        System.out.println("RelatedList DisplayLabel: " + relatedList.getDisplayLabel());
                        System.out.println("RelatedList APIName: " + relatedList.getAPIName());
                        System.out.println("RelatedList Module: " + relatedList.getModule());
                        System.out.println("RelatedList Name: " + relatedList.getName());
                        System.out.println("RelatedList Action: " + relatedList.getAction());
                        System.out.println("RelatedList ID: " + relatedList.getId());
                        System.out.println("RelatedList Href: " + relatedList.getHref());
                        System.out.println("RelatedList Type: " + relatedList.getType());
                        System.out.println("RelatedList Connectedmodule: " + relatedList.getConnectedmodule());
                        System.out.println("RelatedList Linkingmodule: " + relatedList.getLinkingmodule());
                    }
                } else if (responseHandler instanceof APIException) {
                    APIException exception = (APIException) responseHandler;
                    System.out.println("Status: " + exception.getStatus().getValue());
                    System.out.println("Code: " + exception.getCode().getValue());
                    System.out.println("Details: ");
                    for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    System.out.println("Message: " + exception.getMessage());
                }
            } else {
                Model                  responseObject = response.getModel();
                Class<? extends Model> clas           = responseObject.getClass();
                java.lang.reflect.Field[] fields         = clas.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    System.out.println(field.getName() + ":" + field.get(responseObject));
                }
            }
        }
    }
}
