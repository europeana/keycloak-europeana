package eu.europeana.keycloak.usermgt;

/**
 * Created by luthien on 22/05/2024.
 */
public class UserUuidDto {

    private String uuid;
    private String email;

    public UserUuidDto(String uuid, String email) {
        this.uuid = uuid;
        this.email = email;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "UserUuidDto{" +
               ", uuid='" + uuid + '\'' +
               ", email='" + email + '\'' +
               '}';
    }

}

