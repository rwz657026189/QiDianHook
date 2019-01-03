package com.rwz.qidianhook.entity.response;

public class SignResponseEntity {

    int Code;
    String Signature;
    String QDSign;
    String QDInfo;

    public SignResponseEntity(int code, String signature, String QDSign, String QDInfo) {
        Code = code;
        Signature = signature;
        this.QDSign = QDSign;
        this.QDInfo = QDInfo;
    }

}
