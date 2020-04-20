package com.GXChecker.Entity;

import java.util.Date;

public class UggQuery {
    private int QueryId ;
    private String Id ;
    private Date DTime ;
    private String IpAddress ;
    private String UserName ;


    public int getQueryId() {
        return QueryId;
    }

    public void setQueryId(int queryId) {
        QueryId = queryId;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public Date getDTime() {
        return DTime;
    }

    public void setDTime(Date DTime) {
        this.DTime = DTime;
    }

    public String getIpAddress() {
        return IpAddress;
    }

    public void setIpAddress(String ipAddress) {
        IpAddress = ipAddress;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }
}
