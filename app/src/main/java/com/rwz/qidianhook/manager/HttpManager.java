package com.rwz.qidianhook.manager;

import com.rwz.qidianhook.utils.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class HttpManager {

    private static final String TAG = "HttpManager";

    private static  HttpManager instance;
    private final OkHttpClient mOkClient;

    public static HttpManager getInstance() {
        if(instance == null)
            synchronized (HttpManager.class) {
                if(instance == null)
                    instance = new HttpManager();
            }
        return instance;
    }

    private HttpManager() {
        int timeout = 3;
        mOkClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void createSign(String key, String qdSign, String qdInfo) {
        FormBody.Builder builder = new FormBody.Builder();
        LogUtil.d(TAG, "key = " + key, "qdSign = " + qdSign);
        builder.add("signature", key);
        builder.add("qdSign", qdSign);
        builder.add("qdInfo", qdInfo);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://192.168.0.183:8080/push")
                .post(builder.build())
                .build();
        mOkClient.newCall(request).enqueue(callback);
    }

    Callback callback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String text = response.body().string();
            LogUtil.d(TAG, "onResponse", "text = " + text);
        }
    };


}
