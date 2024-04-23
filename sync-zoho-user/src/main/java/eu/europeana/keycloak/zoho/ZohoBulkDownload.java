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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBulkDownload {

    public void downloadResult(Long jobId) throws Exception {
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
                        extractZip(file.getCanonicalPath());
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

    private static void extractZip(String canonicalPath) {
        Path cPath = Paths.get(canonicalPath);
        Path parentDir = cPath.getParent();
        String fileName = cPath.toFile().getName();
//        Path targetDir = parentDir.resolve(FilenameUtils.removeExtension(fileName));

        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

        try (InputStream inputStream = Files.newInputStream(cPath);
             ArchiveInputStream archiveInputStream = archiveStreamFactory
                     .createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream)) {

            ArchiveEntry archiveEntry = null;
            while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
                Path path = Paths.get(parentDir.toString(), archiveEntry.getName());
                File file = path.toFile();
                if (archiveEntry.isDirectory()) {
                    if (!file.isDirectory()) {
                        file.mkdirs();
                    }
                } else {
                    File parent = file.getParentFile();
                    if (!parent.isDirectory()) {
                        parent.mkdirs();
                    }
                    try (OutputStream outputStream = Files.newOutputStream(path)) {
                        IOUtils.copy(archiveInputStream, outputStream);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ArchiveException e) {
            throw new RuntimeException(e);
        }
    }


}
