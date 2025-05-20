package eu.europeana.keycloak.zoho;

import com.opencsv.bean.CsvBindByPosition;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;


/**
 * Created by luthien on 24/04/2024.
 */
public class Contact {

    @CsvBindByPosition(position = 0)
    private String ID;

    @CsvBindByPosition(position = 1)
    private String firstName;

    @CsvBindByPosition(position = 2)
    private String lastName;

    @CsvBindByPosition(position = 3)
    private String fullName;

    @CsvBindByPosition(position = 4)
    private String accountID;

    @CsvBindByPosition(position = 5)
    private String email;

    @CsvBindByPosition(position = 6)
    private String secondaryEmail;


    @CsvBindByPosition(position = 7)
    private String leadSource;

    @CsvBindByPosition(position = 8)
    private String userAccountId;

    @CsvBindByPosition(position = 9)
    private String contactParticipation;

    @CsvBindByPosition(position = 10)
    private String modifiedTime;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSecondaryEmail() { return secondaryEmail; }

    public void setSecondaryEmail(String secondaryEmail) { this.secondaryEmail = secondaryEmail; }
    public OffsetDateTime getModifiedTime(){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return OffsetDateTime.parse(modifiedTime, dateTimeFormatter);
    }

    public void setModifiedTime(String modifiedTime){
        this.modifiedTime = modifiedTime;
    }

    public String getLeadSource() { return leadSource; }

    public void setLeadSource(String leadSource) { this.leadSource = leadSource; }

    public String getUserAccountId() { return userAccountId; }

    public void setUserAccountId(String userAccountId) { this.userAccountId = userAccountId; }

    public String getContactParticipation() {
        return contactParticipation;
    }

    public void setContactParticipation(String contactParticipation) {
        this.contactParticipation = contactParticipation;
    }
}