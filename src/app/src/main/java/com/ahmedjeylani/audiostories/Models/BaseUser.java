package com.ahmedjeylani.audiostories.Models;

import java.io.Serializable;
import java.util.HashMap;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.BIO_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.IMAGE_REF_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NAME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.UNIQUEID_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERNAME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USER_TYPE_KEY_NAME;

public class BaseUser extends BaseModel implements Serializable {
    private String userType, bio;

    public BaseUser(){}

    public BaseUser(HashMap<String, String> hashMap) {
         this.uniqueID =  hashMap.get(UNIQUEID_KEY_NAME);
         this.username = hashMap.get(USERNAME_KEY_NAME);
         this.name = hashMap.get(NAME_KEY_NAME);
         this.imageRef = hashMap.get(IMAGE_REF_KEY_NAME);
         this.userType = hashMap.get(USER_TYPE_KEY_NAME);
         this.bio = hashMap.get(BIO_KEY_NAME);
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public HashMap<String,String> ConvertToHashMap() {
        HashMap<String,String> map = new HashMap<>();
        map.put(UNIQUEID_KEY_NAME, this.uniqueID);
        map.put(USERNAME_KEY_NAME, this.username);
        map.put(NAME_KEY_NAME, this.name);
        map.put(IMAGE_REF_KEY_NAME, this.imageRef);
        map.put(USER_TYPE_KEY_NAME, "standard");
        map.put(BIO_KEY_NAME,"");

        return map;
    }
}
