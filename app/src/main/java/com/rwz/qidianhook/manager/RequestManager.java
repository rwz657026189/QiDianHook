package com.rwz.qidianhook.manager;

import android.util.SparseArray;

import com.rwz.qidianhook.inf.IPostEvent;
import com.rwz.qidianhook.utils.Utils;

import java.util.HashMap;

public class RequestManager {

    private static RequestManager instance;
    private HashMap<String, IPostEvent> mRequestData = new HashMap<>();

    public static RequestManager getInstance() {
        if(instance == null)
            synchronized (RequestManager.class) {
                if(instance == null)
                    instance = new RequestManager();
            }
        return instance;
    }

    public String createKey(Object... params) {
        StringBuilder sb = new StringBuilder();
        for (Object param : params) {
            sb.append(param);
        }
        return sb.toString();
    }

    public void register(String key, IPostEvent postEvent) {
        mRequestData.put(key, postEvent);
    }

    public void unregister(String key) {
        mRequestData.remove(key);
    }

    public IPostEvent getPostEvent(String key) {
        return mRequestData.get(key);
    }

}
