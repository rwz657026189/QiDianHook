package com.rwz.qidianhook.server.andserver;

import com.rwz.qidianhook.MainActivity;
import com.rwz.qidianhook.utils.LogUtil;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.RequestBody;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

@RestController
public class MainApi {

    private static final String TAG = "MainApi";

    int count = 0;
    @GetMapping("/test")
    public void info(HttpRequest request,final HttpResponse response) {
        String account = request.getParameter("account");
        String password = request.getParameter("password");
//        String requestBody = request.getParameter("requestBody");
        String content;
        if ("speng".equals(account) && "123".equals(password)) {
            content = "request OK";
        } else {
            content = "request fail";
        }
        count++;
        LogUtil.d(TAG, "MainApi", "count = " + count);
        StringBody body = new StringBody(content);
        response.setBody(body);
    }

    @PostMapping("/sign")
    public void sign(HttpRequest request, HttpResponse response) {
        RequestBody rb = request.getBody();
        HashMap<String, String> map = new HashMap();
        try {
            String content = rb.string();
            LogUtil.d(TAG, "content = " + content);
            String[] split = content.split("&");
            for (String s : split) {
                String[] kv = s.split("=");
                if(kv.length > 1)
                    map.put(kv[0], kv[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.d(TAG, map);
        String content = "request OK";
        String url = map.get("url");
        String method = map.get("method");
        String requestBody = map.get("requestBody");
        String signature = map.get("Signature");
        try {
            url = URLDecoder.decode(url, "utf-8");
            requestBody = URLDecoder.decode(requestBody, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        LogUtil.d(TAG, "url = " + url, "method = " + method);
        StringBody body = new StringBody(content);
        response.setBody(body);
        MainActivity.createSign(url, method, requestBody, signature);
    }


}
