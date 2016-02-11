package ru.linachan.nemesis.gerrit;

import org.json.simple.JSONObject;

public class Author {

    private String name;
    private String eMail;
    private String userName;

    public Author(JSONObject authorData) {
        name = (String) authorData.get("name");
        eMail = (String) authorData.get("email");
        userName = (String) authorData.get("username");
    }

    public String getName() {
        return name;
    }

    public String getEMail() {
        return eMail;
    }

    public String getUserName() {
        return userName;
    }
}