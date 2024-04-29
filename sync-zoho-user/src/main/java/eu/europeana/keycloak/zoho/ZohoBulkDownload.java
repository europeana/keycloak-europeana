package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.FileBodyWrapper;
import com.zoho.crm.api.bulkread.JobDetail;
import com.zoho.crm.api.bulkread.ResponseHandler;
import com.zoho.crm.api.bulkread.ResponseWrapper;
import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;
import com.zoho.crm.api.util.StreamWrapper;
import java.io.File;
import java.io.InputStream;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBulkDownload {

    private static final Logger LOG = Logger.getLogger(ZohoBulkDownload.class);
    private final static String COMPLETED = "COMPLETED";

    public String downloadResult(Long jobId) throws Exception {
        String jobStatus = null;
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();
        int maxLoops = 20;
        int loops = 0;
        while (loops < maxLoops) {
            APIResponse<ResponseHandler> response = bulkReadOperations.getBulkReadJobDetails(jobId);
            if (response != null) {
                if (response.isExpected()) {
                    ResponseHandler responseHandler = response.getObject();
                    if (responseHandler instanceof ResponseWrapper){
                        ResponseWrapper responseWrapper = (ResponseWrapper) responseHandler;
                        List<JobDetail> jobDetails      = responseWrapper.getData();
                        for (JobDetail jobDetail : jobDetails){
                            if (StringUtils.equalsIgnoreCase(jobDetail.getState().getValue(), COMPLETED)){
                                return downloadCompleted(jobId);
                            }
                        }
                    }

                }
            }
            loops ++;
            LOG.debug("Job not yet finished in loop " + loops + ". Retrying in one second.");
            Thread.sleep(1000);
        }
        return "";
    }

    private String downloadCompleted(Long jobId) throws Exception {
        String jobStatus = null;
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();

        APIResponse<ResponseHandler> response           = bulkReadOperations.downloadResult(jobId);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                LOG.error(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
                return "";
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
                        return unZipFile(file.getCanonicalPath());
                    } catch (Exception e) {
                        LOG.error("Error downloading bulk job: " + e.getMessage());
                    }
                    inputStream.close();
                } else if (responseHandler instanceof APIException) {
                    APIException exception = (APIException) responseHandler;
                    LOG.error("Status: " + exception.getStatus().getValue());
                    LOG.error("Code: " + exception.getCode().getValue());
                    LOG.error("Details: ");
                    for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
                        LOG.error(entry.getKey() + ": " + entry.getValue());
                    }
                    LOG.error("Error downloading bulk job: " + exception.getMessage());
                }
            } else {
                LOG.error("No usable response received");
            }
        }
        return "";
    }

    public static String unZipFile(String pathToZipFile) throws Exception {

        Path   zipFilePath = Paths.get(pathToZipFile);
        Path   zipDir      = zipFilePath.getParent();
        String zipFileName = zipFilePath.toFile().getName();

        //Open the file
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {

            FileSystem                      fileSystem = FileSystems.getDefault();
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

            //We will unzip files in this folder
            if (!zipDir.toFile().isDirectory()
                && !zipDir.toFile().mkdirs()) {
                throw new IOException("failed to create directory " + zipDir);
            }

            //Iterate over zipEntries
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipEntries.nextElement();

                File unzippedFile = new File(zipDir.resolve(Path.of(zipEntry.getName())).toString());

                //If directory then create a new directory in uncompressed folder
                if (zipEntry.isDirectory()) {
                    if (!unzippedFile.isDirectory() && !unzippedFile.mkdirs()) {
                        throw new IOException("failed to create directory " + unzippedFile);
                    }
                }

                //Else create the file
                else {
                    File unzippedParent = unzippedFile.getParentFile();
                    if (!unzippedParent.isDirectory() && !unzippedParent.mkdirs()) {
                        throw new IOException("failed to create directory " + unzippedParent);
                    }

                    try(InputStream in = zipFile.getInputStream(zipEntry)) {
                        Files.copy(in, unzippedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(zipFilePath);
                        return unzippedFile.getCanonicalPath();
                    } catch (Exception e) {
                        LOG.error("Error unzipping bulk job file:" + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("IOException occurred:" + e.getMessage());
        }
        return null;
    }

}
