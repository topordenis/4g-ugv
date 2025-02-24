package com.topor.rover;

import com.google.gson.Gson;

public class WebSocketMessage {
    private String type;
    private String data;
    private long timestamp;

    // Constructor
    public WebSocketMessage(String type, Object data) {
        this.type = type;
        Gson gson = new Gson();
        this.data = gson.toJson(data);
        this.timestamp = System.currentTimeMillis();  // Automatically set the timestamp
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
