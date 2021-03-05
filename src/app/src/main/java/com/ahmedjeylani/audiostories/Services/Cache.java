package com.ahmedjeylani.audiostories.Services;

import android.util.LruCache;

import java.util.HashMap;

public class Cache extends LruCache<String, HashMap<String, String>> {

    public Cache() {
        super(getCacheSize());
    }

    private static int getCacheSize() {
        final int maxMemorySize = (int) Runtime.getRuntime().maxMemory() / 1024;
        final int cacheSize = maxMemorySize / 10;

        return cacheSize;
    }
}
