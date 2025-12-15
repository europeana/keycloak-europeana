package eu.europeana.keycloak.zoho.batch;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.FileBodyWrapper;
import com.zoho.crm.api.bulkread.JobDetail;
import com.zoho.crm.api.bulkread.ResponseHandler;
import com.zoho.crm.api.bulkread.ResponseWrapper;
import com.zoho.crm.api.exception.SDKException;
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
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 22/04/2024.
 */
@SuppressWarnings("javasecurity:S6096")
public class ZohoBatchDownload {
    private static final Logger LOG = Logger.getLogger(ZohoBatchDownload.class);
    private static final String COMPLETED = "COMPLETED";
    public String downloadResult(Long jobId,String module) throws SDKException {
        BulkReadOperations bulkReadOperations = new BulkReadOperations();
        int maxLoops = 25;
        int loops = 0;
        while (loops < maxLoops) {
            APIResponse<ResponseHandler> response = bulkReadOperations.getBulkReadJobDetails(jobId);
            if (response != null && response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof ResponseWrapper responseWrapper
                        && isJobCompleted(responseWrapper)) {
                    return downloadCompleted(jobId);
                }
            }
            loops++;
            LOG.debug("Job not yet finished in loop " + loops + ". Retrying in one second.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.error(e.getStackTrace());
                Thread.currentThread().interrupt();
            }
        }
        LOG.error("Retries exhausted ! Status of Bulk download job ID- " +jobId+ " for module "+module+" was not complete!");
        return "";
    }

    private boolean isJobCompleted(ResponseWrapper responseWrapper) {
        List<JobDetail> jobDetails = responseWrapper.getData();
        for (JobDetail jobDetail : jobDetails){
            if (StringUtils.equalsIgnoreCase(jobDetail.getState().getValue(), COMPLETED)){
                return true;
            }
        }
        return false;
    }

    private String downloadCompleted(Long jobId) throws SDKException {
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();
        APIResponse<ResponseHandler> response           = bulkReadOperations.downloadResult(jobId);
        if (response != null) {
            if (Arrays.asList(HttpStatus.SC_NO_CONTENT, HttpStatus.SC_NOT_MODIFIED).contains(response.getStatusCode())) {
                LOG.error(response.getStatusCode() == HttpStatus.SC_NO_CONTENT ? "No Content" : "Not Modified");
                return "";
            }
            if (response.isExpected()) {
                ResponseHandler responseHandler = response.getObject();
                if (responseHandler instanceof FileBodyWrapper fileBodyWrapper)
                    return getDownloadedFilePath(fileBodyWrapper);
                else if (responseHandler instanceof APIException exception) {
                    logExceptionDetails(exception);
                }
            } else {
                LOG.error("No usable response received");
            }
        }
        return "";
    }

    private static String getDownloadedFilePath(FileBodyWrapper fileBodyWrapper) {
        StreamWrapper   streamWrapper   = fileBodyWrapper.getFile();
        try(InputStream inputStream = streamWrapper.getStream()) {
            File file = new File(streamWrapper.getName());
            FileUtils.copyInputStreamToFile(inputStream, file);
            return unZipFile(file.getCanonicalPath());
        } catch (Exception e) {
            LOG.error("Error downloading batch job: " + e.getMessage());
            return null;
        }
    }

    private static void logExceptionDetails(APIException exception) {
        LOG.error("Status: " + exception.getStatus().getValue());
        LOG.error("Code: " + exception.getCode().getValue());
        LOG.error("Details: ");
        for (Entry<String, Object> entry : exception.getDetails().entrySet()) {
            LOG.error(entry.getKey() + ": " + entry.getValue());
        }
        LOG.error("Error downloading batch job: " + exception.getMessage());
    }

    public static String unZipFile(String pathToZipFile) {
        Path   zipFilePath = Paths.get(pathToZipFile);
        Path   zipDir      = zipFilePath.getParent();
        //Open the file
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            //We will unzip files in this folder
            if (!zipDir.toFile().isDirectory()
                && !zipDir.toFile().mkdirs()) {
                throw new IOException("failed to create directory " + zipDir);
            }
            // Get the canonical path of the destination directory for validation
            Path canonicalZipDir = zipDir.toRealPath();
            //Iterate over zipEntries
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipEntries.nextElement();
                String entryName = zipEntry.getName();

                // Construct the target file path
                Path targetPath = zipDir.resolve(entryName);
                Path canonicalTargetPath = targetPath.toAbsolutePath();
                if (!canonicalTargetPath.startsWith(canonicalZipDir)) {
                    throw new IOException("Illegal zip entry path: " + entryName + " attempts to write outside the target directory.");
                }

                File unzippedFile = canonicalTargetPath.toFile();
                //If directory then create a new directory in uncompressed folder
                String filePath = getUnzippedFilePath(zipEntry, unzippedFile,
                    zipFile, zipFilePath);
                if (filePath != null) {
                    return filePath;
                }
            }
        } catch (IOException e) {
            LOG.error("IOException occurred:" + e.getMessage());
        }
        return null;
    }

    private static String getUnzippedFilePath(ZipEntry zipEntry, File unzippedFile,
         ZipFile zipFile, Path zipFilePath) throws IOException {

        String directoryCreationFailed = "failed to create directory ";
        if (zipEntry.isDirectory()) {
            if (!unzippedFile.isDirectory() && !unzippedFile.mkdirs()) {
                throw new IOException(directoryCreationFailed + unzippedFile);
            }
        }
        //Else create the file
        else {
            File unzippedParent = unzippedFile.getParentFile();
            if (!unzippedParent.isDirectory() && !unzippedParent.mkdirs()) {
                throw new IOException(directoryCreationFailed + unzippedParent);
            }
          return createAndgetUnzipFileFilePath(zipFile, zipEntry, unzippedFile,
              zipFilePath);
        }
        return null;
    }

    private static String createAndgetUnzipFileFilePath(ZipFile zipFile, ZipEntry zipEntry, File unzippedFile,
        Path zipFilePath) {
        try(InputStream in = zipFile.getInputStream(zipEntry)) {
            Files.copy(in, unzippedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(zipFilePath);
            return unzippedFile.getCanonicalPath();
        } catch (Exception e) {
            LOG.error("Error unzipping batch job file:" + e.getMessage());
        }
        return null;
    }



}