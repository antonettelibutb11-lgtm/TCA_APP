package com.example.tca_app;

public class MembershipRequest {
    private String id;        // Document ID (user uid or notification id)
    private String userUid;   // Student UID
    private String name;      // Student Name
    private String email;     // Student Email
    private long timestamp;

    public MembershipRequest() {}

    public MembershipRequest(String id, String userUid, String name, String email, long timestamp) {
        this.id = id;
        this.userUid = userUid;
        this.name = name;
        this.email = email;
        this.timestamp = timestamp;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getUserUid() { return userUid != null ? userUid : ""; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
