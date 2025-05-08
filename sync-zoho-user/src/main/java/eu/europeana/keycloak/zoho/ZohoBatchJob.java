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


    public String ZohoBulkCreateJob(String moduleAPIName) throws Exception {

        String jobID = "";
        BulkReadOperations bulkReadOperations = new BulkReadOperations();
        BodyWrapper        bodyWrapper        = new BodyWrapper();
        MinifiedModule     module             = new MinifiedModule();
        module.setAPIName(moduleAPIName);

        Query query = new Query();
        query.setModule(module);
        List<String> fieldAPINames = new ArrayList<String>();

        if (StringUtils.equalsIgnoreCase(moduleAPIName, CONTACTS)){
            fieldAPINames.add("First_Name");
            fieldAPINames.add("Last_Name");
            fieldAPINames.add("Full_Name");
            fieldAPINames.add("Account_Name");
            fieldAPINames.add("Email");
            fieldAPINames.add("Secondary_Email");
            fieldAPINames.add("Lead_Source");
            fieldAPINames.add("User_Account_ID");
        } else {
            fieldAPINames.add("Account_Name");
            fieldAPINames.add("Europeana_org_ID");
        }

        fieldAPINames.add("Modified_Time");
        query.setFields(fieldAPINames);
        query.setPage(1);
        bodyWrapper.setQuery(query);

        APIResponse<ActionHandler> response = bulkReadOperations.createBulkReadJob(bodyWrapper);
        if (response != null) {
            if (response.isExpected()) {
                ActionHandler actionHandler = response.getObject();
                if (actionHandler instanceof ActionWrapper) {
                    ActionWrapper        actionWrapper   = (ActionWrapper) actionHandler;
                    List<ActionResponse> actionResponses = actionWrapper.getData();

                    for (ActionResponse actionResponse : actionResponses) {
                        if (actionResponse instanceof SuccessResponse) {
                            SuccessResponse successResponse = (SuccessResponse) actionResponse;
                            for (Entry<String, Object> entry : successResponse.getDetails().entrySet()) {
                                if (entry.getKey().equalsIgnoreCase("id")){
                                    jobID = entry.getValue().toString();
                                }
                            }
                            LOG.info(moduleAPIName + " batch download job " + successResponse.getMessage().getValue().toLowerCase());
                            return jobID;

                        } else if (actionResponse instanceof APIException) {
                            APIException exception = (APIException) actionResponse;
                            LOG.error("Status: " + exception.getStatus().getValue());
                            LOG.error("Code: " + exception.getCode().getValue());
                            LOG.error("Details: ");
                            for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
                                LOG.error(entry.getKey() + ": " + entry.getValue());
                            }
                            LOG.error("Error occurred creating bulk job: " + exception.getMessage());

                        }
                    }
                } else if (actionHandler instanceof APIException) {
                    APIException exception = (APIException) actionHandler;
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
}
