package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.bulkread.APIException;
import com.zoho.crm.api.bulkread.BulkReadOperations;
import com.zoho.crm.api.bulkread.FileBodyWrapper;
import com.zoho.crm.api.bulkread.ResponseHandler;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoBulkDownload {

    public String downloadResult(Long jobId) throws Exception {
        BulkReadOperations           bulkReadOperations = new BulkReadOperations();
        APIResponse<ResponseHandler> response           = bulkReadOperations.downloadResult(jobId);
        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            if (Arrays.asList(204, 304).contains(response.getStatusCode())) {
                System.out.println(response.getStatusCode() == 204 ? "No Content" : "Not Modified");
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
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
