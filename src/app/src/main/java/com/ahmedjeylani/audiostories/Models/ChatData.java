package com.ahmedjeylani.audiostories.Models;

public class ChatData extends BaseModel {
    private String message, messageTime;

    public ChatData() {

    }

    public ChatData(String uniqueID,String username,String message,String messageTime,String imageRef) {

        this.message = message;
        this.username = username;
        this.uniqueID = uniqueID;
        this.imageRef = imageRef;

        this.messageTime = messageTime;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(String messageTime) {
        this.messageTime = messageTime;
    }

}
