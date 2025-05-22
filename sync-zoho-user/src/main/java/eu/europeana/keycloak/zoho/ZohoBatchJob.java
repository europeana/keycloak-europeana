package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.ActionHandler;
import com.zoho.crm.api.bulkread.ActionResponse;
import com.zoho.crm.api.bulkread.ActionWrapper;
import com.zoho.crm.api.bulkread.BodyWrapper;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.Query;
import com.zoho.crm.api.bulkread.SuccessResponse;
import com.zoho.crm.api.modules.MinifiedModule;
import com.zoho.crm.api.util.APIResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBatchJob {
    private static final Logger LOG = Logger.getLogger(ZohoBatchJob.class);

    private static final String CONTACTS = "Contacts";
    private static final String ACCOUNTS = "Accounts";


    public String zohoBulkCreateJob(String moduleAPIName) throws Exception {

        String jobID = "";
        BulkReadOperations bulkReadOperations = new BulkReadOperations();
        BodyWrapper        bodyWrapper        = new BodyWrapper();
        MinifiedModule     module             = new MinifiedModule();
        module.setAPIName(moduleAPIName);

        Query query = new Query();
        query.setModule(module);
       List<String> fieldAPINames = populateFieldsList(moduleAPIName);
      query.setFields(fieldAPINames);
        query.setPage(1);
        bodyWrapper.setQuery(query);

        APIResponse<ActionHandler> response = bulkReadOperations.createBulkReadJob(bodyWrapper);
        if (response != null) {
            if (response.isExpected()) {
                ActionHandler actionHandler = response.getObject();
                if (actionHandler instanceof ActionWrapper actionWrapper) {
                  List<ActionResponse> actionResponses = actionWrapper.getData();

                    for (ActionResponse actionResponse : actionResponses) {
                        if (actionResponse instanceof SuccessResponse successResponse) {
                          for (Entry<String, Object> entry : successResponse.getDetails().entrySet()) {
                                if ("id".equalsIgnoreCase(entry.getKey())){
                                    jobID = entry.getValue().toString();
                                }
                            }
                            LOG.info(moduleAPIName + " batch download job " + successResponse.getMessage().getValue().toLowerCase());
                            return jobID;

                        } else if (actionResponse instanceof APIException exception) {
                          LOG.error("Status: " + exception.getStatus().getValue());
                            LOG.error("Code: " + exception.getCode().getValue());
                            LOG.error("Details: ");
                            for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
                                LOG.error(entry.getKey() + ": " + entry.getValue());
                            }
                            LOG.error("Error occurred creating bulk job: " + exception.getMessage());

                        }
                    }
                } else if (actionHandler instanceof APIException exception) {
                  LOG.error("Status: " + exception.getStatus().getValue());
                    LOG.error("Code: " + exception.getCode().getValue());
                    LOG.error("Details: ");
                    for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        LOG.error(entry.getKey() + ": " + entry.getValue());
                    }
                    LOG.error("Error occurred creating bulk job: " + exception.getMessage());
                }
            } else {
                LOG.error("No usable response received");
            }
        }
        return jobID;
    }

  private static List<String> populateFieldsList(String moduleAPIName) {
    List<String> fieldAPINames = new ArrayList<>();

    if (StringUtils.equalsIgnoreCase(moduleAPIName, CONTACTS)){
        fieldAPINames.add("First_Name");
        fieldAPINames.add("Last_Name");
        fieldAPINames.add("Full_Name");
        fieldAPINames.add("Account_Name");
        fieldAPINames.add("Email");
        fieldAPINames.add("Secondary_Email");
        fieldAPINames.add("Lead_Source");
        fieldAPINames.add("User_Account_ID");
        fieldAPINames.add("Contact_Participation");
    } else if(StringUtils.equalsIgnoreCase(moduleAPIName, ACCOUNTS)) {
        fieldAPINames.add("Account_Name");
        fieldAPINames.add("Europeana_org_ID");
    }

    fieldAPINames.add("Modified_Time");
    return fieldAPINames;
  }
}