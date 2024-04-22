package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.FileBodyWrapper;
import com.zoho.crm.api.bulkread.ResponseHandler;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;
import com.zoho.crm.api.util.StreamWrapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBulkDownload {

    public static void downloadResult(Long jobId) throws Exception {
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();
        APIResponse<ResponseHandler> response           = bulkReadOperations.downloadResult(jobId);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                System.out.println(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
                return;
            }
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof FileBodyWrapper) {

                    FileBodyWrapper fileBodyWrapper = (FileBodyWrapper) responseHandler;
                    StreamWrapper   streamWrapper   = fileBodyWrapper.getFile();
                    File            file            = new File(streamWrapper.getName());
                    InputStream inputStream = streamWrapper.getStream();
                    try {
                        FileUtils.copyInputStreamToFile(inputStream, file);
                    } catch (Exception e) {
                        System.out.println("Balen.");
                    }
                    inputStream.close();
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
