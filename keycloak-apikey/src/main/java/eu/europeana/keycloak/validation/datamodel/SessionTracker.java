package eu.europeana.keycloak.validation.datamodel;

import java.io.Serializable;

import java.util.Objects;

public class SessionTracker implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private int sessionCount;
    private String lastAccessDate;
    private String lastRateLimitReachingTime;

    public SessionTracker(String id, int sessionCount, String lastAccessDate) {
        this.id = id;
        this.sessionCount = sessionCount;
        this.lastAccessDate = lastAccessDate;
    }

    /**
     * Constructs new SessionTracker Object from existing.
     * Deep copy the object. As we don't have Mutable fields in class ,
     * shallow copy code below is effectively a deep copy.
     * @param existing SessionTracker object
     */
    public SessionTracker(SessionTracker existing) {
        if(existing!=null) {
            this.id = existing.id;
            this.sessionCount = existing.sessionCount;
            this.lastAccessDate = existing.lastAccessDate;
            this.lastRateLimitReachingTime = existing.lastRateLimitReachingTime;
        }
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public String getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(String lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    public String getLastRateLimitReachingTime() {
        return lastRateLimitReachingTime;
    }

    public void setLastRateLimitReachingTime(String lastRateLimitReachingTime) {
        this.lastRateLimitReachingTime = lastRateLimitReachingTime;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SessionTracker that = (SessionTracker) obj;
        return Objects.equals(id, that.id) &&
            Objects.equals(sessionCount, that.sessionCount) &&
            Objects.equals(lastAccessDate, that.lastAccessDate) &&
            Objects.equals(lastRateLimitReachingTime, that.lastRateLimitReachingTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionCount, lastAccessDate, lastRateLimitReachingTime);
    }



}
