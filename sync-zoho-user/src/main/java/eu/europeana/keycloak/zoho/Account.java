package eu.europeana.keycloak.zoho;


import com.opencsv.bean.CsvBindByPosition;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Created by luthien on 24/04/2024.
 */
public class Account {

    @CsvBindByPosition(position = 0)
    private String id;

    @CsvBindByPosition(position = 1)
    private String accountName;

    @CsvBindByPosition(position = 2)
    private String europeanaOrgID;

    @CsvBindByPosition(position = 3)
    private String modifiedTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getEuropeanaOrgID() {
        return europeanaOrgID;
    }

    public void setEuropeanaOrgID(String europeanaOrgID) {
        this.europeanaOrgID = europeanaOrgID;
    }

    public OffsetDateTime getModifiedTime(){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return OffsetDateTime.parse(modifiedTime, dateTimeFormatter);
    }

    public void setModifiedTime(String modifiedTime){
        this.modifiedTime = modifiedTime;
    }
}