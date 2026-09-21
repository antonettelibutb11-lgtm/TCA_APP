package com.example.tca_app;

public class Contributor {
    private String id;
    private String name;
    private String role;
    private String avatarUrl;
    private long postCount;
    private boolean isTopContributor;

    public Contributor(String id, String name, String role, String avatarUrl, long postCount) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.postCount = postCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
    public long getPostCount() { return postCount; }
    public boolean isTopContributor() { return isTopContributor; }
    
    public void setTopContributor(boolean topContributor) {
        isTopContributor = topContributor;
    }
}
