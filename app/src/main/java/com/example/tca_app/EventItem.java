package com.example.tca_app;

public class EventItem {
    private String id;
    private String title;
    private int day;
    private int month; // 1-12
    private int year;
    private String time;
    private String description;

    public EventItem(String id, String title, int day, int month, int year, String time, String description) {
        this.id = id;
        this.title = title;
        this.day = day;
        this.month = month;
        this.year = year;
        this.time = time;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public String getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public String getDateFormatted() {
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String mName = (month >= 1 && month <= 12) ? monthNames[month - 1] : "Aug";
        return mName + " " + day + ", " + year + " • " + time;
    }
}
