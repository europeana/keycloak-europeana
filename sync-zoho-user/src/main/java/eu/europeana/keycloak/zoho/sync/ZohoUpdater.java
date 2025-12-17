package eu.europeana.keycloak.zoho.sync;

import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.record.*;
import com.zoho.crm.api.util.APIResponse;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles operations related to update calls towards ZOHO
 */
public class ZohoUpdater {
  private static final Logger LOG = Logger.getLogger(ZohoUpdater.class);
  public static final int BATCH_SIZE = 100;

  /**
   * Updates records in zoho in batches.
   * @param records records to update
   * @param moduleName Zoho module name
   * @throws SDKException
   */
  public void updateInBatches(List<Record> records, String moduleName) throws SDKException {
    if(records == null || records.isEmpty()) {
      return;
    }
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
      if (!callZohoBatchUpdate(new ArrayList<>(currentBatch), moduleName)) {
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


  /**This method is used to update a single contact of zoho with ID and print the response.
   * @param recordId - The Zoho ID of the record to be updated.   *
   * @return boolean true if contact updated successfully
   * @throws SDKException -
   */
  public boolean callZohoUpdate(long recordId,  Record recordToUpdate) throws SDKException {
    List<Record> records = new ArrayList<>();
    records.add(recordToUpdate);
    RecordOperations recordOperations = new RecordOperations("Contacts");
    BodyWrapper request = new BodyWrapper();
    request.setData(records);
    LOG.info("Updating  zoho contact id :" + recordId);
    APIResponse<ActionHandler> response = recordOperations.updateRecord(recordId, request,new HeaderMap());
    return processResponse(response);
  }


  /**This method is used to create a single contact of zoho with ID and print the response.
   * @param newRecord request object
   * @return boolean true if contact created successfully
   * @throws SDKException -
   * */
  public boolean createNewZohoContact(Record newRecord) throws SDKException {
    String moduleAPIName = "Contacts";
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper bodyWrapper = new BodyWrapper();

    List<Record> records = new ArrayList<>();
    records.add(newRecord);
    bodyWrapper.setData(records);

    HeaderMap headerInstance = new HeaderMap();
    APIResponse<ActionHandler> response = recordOperations.createRecords(bodyWrapper,
            headerInstance);
    return processResponse(response);
  }


  /**
   * Process the APIResponse returned from ZOHO.
   * Checks if call is success and Logs the error
   * @param response APIResponse
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