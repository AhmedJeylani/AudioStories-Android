package com.ahmedjeylani.audiostories.Services;

import com.ahmedjeylani.audiostories.Models.BaseUser;

import java.util.HashMap;

public class UserCache {

    private static Cache userMemoryCache = new Cache();

    private static String USER_INFO_KEY = "userInfo";

    public static BaseUser loadCache() {
        HashMap<String, String> map = userMemoryCache.get(USER_INFO_KEY);

        if (map == null) {
            return null;
        } else {
            return new BaseUser(map);
        }
    }

    public static void cacheUser(BaseUser user) {
        userMemoryCache.put(USER_INFO_KEY, user.ConvertToHashMap());
    }
}

