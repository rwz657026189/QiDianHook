package com.rwz.qidianhook.config;

public interface Constance {

    boolean isDebug = true;
    boolean showLog = true;
    //最大支持线程数
    int MAX_THREAD = 64;

    //创建签名
    int CREATE_SIGN = 10000;
    //下载并解压vip文本
    int DOWN_VIP_TXT_DECRYPT = 10001;

    //*******************  客户端相应  *******************//
    //创建签名结果
    int CREATE_SIGN_RESULT = 20000;
    //下载并解压vip文本 结果
    int DOWN_VIP_TXT_DECRYPT_RESULT = 20001;

    int CLIENT_JOIN = 1;
    int QD_JOIN = 2;
    int LOG = 5;
    int DECRYPT = 6;
    int DOWN_KS = 7;
    int TEST = 8;
    int QD_APP_ID = 9;

    //正常日志
    int LOG_NORMAL = 100;
    //异常日志
    int LOG_ERROR = 400;

    String URL = "url";
    String METHOD = "method";
    String REQUEST_BODY = "requestBody";
    String RESULT = "result";
    String QD_SIGN = "qdSign";
    String QD_INFO = "qdInfo";
    String MSG = "msg";
    String REQUEST_KEY = "REQUEST_KEY";
    String BOOK_ID = "BOOK_ID";
    String CHAPTER_ID = "CHAPTER_ID";
    String CHAPTER_IDS = "CHAPTER_IDS";
    String CONTENT = "CONTENT";

}
