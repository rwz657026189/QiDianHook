package com.rwz.qidianhook.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.rwz.qidianhook.config.Constance;
import com.rwz.qidianhook.entity.request.SignEntity;
import com.rwz.qidianhook.entity.request.VIPTextEntity;
import com.rwz.qidianhook.entity.response.SignResponseEntity;
import com.rwz.qidianhook.entity.response.VIPTextResponseEntity;
import com.rwz.qidianhook.server.websocket.WebSocketManager;
import com.rwz.qidianhook.utils.LogUtil;
import com.rwz.qidianhook.utils.Utils;

public class BridgeService extends Service{

    private static final String TAG = "BridgeService";

    private static final int MAX_LOG_TEMP = 10000;
    private static MessengerHandler mHandle = new MessengerHandler();
    private Messenger mMsg = new Messenger(mHandle);


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMsg.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        WebSocketManager.getInstance().startConn();
    }

    private static class MessengerHandler extends Handler{

        private Messenger mClientMes;
        private Messenger mQDMes;
        private final StringBuilder logData = new StringBuilder();

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogUtil.d(TAG, "handleMessage", "msg = " + msg, Thread.currentThread().getName());
            try {
                switch (msg.what) {
                    case Constance.CLIENT_JOIN: //客户端注册
                        mClientMes = msg.replyTo;
                        outputLog("客户端注册成功");
                        break;
                    case Constance.QD_JOIN: //起点注册
                        mQDMes = msg.replyTo;
                        outputLog("起点hook成功");
                        break;
                    case Constance.CREATE_SIGN_RESULT: //创建签名文件结果
                        onCreateSignSuccess(msg);
                        break;
                    case Constance.LOG: //输出日志
                        outputLog(msg.getData().getString(Constance.MSG));
                        break;
                    case Constance.QD_APP_ID: //起点appID
                        outputLog("appId=[" + msg.getData().getString(Constance.MSG)+ "]");
                        break;
                    case Constance.DOWN_VIP_TXT_DECRYPT_RESULT: //下载vip章节结果
                        onVIPTextResult(msg);
                        break;
                    default:
                        sendMsg(msg);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void onCreateSignSuccess(Message msg){
            Bundle bundle = msg.getData();
            String qdSign = bundle.getString(Constance.QD_SIGN);
            String qdInfo = bundle.getString(Constance.QD_INFO);
            String key = bundle.getString(Constance.REQUEST_KEY);
//                HttpManager.getInstance().createSign(key, qdSign, qdInfo);
            WebSocketManager.getInstance().sendMsg(new SignResponseEntity(Constance.CREATE_SIGN_RESULT, key, qdSign, qdInfo));
        }

        private void onVIPTextResult(Message msg) {
            Bundle bundle = msg.getData();
            long bookId = bundle.getLong(Constance.BOOK_ID);
            long chapterId = bundle.getLong(Constance.CHAPTER_ID);
            String content = bundle.getString(Constance.CONTENT);
            WebSocketManager.getInstance().sendMsg(new VIPTextResponseEntity(bookId, chapterId, content));
        }

        private void outputLog(String msg) throws RemoteException{
            if(!Constance.showLog)
                return;
            logData.append(Utils.getTime()).append(msg).append("\n");
            if (logData.length() > MAX_LOG_TEMP) {
                logData.delete(0, logData.length() - MAX_LOG_TEMP);
            }
            if (mClientMes != null) {
                Message obtain = Message.obtain(null, Constance.LOG);
                Bundle bundle = new Bundle();
                bundle.putString(Constance.MSG, logData.toString());
                obtain.setData(bundle);
                mClientMes.send(obtain);
            } else {
                LogUtil.e(TAG, "mClientMes = null");
            }
        }

        public void sendMsg(Message msg) throws RemoteException {
            if (mQDMes != null) {
                Message obtain = Message.obtain(null, msg.what);
                obtain.copyFrom(msg);
                mQDMes.send(obtain);
            } else {
                LogUtil.e(TAG, msg.what, "mQDMes = null");
                WebSocketManager.getInstance().sendErrorLog("起点未hook成功！");
            }
        }
    }

    /**
     * 创建一个签名
     */
    public static void createSign(SignEntity entity) {
        if(entity != null)
            createSign(entity.getUrl(), entity.getRequestMethod(), entity.getRequestBody(), entity.getSignature());
        else {
            LogUtil.e(TAG, "签名参数解析失败");
        }
    }

    public static void createSign(String url, String requestMethod, String requestBody, String signature) {
        LogUtil.d(TAG, "createSign");
        Message msg = Message.obtain(null, Constance.CREATE_SIGN);
        Bundle bundle = new Bundle();
        bundle.putString(Constance.URL, url);
        bundle.putString(Constance.METHOD, requestMethod);
        bundle.putString(Constance.REQUEST_BODY, requestBody);
        bundle.putString(Constance.REQUEST_KEY, signature);
        try {
            msg.setData(bundle);
            mHandle.sendMsg(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public static void vipText(VIPTextEntity entity){
        if (entity != null) {
            Message msg = Message.obtain(null, entity.getCode());
            Bundle bundle = new Bundle();
            bundle.putLong(Constance.BOOK_ID, entity.getBookID());
            bundle.putStringArrayList(Constance.CHAPTER_IDS, entity.getChapterIDs());
            msg.setData(bundle);
            try {
                msg.setData(bundle);
                mHandle.sendMsg(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


}
