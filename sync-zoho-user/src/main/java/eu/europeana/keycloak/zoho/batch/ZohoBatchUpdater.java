package eu.europeana.keycloak.zoho.batch;

import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.APIException;
import com.zoho.crm.api.record.ActionHandler;
import com.zoho.crm.api.record.ActionResponse;
import com.zoho.crm.api.record.ActionWrapper;
import com.zoho.crm.api.record.BodyWrapper;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.SuccessResponse;
import com.zoho.crm.api.util.APIResponse;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Handles operations related to batch update call towards ZOHO
 */
public class ZohoBatchUpdater {
  private static final Logger LOG = Logger.getLogger(ZohoBatchUpdater.class);
  public static final int BATCH_SIZE = 100;

  /**
   * Updates records in zoho in batches.
   * @param records records to update
   * @param moduleName Zoho module name
   * @throws SDKException
   */
  public void updateInBatches(List<Record> records, String moduleName) throws SDKException {
    if(records == null || records.isEmpty()) return;
    int totalRecords = records.size();
    LOG.info("Batch update process started for module '"+moduleName+"'");
    LOG.info("Total number of records to update - " +  totalRecords);
    for (int i = 0; i <= totalRecords; i += BATCH_SIZE) {
      //call the zoho update in batches
      int endIndex = Math.min(i + BATCH_SIZE, totalRecords);
      //Extract number of records equivalent to batch size
      List<Record> currentBatch = records.subList(i, endIndex);
      //call zoho update
      LOG.info("Processing batch number "+ ((i/BATCH_SIZE)+1) + " Records from - " + (i+1) + " To- "+ endIndex);
      if (!callZohoBatchUpdate(currentBatch, moduleName)) {
        LOG.error("Further batches if any ,wont be processed due to failure");
        return;
      }
    }
    LOG.info("Batch update process finished.");
  }

  /**
   * Calls Zoho to update the List of records specified in input.
   * @param records List of records to update
   * @param moduleAPIName zoho module to update e.g.Contacts,Accounts etc.
   * @return boolean TRUE if the call is successful , else FALSE
   * @throws SDKException
   */
  private boolean callZohoBatchUpdate(List<Record> records, String moduleAPIName) throws SDKException {
    // trigger zoho api call for the elements in the list
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper request = new BodyWrapper();
    request.setData(records);
    APIResponse<ActionHandler> response = recordOperations.updateRecords(request, new HeaderMap());
    return processResponse(response);
  }

  /**
   * Process the APIResponse returned from ZOHO.
   * Checks if call is success and Logs the error
   * @param response
   * @return  boolean 'true' if response is ok
   */
  public boolean processResponse(APIResponse<ActionHandler> response) {
    if (response != null && response.isExpected()) {
      if (response.getObject() instanceof ActionWrapper actionWrapper) {
        for (ActionResponse actionResponse : actionWrapper.getData()) {
          if (actionResponse instanceof SuccessResponse) {
            return true;
          } else if (actionResponse instanceof APIException exception) {
            LOG.error("Status: " + exception.getStatus().getValue() +
                " Code:" + exception.getCode().getValue() +
                " Message: " + exception.getMessage().getValue());
            return false;
          }
        }
      } else if (response.getObject() instanceof APIException exception) {
        LOG.error(" Status: " + exception.getStatus().getValue() + " Code:" + exception.getCode()
            .getValue() + " Message: " + exception.getMessage().getValue());
        return false;
      }
    }
    LOG.error("Unexpected response from zoho");
    return false;
  }
}