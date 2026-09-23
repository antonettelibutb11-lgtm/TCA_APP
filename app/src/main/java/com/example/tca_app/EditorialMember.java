package com.example.tca_app;

public class EditorialMember {

    private String id;
    private String name;
    private String department;
    private String role;
    private String photoUrl;
    private int order;

    public EditorialMember() {}

    public EditorialMember(String id, String name, String department, String role, String photoUrl, int order) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.role = role;
        this.photoUrl = photoUrl;
        this.order = order;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department != null ? department : ""; }
    public void setDepartment(String department) { this.department = department; }

    public String getRole() { return role != null ? role : ""; }
    public void setRole(String role) { this.role = role; }

    public String getPhotoUrl() { return photoUrl != null ? photoUrl : ""; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
