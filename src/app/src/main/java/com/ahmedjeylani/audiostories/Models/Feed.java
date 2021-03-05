package com.ahmedjeylani.audiostories.Models;

import java.io.Serializable;

public class Feed extends BaseModel implements Serializable{

    private String info, audioRef, noRecordings, noLikes, creatorID;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getAudioRef() {
        return audioRef;
    }

    public void setAudioRef(String audioRef) {
        this.audioRef = audioRef;
    }

    public String getNoRecordings() {
        return noRecordings;
    }

    public void setNoRecordings(String noRecordings) {
        this.noRecordings = noRecordings;
    }

    public String getNoLikes() {
        return noLikes;
    }

    public void setNoLikes(String noLikes) {
        this.noLikes = noLikes;
    }

    public String getCreatorID() { return creatorID;}

    public void setCreatorID(String creatorID) { this.creatorID = creatorID; }
}
