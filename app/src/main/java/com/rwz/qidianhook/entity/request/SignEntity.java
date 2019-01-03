package com.rwz.qidianhook.entity.request;

public class SignEntity {

    private int Code;
    private String URL;
    private String Method;
    private String RequestBody;
    private String Signature;

    public String getUrl() {
        return URL;
    }

    public String getRequestMethod() {
        return Method;
    }

    public String getRequestBody() {
        return RequestBody;
    }

    public String getSignature() {
        return Signature;
    }


}
