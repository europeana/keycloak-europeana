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
import com.zoho.crm.api.tags.Tag;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;


/**
 * @author provided by Zoho in the samples directory Created on 12 feb 2024
 */
public class GetRecords {


    private static final Logger LOG        = Logger.getLogger(GetRecords.class);
    private static final String LOG_PREFIX = "ZOHO_GET_RECORDS:";


    public static String getRecords(String moduleAPIName) throws Exception {

        LOG.info("started GetRecords");
        StringBuffer     sb               = new StringBuffer("Bericht van Zoho: ");
        RecordOperations recordOperations = new RecordOperations(moduleAPIName);

        ParameterMap paramInstance = new ParameterMap();
        paramInstance.add(GetRecordsParam.APPROVED, "both");
        HeaderMap      headerInstance  = new HeaderMap();
        OffsetDateTime ifmodifiedsince = OffsetDateTime.of(2019, 05, 20, 10, 00, 01, 00, ZoneOffset.of("+05:30"));
        headerInstance.add(GetRecordsHeader.IF_MODIFIED_SINCE, ifmodifiedsince);
        //Call getRecords method that takes moduleAPIName, paramInstance and headerInstance as parameter.
        APIResponse<ResponseHandler> response = recordOperations.getRecords(paramInstance, headerInstance);


        LOG.info("Response from Zoho:" + response.getStatusCode());

        if (response != null) {
            //Get the status code from response
            sb.append("\n").append("Status Code: ").append(response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                sb.append("\n").append(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
                return sb.toString();
            }

            //Check if expected response is received
            if (response.isExpected()) {
                //Get the object from response
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper) {
                    //Get the received ResponseWrapper instance
                    ResponseWrapper responseWrapper = (ResponseWrapper) responseHandler;
                    //Get the obtained Record instances
                    List<com.zoho.crm.api.record.Record> records = responseWrapper.getData();
                    for (com.zoho.crm.api.record.Record record : records) {
                        //Get the ID of each Record
                        sb.append("\n").append("Record ID: ").append(record.getId());


                        //Get the ModifiedTime of each Record
                        sb.append("\n").append("Record ModifiedTime: ").append(record.getModifiedTime());
                        //Get the list of Tag instance each Record
                        List<Tag> tags = record.getTag();
                        //Check if tags is not null
                        if (tags != null) {
                            for (Tag tag : tags) {
                                //Get the Name of each Tag
                                sb.append("\n").append("Record Tag Name: ").append(tag.getName());
                                //Get the Id of each Tag
                                sb.append("\n").append("Record Tag ID: ").append(tag.getId());
                            }
                        }
                        //To get particular field value
                        sb.append("\n").append("Record Field Value: ").append(
                            record.getKeyValue("Last_Name"));
                    }
                    //Get the Object obtained Info instance
                    Info info = responseWrapper.getInfo();
                    //Check if info is not null
                    if (info != null) {
                        if (info.getPerPage() != null) {
                            //Get the PerPage of the Info
                            sb.append("\n").append("Record Info PerPage: ").append(info.getPerPage().toString());
                        }
                        if (info.getCount() != null) {
                            //Get the Count of the Info
                            sb.append("\n").append("Record Info Count: ").append(info.getCount().toString());
                        }
                        if (info.getPage() != null) {
                            //Get the Page of the Info
                            sb.append("\n").append("Record Info Page: ").append(info.getPage().toString());
                        }
                        if (info.getMoreRecords() != null) {
                            //Get the MoreRecords of the Info
                            sb.append("\n").append("Record Info MoreRecords: ").append(
                                info.getMoreRecords().toString());
                        }
                    }
                }
                //Check if the request returned an exception
                else if (responseHandler instanceof APIException) {
                    //Get the received APIException instance
                    APIException exception = (APIException) responseHandler;
                    //Get the Status
                    sb.append("Status: ").append(exception.getStatus().getValue());
                    //Get the Code
                    sb.append("Code: ").append(exception.getCode().getValue());
                    sb.append("\nDetails: \n");
                    //Get the details map
                    for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        //Get each value in the map
                        sb.append("\n").append(entry.getKey()).append("Message: ").append(exception.getMessage().getValue());
                    }
                    //Get the Message
                    sb.append("\n").append("Message: ").append(exception.getMessage().getValue());
                }
            } else {//If response is not as expected
                //Get model object from response
                Model responseObject = response.getModel();
                //Get the response object's class
                Class<? extends Model> clas = responseObject.getClass();
                //Get all declared fields of the response class
                java.lang.reflect.Field[] fields = clas.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    //Get each value
                    sb.append("\n").append(field.getName()).append(":").append(field.get(responseObject));
                }
            }
        }
        return sb.toString();
    }

}
