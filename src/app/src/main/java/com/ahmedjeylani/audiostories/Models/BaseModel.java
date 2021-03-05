package com.ahmedjeylani.audiostories.Models;

import java.io.Serializable;

public class BaseModel implements Serializable {

    public String imageRef, uniqueID, name, date, username, fileName;

    public BaseModel(){}


    public String getImageRef() {
        return imageRef;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = imageRef;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) {this.username = username; }

    public String getFileName() { return fileName; }

    public void setFileName(String fileName) {this.fileName = fileName; }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
