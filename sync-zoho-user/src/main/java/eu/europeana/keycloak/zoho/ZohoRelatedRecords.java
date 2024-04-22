package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.relatedrecords.ResponseHandler;
import com.zoho.crm.api.relatedrecords.RelatedRecordsOperations;
import com.zoho.crm.api.relatedrecords.RelatedRecordsOperations.GetRelatedRecordsParam;
import com.zoho.crm.api.util.APIResponse;
import java.time.OffsetDateTime;

/**
 * Created by luthien on 22/04/2024.
 */
public class ZohoRelatedRecords {

    public void getRelatedRecords(String moduleAPIName, Long recordId, String relatedListAPIName) throws Exception {
        RelatedRecordsOperations relatedRecordsOperations = new RelatedRecordsOperations(relatedListAPIName, moduleAPIName);
        ParameterMap             paramInstance            = new ParameterMap();
        paramInstance.add(GetRelatedRecordsParam.PAGE, 1);
        paramInstance.add(GetRelatedRecordsParam.PER_PAGE, 2);
        paramInstance.add(GetRelatedRecordsParam.FIELDS, "Id");
        HeaderMap      headerInstance = new HeaderMap();
//        OffsetDateTime startdatetime  = OffsetDateTime.of(2019, 06, 01, 10, 00, 01, 00, ZoneOffset.of("+05:30"));
//        headerInstance.add(GetRelatedRecordsHeader.IF_MODIFIED_SINCE, startdatetime);
        APIResponse<ResponseHandler> response = relatedRecordsOperations.getRelatedRecords(recordId, paramInstance, headerInstance);
//        if (response != null)
    }

}
