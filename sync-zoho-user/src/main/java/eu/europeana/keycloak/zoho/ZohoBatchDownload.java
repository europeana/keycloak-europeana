package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.FileBodyWrapper;
import com.zoho.crm.api.bulkread.JobDetail;
import com.zoho.crm.api.bulkread.ResponseHandler;
import com.zoho.crm.api.bulkread.ResponseWrapper;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.StreamWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBatchDownload {

    private static final int WAIT_A_SECOND = 1000;
    private static final int NO_CONTENT = 204;
    private static final int NOT_MODIFIED = 304;
    private static final int MAX_LOOPS = 20;
    private static final String FAILED_CREATING_DIR = "failed to create directory ";
    private static final Logger LOG = Logger.getLogger(ZohoBatchDownload.class);
    private static final String COMPLETED = "COMPLETED";

    public String downloadResult(Long jobId) throws Exception {
        String jobStatus = null;
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();
        int maxLoops = MAX_LOOPS;
        int loops = 0;
        while (loops < maxLoops) {
            APIResponse<ResponseHandler> response = bulkReadOperations.getBulkReadJobDetails(jobId);
            if (response != null && response.isExpected() && response.getObject() instanceof ResponseWrapper ) {
                checkJobDetails(response, jobId);
            }
            loops ++;
            LOG.debug("Job not yet finished in loop " + loops + ". Retrying in one second.");
            Thread.sleep(WAIT_A_SECOND);
        }
        return "";
    }

    private String checkJobDetails(APIResponse<ResponseHandler> response, Long jobId) throws Exception {
        for (JobDetail jobDetail : ((ResponseWrapper) response.getObject()).getData()){
            if (StringUtils.equalsIgnoreCase(jobDetail.getState().getValue(), COMPLETED)){
                return downloadCompleted(jobId);
            }
        }
        return null;
    }


    private String downloadCompleted(Long jobId) throws Exception {
        String jobStatus = null;
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();
        APIResponse<ResponseHandler> response           = bulkReadOperations.downloadResult(jobId);
        if (response != null) {
            if (Arrays.asList(NO_CONTENT, NOT_MODIFIED).contains(response.getStatusCode())) {
                LOG.error(response.getStatusCode() == NO_CONTENT ? "No Content" : "Not Modified");
                return "";
            }
            if (response.isExpected()) {
                processResponse(response);
            } else {
                LOG.error("No usable response received");
            }
        }
        return "";
    }

    private String processResponse(APIResponse<ResponseHandler> response) throws IOException {
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
                LOG.error("Error downloading batch job: " + e.getMessage());
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
            LOG.error("Error downloading batch job: " + exception.getMessage());
        }
        return null;
    }

    public static String unZipFile(String pathToZipFile) throws Exception {
        Path   zipFilePath = Paths.get(pathToZipFile);
        Path   zipDir      = zipFilePath.getParent();
        //Open the file
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            //We will unzip files in this folder
            if (!zipDir.toFile().isDirectory() && !zipDir.toFile().mkdirs()) {
                throw new IOException(FAILED_CREATING_DIR + zipDir);
            }
            processZipEntries(zipEntries, zipDir, zipFilePath, zipFile);
        } catch (IOException e) {
            LOG.error("IOException occurred:" + e.getMessage());
        }
        return null;
    }

    private static String processZipEntries(Enumeration<? extends ZipEntry> zipEntries, Path zipDir, Path zipFilePath, ZipFile zipFile) throws IOException {
        //Iterate over zipEntries
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();
            File unzippedFile = new File(zipDir.resolve(Path.of(zipEntry.getName())).toString());
            //If directory then create a new directory in uncompressed folder
            if (zipEntry.isDirectory() && !unzippedFile.isDirectory() && !unzippedFile.mkdirs()) {
                throw new IOException(FAILED_CREATING_DIR + unzippedFile);
            } else {
                File unzippedParent = unzippedFile.getParentFile();
                if (!unzippedParent.isDirectory() && !unzippedParent.mkdirs()) {
                    throw new IOException(FAILED_CREATING_DIR + unzippedParent);
                }
                try(InputStream in = zipFile.getInputStream(zipEntry)) {
                    Files.copy(in, unzippedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(zipFilePath);
                    return unzippedFile.getCanonicalPath();
                } catch (Exception e) {
                    LOG.error("Error unzipping batch job file:" + e.getMessage());
                }
            }
        }
        return null;
    }



}
