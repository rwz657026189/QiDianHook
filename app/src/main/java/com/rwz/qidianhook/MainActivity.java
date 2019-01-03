package com.rwz.qidianhook;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.rwz.qidianhook.config.Constance;
import com.rwz.qidianhook.entity.request.SignEntity;
import com.rwz.qidianhook.server.websocket.WebSocketManager;
import com.rwz.qidianhook.server.websocket.WebSocketServerManager;
import com.rwz.qidianhook.utils.LogUtil;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private TextView mLogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogView = findViewById(R.id.log);
        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.createSign).setOnClickListener(this);
        findViewById(R.id.downKs).setOnClickListener(this);
        findViewById(R.id.test).setOnClickListener(this);
        findViewById(R.id.startServer).setOnClickListener(this);
        findViewById(R.id.vipContent).setOnClickListener(this);
        connService();
    }

    private void connService() {
        Intent service = new Intent();
        service.setClassName("com.rwz.qidianhook", "com.rwz.qidianhook.service.BridgeService");
        bindService(service, conn, Service.BIND_AUTO_CREATE);
    }

    private static Messenger messenger;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, "onServiceConnected");
            messenger = new Messenger(service);
            Message msg = Message.obtain(null, Constance.CLIENT_JOIN);
            msg.replyTo = replay;
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private Messenger replay = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogUtil.d(TAG, "handleMessage", "msg = " + msg);
            switch (msg.what) {
                case Constance.LOG:
                    onReceivedLog(msg.getData().getString(Constance.MSG));
                    break;
            }
        }
    });

    private void onReceivedLog(String msg) {
        if (mLogView != null) {
            mLogView.setText(msg);
        }
    }

    @Override
    public void onClick(View v) {
        EditText et = findViewById(R.id.ids);
        String ids = et.getText().toString();
        long bookID = 1010741811L;
        long chapterID = 396449301L;
        String[] split = ids.split(",");
        if (split != null && split.length == 2) {
            bookID = getLong(split[0], bookID);
            chapterID = getLong(split[1], chapterID);
        }
        int id = v.getId();
        if (id == R.id.start) {
            WebSocketManager.getInstance().startConn();
        } else if (id == R.id.stop) {
            WebSocketManager.getInstance().closeConn();
        } else if (id == R.id.createSign) {
            sendMsg(Constance.DECRYPT);
        } else if (id == R.id.downKs) {
            sendMsg(Constance.DOWN_KS);
        }else if (id == R.id.test) {
            handleTest();
            //            read("/storage/emulated/0/QDReader/book/325687182/1010327039/437721636.qd");
        } else if (id == R.id.startServer) {
            WebSocketServerManager.getInstance().startServer();
        } else if (id == R.id.serverSendMsg) {
            WebSocketServerManager.getInstance().sendMsg("hello");
        } else if (id == R.id.vipContent) {
            Bundle bundle = new Bundle();
            bundle.putLong(Constance.BOOK_ID, bookID);
            bundle.putLong(Constance.CHAPTER_ID, chapterID);
            /*ArrayList<String> list = new ArrayList<>();
            list.add("435244135");
            list.add("435251723");
            list.add("435359985");
            list.add("435368146");
            list.add("435480125");
            bundle.putStringArrayList(Constance.CHAPTER_IDS, list);*/
            sendMsg(Constance.DOWN_VIP_TXT_DECRYPT, bundle);
        }
    }

    private long getLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    private void handleTest() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                createSign("http://test", "GET", "test", "key");
            }
        }.start();
    }

    private void sendMsg(int what) {
        Message msg = Message.obtain(null, what);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(int what, Bundle bundle) {
        Message msg = Message.obtain(null, what);
        try {
            msg.setData(bundle);
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class CallBack{
        private String a;
        private String b;
        private String c;

        public void onASuccessed(String a) {
            this.a = a;
            isNext();
        }

        public void onBSuccessed(String b) {
            this.b = b;
            isNext();
        }

        public void onCSuccessed(String c) {
            this.c = c;
            isNext();
        }

        private void isNext() {
            if (a != null && b != null && c != null) {
                // TODO: 2018/12/20 0020
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
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }




}
