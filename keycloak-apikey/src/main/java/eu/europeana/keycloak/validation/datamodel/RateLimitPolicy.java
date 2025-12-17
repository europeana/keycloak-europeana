package eu.europeana.keycloak.validation.datamodel;

/**
 * RateLimit policy object
 * will define the rate-limit policy specific to the user
 *
 * @author srishti singh
 * @See: https://www.ietf.org/archive/id/draft-ietf-httpapi-ratelimit-headers-10.html
 * @since 1 December 2025
 */
public class RateLimitPolicy {

    /**
     * Implementation- or service-specific parameters SHOULD be prefixed
     * parameters with a vendor identifier, e.g. acme-policy, acme-burst.
     */
    private String vendorIdentifier;

    /**
     * "q" parameter indicates the quota allocated by this policy measured in quota units.
     */
    private Integer q;

    /**
     * "w" parameter value conveys a time window.
     */
    private long w;

    public RateLimitPolicy(String vendorIdentifier, Integer q, long w) {
        this.vendorIdentifier = vendorIdentifier;
        this.q = q;
        this.w = w;
    }

    public String getVendorIdentifier() {
        return vendorIdentifier;
    }

    public void setVendorIdentifier(String vendorIdentifier) {
        this.vendorIdentifier = vendorIdentifier;
    }

    public Integer getQ() {
        return q;
    }

    public void setQ(Integer q) {
        this.q = q;
    }

    public long getW() {
        return w;
    }

    public void setW(long w) {
        this.w = w;
    }

    @Override
    public String toString() {
        return "\"" + vendorIdentifier + "\";" +
                "q=" + q +
                ";w=" + w;
    }
}