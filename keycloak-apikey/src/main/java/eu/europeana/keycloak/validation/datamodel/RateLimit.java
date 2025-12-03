package eu.europeana.keycloak.validation.datamodel;

/**
 * RateLimit object
 * will hold the current rate limit values of the session for the specific user
 * @See: https://www.ietf.org/archive/id/draft-ietf-httpapi-ratelimit-headers-10.html
 *
 * @author srishti singh
 * @since 1 December 2025
 */
public class RateLimit {

    /**
     * Implementation- or service-specific parameters SHOULD be prefixed
     * parameters with a vendor identifier, e.g. acme-policy, acme-burst.
     */
    private String vendorIdentifier;

    /**
     * conveys the remaining quota units for the identified policy
     */
    private long r;

    /**
     * conveys the time until additional quota is made available for the identified policy
     */
    private long t;

    public RateLimit(String vendorIdentifier, long r, long t) {
        this.vendorIdentifier = vendorIdentifier;
        this.r = r;
        this.t = t;
    }

    public String getVendorIdentifier() {
        return vendorIdentifier;
    }

    public void setVendorIdentifier(String vendorIdentifier) {
        this.vendorIdentifier = vendorIdentifier;
    }

    public long getR() {
        return r;
    }

    public void setR(long r) {
        this.r = r;
    }

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
    }

    @Override
    public String toString() {
        return "\"" + vendorIdentifier + "\";" +
                "r=" + r +
                ";t=" + t ;
    }
}
