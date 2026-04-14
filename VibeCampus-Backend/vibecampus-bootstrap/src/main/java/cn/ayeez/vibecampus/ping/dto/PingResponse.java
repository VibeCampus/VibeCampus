package cn.ayeez.vibecampus.ping.dto;

import java.time.Instant;

public class PingResponse {

    private String app;
    private String status;
    private Instant timestamp;

    public PingResponse() {
    }

    public PingResponse(String app, String status, Instant timestamp) {
        this.app = app;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

