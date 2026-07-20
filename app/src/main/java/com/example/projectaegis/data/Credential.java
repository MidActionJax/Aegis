package com.example.projectaegis.data;

public class Credential {

    private long id;
    private String accountName;
    private String url;
    private String username;
    private String password;
    private long updatedAt;

    public Credential(long id, String accountName, String url, String username, String password, long updatedAt) {
        this.id = id;
        this.accountName = accountName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
