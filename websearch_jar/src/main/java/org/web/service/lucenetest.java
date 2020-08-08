package org.web.service;

public class lucenetest {
    private String created_at;
    private String id;
    private String source;
    private String text;
    private String lang;
    private String favoriate_count;
    private String retween_count;
    private String user_mentions;
    private String location;
    private String link;
    private String title;
    private String lat;
    private String lng;
    private String score;

    public lucenetest(String created_at, String id, String source, String text, String lang,
                      String favoriate_count, String retween_count, String user_mentions,
                      String location, String link, String title, String lat, String lng, String score) {
        this.created_at = created_at;
        this.id = id;
        this.source = source;
        this.text = text;
        this.lang = lang;
        this.favoriate_count = favoriate_count;
        this.retween_count = retween_count;
        this.user_mentions = user_mentions;
        this.location = location;
        this.link = link;
        this.title = title;
        this.lat = lat;
        this.lng = lng;
        this.score = score;
    }

    public String getCreated_at() {
        return "Time: " + created_at;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getText() {
        return "text: " + text;
    }

    public String getLang() {
        return lang;
    }

    public String getFavoriate_count() {
        return favoriate_count;
    }

    public String getRetween_count() {
        return retween_count;
    }

    public String getUser_mentions() {
        return user_mentions;
    }

    public String getLocation() {
        return "Location: " + location;
    }

    public String getLink() {
        return "Link: " + link;
    }

    public String getTitle() {
        return "Title: " + title;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getScore() {
        return "Score: " + score;
    }
}
