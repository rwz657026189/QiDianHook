package com.rwz.qidianhook.hook;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.widget.TextView;

import com.rwz.qidianhook.config.Constance;
import com.rwz.qidianhook.utils.FileUtils;
import com.rwz.qidianhook.utils.LogUtil;
import com.rwz.qidianhook.utils.ReflectUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class QDManager {

    private static final String TAG = "QDManager";

    public static QDManager instance;
    private ClassLoader mClassLoader;
    private Context mContext;

    public static QDManager getInstance() {
        if (instance == null)
            synchronized (QDManager.class) {
                if (instance == null)
                    instance = new QDManager();
            }
        return instance;
    }

    public void init(ClassLoader classLoader) {
        boolean isHook = this.mClassLoader == null && classLoader != null;
        this.mClassLoader = classLoader;
        LogUtil.d(TAG, "isHook = " + isHook);
        if (isHook) {
            hookApp();
        }
    }

    private void hookApp() {
        String appContext = "com.qidian.QDReader.QDApplication";
        LogUtil.d(TAG, "hookApp = " + appContext);
        XposedHelpers.findAndHookMethod(appContext, mClassLoader, "attachBaseContext", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.d(TAG, "【afterHookedMethod2】");
                        mContext = (Context) param.args[0];
                        connService();
//                        hookTest22();
//                        hookTest9();
//                        hookTest26();
                        decryptVIPText();
                        hookVIPTextChapterItem();
                    }
                });
    }

    /**
     * hook 下载
     */
    public void hookVIPTextChapterItem() {
        Class cl = XposedHelpers.findClass("com.qidian.QDReader.component.bll.c", mClassLoader);
        Class cl2 = XposedHelpers.findClass("com.qidian.QDReader.component.bll.callback.b", mClassLoader);
        XposedHelpers.findAndHookConstructor(cl, int.class, long.class, long.class, boolean.class, boolean.class,cl2,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Object chapterItem = ReflectUtil.getDeclaredField(param.thisObject, "d");
                        if (chapterItem == null) {
                            Class cl = XposedHelpers.findClass("com.qidian.QDReader.component.entity.ChapterItem", mClassLoader);
                            chapterItem = cl.newInstance();
                            initChapterItem(chapterItem, param.args[2]);
                            Field d = param.thisObject.getClass().getDeclaredField("d");
                            d.setAccessible(true);
                            d.set(param.thisObject, chapterItem);
                        }
                        //1,1010741811,396449908,true,false,com.qidian.QDReader.component.bll.manager.j$3@2e685864
                        ReflectUtil.printArgs(TAG + "_hookVIPTextChapterItem",param.args);
                    }
                });
    }

    private void initChapterItem(Object object, Object chapterId) {
        setField(object, "ChapterId", chapterId);
        setField(object, "ChapterName", "_章节名_");
        setField(object, "IsVip", 1);
    }

    /**
     * 获取解压vip章节的内容
     */
    public void decryptVIPText() {
        Class cl = XposedHelpers.findClass("com.qidian.QDReader.component.bll.c", mClassLoader);
        //  public static String b(long j, ChapterItem chapterItem) {
        LogUtil.d(TAG, "decryptVIPText", cl);
        XposedHelpers.findAndHookMethod(cl, "b", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                ReflectUtil.printAllFiled(param.thisObject, TAG + "_hookTest33");
                Class cl = XposedHelpers.findClass("com.qidian.QDReader.component.entity.ChapterContentItem", mClassLoader);
                Object chapterContent = ReflectUtil.getDeclaredField(cl, param.getResult(), "chapterContent");
                Object bookId = ReflectUtil.getDeclaredField(param.thisObject, "b");
                Object chapterId = ReflectUtil.getDeclaredField(param.thisObject, "c");
                Object userId = ReflectUtil.getDeclaredField(param.thisObject, "n");
//                String fileName = "/vipText/" + bookId + "/" + chapterId + ".txt";
//                FileUtils.outputString(toStr(chapterContent), fileName);
                sendVIPTextMsg(toStr(bookId), toStr(chapterId), toStr(chapterContent));
                //删除起点的本地文件
                File file = new File("/storage/emulated/0/QDReader/book/" + userId + "/" + bookId + "/" + chapterId + ".qd");
                if (file.exists()) {
                    boolean result = file.delete();
//                    LogUtil.d(TAG, "VIP章节解压 删除 ：" + result);
                }
            }
        });
    }

    private String toStr(Object object) {
        return object == null ? null : object.toString();
    }

    private void sendVIPTextMsg(String bookId, String chapterID, String content) {
        LogUtil.d(TAG, "bookId = " + bookId, "chapterID = " + chapterID, "VIP章节解压：" + (TextUtils.isEmpty(content) ? "失败" : "成功"));
        long bi = 0;
        long ci = 0;
        try {
            bi = Long.parseLong(bookId);
            ci = Long.parseLong(chapterID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(content)) {
            Message msg = Message.obtain(null, Constance.DOWN_VIP_TXT_DECRYPT_RESULT);
            Bundle bundle = new Bundle();
            bundle.putLong(Constance.BOOK_ID, bi);
            bundle.putLong(Constance.CHAPTER_ID,ci);
            bundle.putString(Constance.CONTENT, content);
            msg.setData(bundle);
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 发送起点的appID， 下载收费章节需要
     */
    private void sendQDAppId() {
        Object g = null;
        try {
            Class cl = XposedHelpers.findClass("com.qidian.QDReader.core.config.a", mClassLoader);
            Object obj = XposedHelpers.callStaticMethod(cl, "a");
            g = XposedHelpers.callMethod(obj, "G");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Message msg = Message.obtain(null, Constance.QD_APP_ID);
        Bundle bundle = new Bundle();
        bundle.putString(Constance.MSG, g == null ? "" : g.toString());
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void hookTest32() {
        //format.epub.view.a 搜索： format.epub.view.n
        // L_0x0bd0:
        //        r4 = r6 instanceof format.epub.view.n;
        //        if (r4 == 0) goto L_0x0bdb;

        Class cl = XposedHelpers.findClass("format.epub.view.p", mClassLoader);
        // protected java.io.File a(java.lang.String r9)
        //public d a(int i) {
        XposedHelpers.findAndHookMethod(cl, "a", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                ReflectUtil.printAllFiled(param.getResult(), TAG + "_hookTest32");
                //QDManager_hookTest32,f = OEBPS/Images/device_phone_frontcover.jpg
                //    QDManager_hookTest32,h = imagefile:///storage/emulated/0/QDReader/epub/325587223/1001535109/1001535109.trial:OEBPS/Images/device_phone_frontcover.jpg��0��212242
                Object f = ReflectUtil.getDeclaredField(param.getResult(), "f");
                LogUtil.d(TAG + "_hookTest32_args", param.args[0], "f = " + f);
                //    QDManager_hookTest32,f = OEBPS/Images/device_phone_frontcover.jpg
                if (f != null && (f + "").endsWith(".jpg")) {
                    LogUtil.stackTraces(TAG + "_hookTest32");
                    // com.qidian.QDReader.readerengine.f.c$4.run  (QDEpubChapterManager.java:437)
                    //    com.qidian.QDReader.readerengine.g.d$1.a  (QDEpubContentProvider.java:242)
                    //       com.qidian.QDReader.readerengine.g.d.a  (QDEpubContentProvider.java:277)
                    //          com.qidian.QDReader.readerengine.e.f.a  (QDEpubContentLoader.java:60)
                    //             com.qidian.QDReader.readerengine.e.f.b  (QDEpubContentLoader.java:75)
                    //                com.qidian.QDReader.readerengine.e.f.b  (QDEpubContentLoader.java:165)
                    //                   format.epub.view.a.a  (QEPubFormatter.java:94)
                    //                      format.epub.view.a.a  (QEPubFormatter.java:302)
                    //                         format.epub.view.a.a  (QEPubFormatter.java:674)
                    //                            format.epub.view.p.a  (<Xposed>:-1)
                }
            }
        });
    }

    private void hookTest31() {
        Class cl = XposedHelpers.findClass("format.epub.view.n", mClassLoader);
        Class cl2 = XposedHelpers.findClass("format.epub.common.image.c", mClassLoader);
        // n(String str, c cVar, String str2, boolean z, String str3, boolean z2, boolean z3) {
        XposedHelpers.findAndHookConstructor(cl, String.class, cl2, String.class, boolean.class, String.class, boolean.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                ReflectUtil.printArgs(TAG + "_hookTest31_args", param.args);
                ReflectUtil.printAllFiled(param.thisObject, TAG + "_hookTest31_filed");

            }
        });

    }

    private void hookTest30() {
        Class cl = XposedHelpers.findClass("format.epub.common.image.a", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, "a", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);                                                             // 0 212242
                //imagefile:///storage/emulated/0/QDReader/epub/325587223/1001535109/1001535109.trial:OEBPS/Images/device_phone_frontcover.jpg��0��212242
                LogUtil.d(TAG + "_hookTest30", "result = " + param.getResult());
                LogUtil.stackTraces(TAG + "_hookTest30");
                //       QDManager_hookTest30| com.qidian.QDReader.readerengine.g.d.a  (QDEpubContentProvider.java:277)
                //       QDManager_hookTest30|    com.qidian.QDReader.readerengine.e.f.a  (QDEpubContentLoader.java:60)
                //       QDManager_hookTest30|       com.qidian.QDReader.readerengine.e.f.b  (QDEpubContentLoader.java:75)
                //       QDManager_hookTest30|          com.qidian.QDReader.readerengine.e.f.b  (QDEpubContentLoader.java:165)
                //       QDManager_hookTest30|             format.epub.view.a.a  (QEPubFormatter.java:94)
                //       QDManager_hookTest30|                format.epub.view.a.a  (QEPubFormatter.java:455)
                //       QDManager_hookTest30|                   format.epub.view.f.h  (QRTextWordCursor.java:114)
                //       QDManager_hookTest30|                      format.epub.view.p.g  (ZLTextParagraphCursor.java:317)
                //       QDManager_hookTest30|                         format.epub.view.p.a  (ZLTextParagraphCursor.java:199)
                //       QDManager_hookTest30|                            format.epub.view.p.<init>  (ZLTextParagraphCursor.java:193)
                //       QDManager_hookTest30|                               format.epub.view.p.a  (ZLTextParagraphCursor.java:212)
                //       QDManager_hookTest30|                                  format.epub.view.p$a.a  (ZLTextParagraphCursor.java:79)
                //       QDManager_hookTest30|                                     format.epub.common.image.a.a  (<Xposed>:-1)
            }
        });
    }

    private void hookTest29() {
        Class cl = XposedHelpers.findClass("format.epub.zip.f", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, "c", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object arg = param.args[0];
                LogUtil.d(TAG, "hookTest29, args = " + arg);
                if ((arg + "").endsWith(".jpg")) {
                    LogUtil.stackTraces(TAG + "_hookTest29_jpg");
                } else if ((arg + "").endsWith(".css")) {
                    LogUtil.stackTraces(TAG + "_hookTest29_css");
                }
            }
        });
    }

    private void hookTest28() {
        Class cl = XposedHelpers.findClass("format.epub.common.b.d", mClassLoader);
        XposedHelpers.findAndHookConstructor(cl, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.d(TAG, "hookTest28, args = " + param.args[0]);
                Class cl2 = XposedHelpers.findClass("format.epub.common.b.b", mClassLoader);
                ReflectUtil.printAllFiled(cl2, param.thisObject, TAG + "_hookTest28");
            }
        });
    }

    /**
     * 查看image/ css的引用链
     */
    private void hookTest27() {
        Class cl = XposedHelpers.findClass("format.epub.common.image.a", mClassLoader);
        Class cl2 = XposedHelpers.findClass("format.epub.common.b.b", mClassLoader);
        // public a(String str, b bVar, int i, int i2) {
        XposedHelpers.findAndHookConstructor(cl, String.class, cl2, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                //      QDManager_hookTest27,a = format.epub.common.b.f@aef3c5f9
                //      QDManager_hookTest27,b = 0
                //      QDManager_hookTest27,c = 212242

                //QDManager_hookTest27,a = format.epub.common.b.f@bbf21488
                //    QDManager_hookTest27,b = 0
                //    QDManager_hookTest27,c = 143807

                //QDManager_hookTest27,a = format.epub.common.b.f@c9339530
                //    QDManager_hookTest27,b = 0
                //    QDManager_hookTest27,c = 1447774

                // return "imagefile://" + this.a.c() + "\u0000" + this.b + "\u0000" + this.c;
                //
                LogUtil.d(TAG, "====================================================================");
                LogUtil.d(TAG, param.args);
                ReflectUtil.printArgs(TAG + "_hookTest27", param.args);
                Object a = ReflectUtil.getDeclaredField(param.thisObject, "a");
                LogUtil.d(TAG, "------------------------------------");
                Class<?> cl = XposedHelpers.findClass("format.epub.common.b.b", mClassLoader);
                ReflectUtil.printAllFiled(cl, a, TAG + "_hookTest27");
                LogUtil.d(TAG, "====================================================================");
            }
        });
    }

    /**
     * 获取getVipContent接口参数
     */
    private void hookTest26() {
        Class cl = XposedHelpers.findClass("com.qidian.QDReader.framework.network.qd.QDHttpClient", mClassLoader);
        // public QDHttpResp a(String str, ContentValues contentValues) {
        // public QDHttpResp a(String str, ContentValues contentValues, String str2, boolean z) {
        XposedHelpers.findAndHookMethod(cl, "a", String.class, ContentValues.class, String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                // [https://druid.if.qidian.com/argus/api/v1/bookcontent/getvipcontent,
                // i=866412037187648
                // b=1010327039
                // c=436648460,
                // /storage/emulated/0/QDReader/book/325687182/1010327039/436648460.qd,
                // false]
                ReflectUtil.printArgs(TAG + "_hookTest26_args", param.args);
            }
        });
    }

    private void hookTest25() {
        Class c1 = XposedHelpers.findClass("com.qidian.QDReader.readerengine.f.c", mClassLoader);
        Class c2 = XposedHelpers.findClass("com.qidian.QDReader.component.entity.BookItem", mClassLoader);
        Class c3 = XposedHelpers.findClass("format.epub.b", mClassLoader);
        //  public int a(com.qidian.QDReader.component.entity.BookItem r7, format.epub.b r8) {
        XposedHelpers.findAndHookMethod(c1, "a", c2, c3, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.d(TAG, "args = " + param.args[1]);
                ReflectUtil.printAllFiled(param.args[0], TAG);
                LogUtil.d(TAG, "===================================================================================");
                ReflectUtil.printAllFiled(param.thisObject, TAG);
                //1001535109
            }
        });

    }

    private void hookTest24() {
        Class cl = XposedHelpers.findClass("format.epub.zip.f", mClassLoader);
        Class c2 = XposedHelpers.findClass("format.epub.zip.f$a", mClassLoader);
        LogUtil.d(TAG, "hookTest24, c2 = " + c2);
        XposedHelpers.findAndHookConstructor(cl, c2, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.d(TAG, "args = " + param.args[0]);
                LogUtil.stackTraces(TAG);
                //       QDManager| com.qidian.QDReader.readerengine.g.d$a.run  (QDEpubContentProvider.java:176)
                //       QDManager|    com.qidian.QDReader.readerengine.f.c.a  (QDEpubChapterManager.java:131)
                //       QDManager|       format.epub.common.a.a.a  (BookModel.java:58)
                //       QDManager|          format.epub.common.c.b.e.a  (OEBPlugin.java:79)
                //       QDManager|             format.epub.common.c.b.e.b  (OEBPlugin.java:54)
                //       QDManager|                format.epub.common.core.a.g.a  (ZLXMLReaderAdapter.java:37)
                //       QDManager|                   format.epub.common.core.a.e.a  (ZLXMLProcessor.java:60)
                //       QDManager|                      format.epub.common.core.a.e.a  (ZLXMLProcessor.java:66)
                //       QDManager|                         format.epub.common.b.f.i  (ZLZipEntryFile.java:90)
                //       QDManager|                            format.epub.common.b.f.d  (ZLZipEntryFile.java:57)
                //       QDManager|                               format.epub.zip.f.<init>  (<Xposed>:-1)
            }
        });
    }

    private void hookTest23() {
        //eg. args : OEBPS/Images/device_phone_frontcover.jpg
        //eg. args : OEBPS/paid_Text/chapter39.xhtml#
        //eg. args : OEBPS/paid_Text/chapter34.xhtml#
        //
        Class cl = XposedHelpers.findClass("format.epub.zip.f", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, "b", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.d(TAG, "args = " + param.args[0]);
//                LogUtil.stackTraces(TAG);
            }
        });
    }

    /**
     * 获取到xhtml文本内容， 并存储到成员变量f中
     */
    private void hookTest22() {
        String cl = "format.epub.common.core.a.d";
        //b = format.epub.common.core.xhtml.c@19aa4442
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "b",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Object obj = param.thisObject;
                        Field b = obj.getClass().getDeclaredField("b");
                        Field f = obj.getClass().getDeclaredField("f");
                        f.setAccessible(true);
                        b.setAccessible(true);
                        Object fValue = f.get(obj);
                        Object bValue = b.get(obj);
//                        ReflectUtil.printAllFiled(bValue, TAG);
                        if (fValue instanceof char[]) {
                            FileUtils.outputString((char[]) fValue, getFileName(bValue));
                        }
                    }
                });
    }

    private String getFileName(Object obj) {
        if (obj == null)
            return "notFind_" + UUID.randomUUID();
        String objStr = obj.getClass().getName();
        LogUtil.d(TAG, "getFileName", "obj = 【" + objStr + "】");
        if ("format.epub.common.c.b.a".equals(objStr)) { //ContainerFileReader
            Class parentClass = XposedHelpers.findClass("format.epub.common.core.a.g", mClassLoader);
            Object parent = ReflectUtil.getDeclaredField(parentClass, obj, "a");
            LogUtil.d(TAG, "getFileName", "map = " + parent);

            Object a = ReflectUtil.getDeclaredField(obj, "a");
            return a == null ? ("notFind_" + UUID.randomUUID()) : a.toString();
        } else if ("format.epub.common.c.b.c".equals(objStr)) {
            Object a = ReflectUtil.getDeclaredField(obj, "i");
            LogUtil.d(TAG, "getFileName_return", "i = " + a);
            return a == null ? ("notFind_" + UUID.randomUUID()) : a.toString();
        } else if ("format.epub.common.c.b.b".equals(objStr)) {
            ReflectUtil.printAllFiled(obj, TAG + "_getFileName_" + objStr.getClass().getSimpleName());
            return "notFind_" + UUID.randomUUID();
        } else if (!"format.epub.common.core.xhtml.c".equals(objStr)) {
            //ContainerFileReader: format.epub.common.c.b.a@28241c9f
            //OEBMetaInfoReader : format.epub.common.c.b.d@1a12d1ec
            ReflectUtil.printAllFiled(obj, TAG + "_getFileName_" + obj.getClass().getSimpleName());
            return "notFind_" + UUID.randomUUID();
        }
        Object d = ReflectUtil.getDeclaredField(obj, "d");
        if (d == null)
            return "notFind_" + UUID.randomUUID();
        Object x = ReflectUtil.getDeclaredField(obj, "x");
        String dStr = d.toString();
        if (x == null || !dStr.endsWith("#")) {
            LogUtil.d(TAG, "getFileName", "x = " + x, "d = " + d);
            return dStr;
        }
        Object c = ReflectUtil.getDeclaredField(obj, "c");
        String cStr = c == null ? "" : c.toString();
        int dex = Integer.parseInt(dStr.substring(0, dStr.length() - 1));
        Map<String, Integer> map = (Map<String, Integer>) x;
        Set<String> keySet = map.keySet();
        for (String s : keySet) {
            if (map.get(s) == dex) {
                int index = s.lastIndexOf("/");
                return cStr + (index >= 0 ? s.substring(index + 1) : s);
            }
        }
        return cStr + dStr;
    }

    private void outputString(char[] args, String fileName) {
        if (args == null)
            return;
        String pathName = "/storage/emulated/0/rwz/" + fileName;
//        String pathName = "/mnt/shared/Other/" + fileName;
        File file = new File(pathName);
        if (!file.getParentFile().exists()) {
            boolean mkdirs = file.getParentFile().mkdirs();
            LogUtil.d(TAG, "mkdirs = " + mkdirs);
        } else if (file.exists()) {
            int index = pathName.lastIndexOf("/");
            pathName = pathName.substring(0, index + 1) + "repeat_" + pathName.substring(index + 1);
        }
        LogUtil.d(TAG, "outputString = " + pathName, args.length);
        Writer writer = null;
        try {
            writer = new FileWriter(pathName);
            writer.write(args);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void hookTest21() {
        String cl = "format.epub.common.b.a";
        //public void a(Map<String, String> map)
        Class c = XposedHelpers.findClass("format.epub.common.b.b", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", c, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.stackTraces(TAG);
                        LogUtil.d(TAG, "args = " + param.args[1]);
                        ReflectUtil.printAllFiled(param.args[0], TAG);
                    }
                });
    }


    private void hookTest20() {
        String cl = "format.epub.common.core.a.g";
        //public void a(Map<String, String> map)
        Class c = XposedHelpers.findClass("format.epub.common.b.b", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", c,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.d(TAG, "afterHookedMethod", param.args[0], "result = " + param.getResult());
                        LogUtil.stackTraces(TAG);
                    }
                });
    }

    private void hookTest19() {
        String cl = "format.epub.common.core.a.e";
        // public static boolean a(format.epub.common.core.a.f r3, java.io.InputStream r4, int r5) {
        Class c = XposedHelpers.findClass("format.epub.common.core.a.f", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", c, InputStream.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.stackTraces(TAG);
                        LogUtil.d(TAG, param.args[0], param.args[1], param.args[2], "result = " + param.getResult());
                        // QDManager| com.qidian.QDReader.readerengine.g.d$a.run  (QDEpubContentProvider.java:176)
                        //       QDManager|    com.qidian.QDReader.readerengine.f.c.a  (QDEpubChapterManager.java:131)
                        //       QDManager|       format.epub.common.a.a.a  (BookModel.java:58)
                        //       QDManager|          format.epub.common.c.b.e.a  (OEBPlugin.java:80)
                        //       QDManager|             format.epub.common.c.b.c.b  (OEBBookReader.java:110)
                        //       QDManager|                format.epub.common.core.xhtml.c.a  (XHTMLReader.java:251)
                        //       QDManager|                   format.epub.common.core.a.g.a  (ZLXMLReaderAdapter.java:37)
                        //       QDManager|                      format.epub.common.core.a.e.a  (ZLXMLProcessor.java:60)
                        //       QDManager|                         format.epub.common.core.a.e.a  (ZLXMLProcessor.java:74)
                        //       QDManager|                            format.epub.common.core.a.e.a  (<Xposed>:-1)
                        //       QDManager|                               de.robv.android.xposed.XposedBridge.handleHookedMethod  (XposedBridge.java:374)
                        //       QDManager|                                  com.rwz.qidianhook.hook.QDManager$3.afterHookedMethod  (QDManager.java:122)
                        // 第二个参数：如OEBPS/Text/../Images/bei.jpg相关的流： format.epub.common.b.f.i()
                        // QDManager,format.epub.common.core.xhtml.c@3c20c5c4,format.epub.zip.g@3472453a,65536,result = true
                    }
                });
    }

    private void hookTest18() {
        String cl = "format.epub.zip.c";
        Class c = XposedHelpers.findClass("format.epub.zip.d", mClassLoader);
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", c, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
//                print("_beforeHookedMethod", param.thisObject);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG, 50, 3);
                print("", param.thisObject);
            }
        });
    }

    private void hookTest17() {
        String cl = "format.epub.zip.c";
        //G,N-WP;6HB?,C/D5
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                print("_beforeHookedMethod", param.thisObject);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                print("_afterHookedMethod", param.thisObject);
            }
        });
    }

    private void print(String flag, Object obj) throws Exception {
        Class<?> cl = obj.getClass();
        ReflectUtil.printAllFiled(obj, TAG + flag);
        Field nF = obj.getClass().getDeclaredField("n");
        nF.setAccessible(true);
        Object n = nF.get(obj);
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder3.append("(");
        if (n instanceof byte[]) {
            for (byte b : ((byte[]) n)) {
                stringBuilder.append((char) b);
                stringBuilder3.append(b + ",");
            }
        }
        stringBuilder.append(")");
        stringBuilder3.append(")");
        LogUtil.d(TAG + flag + "_n", stringBuilder.toString());
        LogUtil.d(TAG + flag + "_n", stringBuilder3.toString());

        Field oF = obj.getClass().getDeclaredField("o");
        Object o = oF.get(obj);
        StringBuilder stringBuilder2 = new StringBuilder();
        if (o instanceof int[]) {
            for (int b : ((int[]) o)) {
                stringBuilder2.append(b);
            }
        }
        LogUtil.d(TAG + flag + "_o", stringBuilder2.toString());
    }

    private void hookTest16() {
        // format.epub.common.core.a
        String testClass = "format.epub.common.core.a.d";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "b", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
                Object thisObject = param.thisObject;
                Field[] fields = thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object o = field.get(thisObject);
                    if (o != null) {
                        Class<?> cl = o.getClass();
                        Field[] declaredFields = cl.getDeclaredFields();
                        for (Field declaredField : declaredFields) {
                            declaredField.setAccessible(true);
                            Object obj = declaredField.get(o);
                            if (obj instanceof char[]) {
                                LogUtil.d(TAG + "_" + cl, declaredField.getName() + " = " + new String((char[]) obj));
                            } else {
                                LogUtil.d(TAG + "_" + cl, declaredField.getName() + " = " + obj);
                            }
                        }
                    } else {
                        LogUtil.d(TAG, field.getName() + " = " + o);
                    }
                }
            }
        });
    }

    /**
     * 引用ks文件的地方
     */
    private void hookTest15() {
        String testClass = "com.qidian.QDReader.framework.epubengine.d.c";
        Class c = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "c = " + c);
        XposedHelpers.findAndHookMethod(c, "a", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG, 50, 3);
                LogUtil.d("result = " + param.getResult());
                //       QDManager| com.qidian.QDReader.readerengine.g.d$a.run  (QDEpubContentProvider.java:176)
                //       QDManager|    com.qidian.QDReader.readerengine.f.c.a  (QDEpubChapterManager.java:129)
                //       QDManager|       format.epub.common.book.BookEPub.c  (BookEPub.java:68)
                //       QDManager|          format.epub.common.c.b.e.a  (OEBPlugin.java:71)
                //       QDManager|             format.epub.common.c.b.e.b  (OEBPlugin.java:54)
                //       QDManager|                format.epub.common.core.a.g.a  (ZLXMLReaderAdapter.java:37)
                //       QDManager|                   format.epub.common.core.a.e.a  (ZLXMLProcessor.java:60)
                //       QDManager|                      format.epub.common.core.a.e.a  (ZLXMLProcessor.java:66)
                //       QDManager|                         format.epub.common.b.f.i  (ZLZipEntryFile.java:90)
                //       QDManager|                            format.epub.zip.f.b  (ZipFile.java:145)
                //       QDManager|                               format.epub.zip.f.c  (ZipFile.java:162)
                //       QDManager|                                  format.epub.zip.f.a  (ZipFile.java:70)
                //       QDManager|                                     format.epub.zip.c.a  (LocalFileHeader.java:95)
                //       QDManager|                                        format.epub.zip.c.a  (LocalFileHeader.java:108)
                //       QDManager|                                           com.qidian.QDReader.framework.epubengine.d.c.a  (<Xposed>:-1)
                //       QDManager|                                              de.robv.android.xposed.XposedBridge.handleHookedMethod  (XposedBridge.java:374)
            }
        });
    }

    /**
     * 存入密码
     */
    private void hookTest14() {
        String testClass = "com.qidian.QDReader.framework.epubengine.d.c";
        Class c = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "c = " + c);
        XposedHelpers.findAndHookMethod(c, "a", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
                LogUtil.d("result = " + param.args[0]);
            }
        });
    }

    private void hookTest13() {
        String testClass = "com.qidian.QDReader.readerengine.h.a.a";
        Class c = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "c = " + c);
        XposedHelpers.findAndHookMethod(c, "a", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
                LogUtil.d("result = " + param.getResult());
                ReflectUtil.printAllFiled(param.thisObject, TAG);
            }
        });
    }

    /**
     * 查询.ks文件调用的堆栈信息
     */
    private void hookTest12() {
        String testClass = "com.qidian.QDReader.readerengine.h.a.a";
        Class c = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "c = " + c);
        XposedHelpers.findAndHookMethod(c, "a", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
                LogUtil.d(param.args[0], "result = " + param.getResult());
                //       QDManager| com.qidian.QDReader.readerengine.g.d$a.run  (QDEpubContentProvider.java:176)
                //       QDManager|    com.qidian.QDReader.readerengine.f.c.a  (QDEpubChapterManager.java:121)
                //       QDManager|       com.qidian.QDReader.readerengine.h.a.a.a  (Drm.java:53)
                //       QDManager|          com.qidian.QDReader.readerengine.h.a.a.a  (<Xposed>:-1)
            }
        });
    }

    private void hookTest11() {
        String testClass = "com.qidian.QDReader.component.api.d";
        Class c = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "c = " + c);
        //public static QDHttpResp a(long j, String str, int i) {
        // 1001535085,/storage/emulated/0/QDReader/epub/325587223/1001535085/1001535085.ks,1
        XposedHelpers.findAndHookMethod(c, "a", long.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                //QDManager|                com.qidian.QDReader.component.bll.d$2$1.run  (QDEpubBookContentLoader.java:485)
                //QDManager|                   com.qidian.QDReader.component.api.d.a  (<Xposed>:-1)
                LogUtil.stackTraces(TAG);
                LogUtil.d(param.args[0], param.args[1], param.args[2]);
            }
        });
    }

    private void hookTest10() {
        String testClass = "com.qidian.QDReader.readerengine.g.d$a";
        Class c = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "c = " + c);
        XposedHelpers.findAndHookConstructor(c, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                LogUtil.stackTraces(TAG);
                LogUtil.d(TAG, param.args[0]);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
                LogUtil.d(TAG, param.args[0]);
            }
        });
        XposedHelpers.findAndHookMethod(c, "run", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
            }
        });
    }

    /**
     * 获取ks的接口
     */
    private void hookTest9() {
        String testClass = "com.qidian.QDReader.component.api.d";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "a", long.class, String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        //第三个参数1：onlytrial是否只下载预览章节
                        //args = 1001535085,/storage/emulated/0/QDReader/epub/325587223/1001535085/1001535085.ks,1
                        LogUtil.stackTraces(TAG + "_hookTest9", 50, 3);
                        LogUtil.d(TAG + "_hookTest9", "args = " + param.args[0], param.args[1], param.args[2]);
                        ReflectUtil.printAllFiled(param.getResult(), TAG + "_hookTest9");
                        Object result = ReflectUtil.getDeclaredField(param.getResult(), "a");
                        if (result instanceof Integer && (Integer) result == 200) {
                            LogUtil.d(TAG, "下载ks成功：" + param.args[1]);
                        }
                        //       QDManager| java.lang.Thread.run  (Thread.java:818)
                        //       QDManager|    java.util.concurrent.ThreadPoolExecutor$Worker.run  (ThreadPoolExecutor.java:587)
                        //       QDManager|       java.util.concurrent.ThreadPoolExecutor.runWorker  (ThreadPoolExecutor.java:1112)
                        //       QDManager|          java.util.concurrent.FutureTask.run  (FutureTask.java:237)
                        //       QDManager|             java.util.concurrent.Executors$RunnableAdapter.call  (Executors.java:422)
                        //       QDManager|                com.qidian.QDReader.component.bll.d$2$1.run  (QDEpubBookContentLoader.java:485)
                        //       QDManager|                   com.qidian.QDReader.component.api.d.a  (BookApi.java:364)
                        //       QDManager|                      com.qidian.QDReader.component.api.Urls.cM  (<Xposed>:-1)
                        //       QDManager|                         de.robv.android.xposed.XposedBridge.handleHookedMethod  (XposedBridge.java:374)
                    }
                });
    }

    private void hookTest8() {
        String testClass = "format.epub.common.c.b.c";
        Class cl = XposedHelpers.findClass("format.epub.common.b.b", mClassLoader);
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "b", cl,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.stackTraces(TAG, 50, 3);
                        ReflectUtil.printAllFiled(param.args[0], TAG);
                        ReflectUtil.printAllFiled(param.thisObject, TAG);
                    }
                });
    }

    private void hookTest7() {
        String testClass = "format.epub.common.text.model.a";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "b",
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        print(param);
                        super.afterHookedMethod(param);
                        //       QDManager| com.qidian.QDReader.readerengine.g.d$a.run  (QDEpubContentProvider.java:176)
                        //       QDManager|    com.qidian.QDReader.readerengine.f.c.a  (QDEpubChapterManager.java:131)
                        //       QDManager|       format.epub.common.a.a.a  (BookModel.java:58)
                        //       QDManager|          format.epub.common.c.b.e.a  (OEBPlugin.java:80)
                        //       QDManager|             format.epub.common.c.b.c.b  (OEBBookReader.java:110)
                        //       QDManager|                format.epub.common.core.xhtml.c.a  (XHTMLReader.java:251)
                        //       QDManager|                   format.epub.common.core.a.g.a  (ZLXMLReaderAdapter.java:37)
                        //       QDManager|                      format.epub.common.core.a.e.a  (ZLXMLProcessor.java:60)
                        //       QDManager|                         format.epub.common.core.a.e.a  (ZLXMLProcessor.java:74)
                        //       QDManager|                            format.epub.common.core.a.e.a  (ZLXMLProcessor.java:46)
                        //       QDManager|                               format.epub.common.core.a.d.b  (ZLXMLParser.java:457)
                        //       QDManager|                                  format.epub.common.core.a.d.a  (ZLXMLParser.java:792)
                        //       QDManager|                                     format.epub.common.core.xhtml.c.a  (XHTMLReader.java:339)
                        //       QDManager|                                        format.epub.common.core.xhtml.c.a  (XHTMLReader.java:601)
                        //       QDManager|                                           format.epub.common.core.xhtml.c.a  (XHTMLReader.java:517)
                        //       QDManager|                                              format.epub.common.a.b.a  (BookReader.java:114)
                        //       QDManager|                                                 format.epub.common.text.model.p.a  (ZLTextWritablePlainModel.java:204)
                        //       QDManager|                                                    format.epub.common.text.model.p.b  (ZLTextWritablePlainModel.java:96)
                        //       QDManager|                                                       format.epub.common.text.model.a.b  (<Xposed>:-1)
                        //       QDManager|                                                          de.robv.android.xposed.XposedBridge.handleHookedMethod  (XposedBridge.java:374)
                        LogUtil.stackTraces(TAG, 50, 3);
                        Object thisObject = param.thisObject;
                        ReflectUtil.printAllFiled(thisObject, TAG);
//                        private final ArrayList<WeakReference<char[]>> d = new ArrayList();
                        Field a = thisObject.getClass().getDeclaredField("d");
                        a.setAccessible(true);
                        ArrayList list = (ArrayList) a.get(thisObject);
                        if (list.size() > 0) {
                            WeakReference one = (WeakReference) (list.get(0));
                            LogUtil.d(TAG + ", after", "one = " + new String((char[]) one.get()));
                        }
                    }
                });
    }

    private void print(XC_MethodHook.MethodHookParam param) throws Exception {
        Object thisObject = param.thisObject;
        Field a = thisObject.getClass().getDeclaredField("d");
        a.setAccessible(true);
        ArrayList list = (ArrayList) a.get(thisObject);
        if (list.size() > 0) {
            WeakReference one = (WeakReference) (list.get(0));
            LogUtil.d(TAG + ", before", "one = " + new String((char[]) one.get()));
        }
    }

    /**
     * 打印从本地缓存获取到的字符串
     */
    private void hookTest6() {
        String testClass = "format.epub.common.text.model.a";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "a", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.stackTraces(TAG);
                        LogUtil.d(TAG, param.args[0]);
                        LogUtil.d(TAG, param.getResult());
                        LogUtil.d(TAG, new String((char[]) (param.getResult())));
                    }
                });
    }

    private void hookTest() {
        String testClass = "com.qidian.QDReader.framework.epubengine.b.a.a";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "a", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                ///storage/emulated/0/QDReader/epub/325587223/1001535134/1001535134.trial
                Object path = param.args[0];
                LogUtil.d(TAG, "params1 = " + path);
                LogUtil.stackTraces(TAG);
                Object result = param.getResult();
                ReflectUtil.printAllFiled(result, TAG);
                //      a = 101
                //      b = 0
                //      c = 183526
                //      d = 0

            }
        });
    }

    private void hookTest5() {
        String testClass = "com.qidian.QDReader.framework.epubengine.b.a.a";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "b", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object obj = param.args[0];
                LogUtil.stackTraces(TAG);
                Object result = param.getResult();
                LogUtil.d(TAG, "args = " + obj, "result = " + result);
            }
        });
    }

    private void hookTest4() {
        String testClass = "com.qidian.QDReader.framework.epubengine.model.IBook";
        XposedHelpers.findAndHookMethod(testClass, mClassLoader, "b", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                LogUtil.stackTraces(TAG);
                Object result = param.getResult();
                LogUtil.d(TAG, "result = " + result);
            }
        });
    }

    private void hookTest3() {
        String cl = "com.qidian.QDReader.readerengine.f.c";
        String c2 = "com.qidian.QDReader.component.entity.BookItem";
        final Class<?> c2c = XposedHelpers.findClass(c2, mClassLoader);
        String c3 = "format.epub.b";
        final Class<?> c3c = XposedHelpers.findClass(c3, mClassLoader);
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", c2c, c3c,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
//                        param.setResult(1);
                        LogUtil.d(TAG, "obj = " + param.args[0], "obj2 = " + param.args[1], "result = " + param.getResult());
                        LogUtil.stackTraces(TAG);
                        ReflectUtil.printAllFiled(param.args[0], TAG);
                    }
                });
    }

    private void hookTest2() {
        String testClass = "com.qidian.QDReader.ui.activity.QDReaderActivity";
        final Class cl = XposedHelpers.findClass(testClass, mClassLoader);
        LogUtil.d(TAG, "checkBookExists hook before", cl);
        XposedHelpers.findAndHookMethod(cl, "init",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        LogUtil.d(TAG, "checkBookExists hook success");
                        //mBookItem
                        Field field = cl.getDeclaredField("mBookItem");
                        field.setAccessible(true);
                        Object o = field.get(param.thisObject);
                        LogUtil.d(TAG, "checkBookExists", "o = " + o);
                        ReflectUtil.printAllFiled(o.getClass().getDeclaredFields(), o, TAG);

                    }
                });
    }

    private void hookSign() {
        Class<?> rbClass = XposedHelpers.findClass("okhttp3.RequestBody", mClassLoader);
        LogUtil.d(TAG, "handleLoadPackage hook before", rbClass);
        String cl = "com.qidian.QDReader.core.network.e";
        XposedHelpers.findAndHookMethod(cl, mClassLoader, "a", String.class,
                String.class, rbClass, int.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Object[] args = param.args;
                        String arg = "";
                        for (int i = 0; i < args.length; i++) {
                            arg += args[i] + "\n";
                        }
                        LogUtil.d(TAG, "afterHookedMethod", arg, "result = " + param.getResult());
                    }
                });
    }

    private void connService() {
        Intent intent = new Intent();
        intent.setClassName("com.rwz.qidianhook", "com.rwz.qidianhook.service.BridgeService");
        mContext.bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }

    Messenger messenger;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, "onServiceConnected");
            messenger = new Messenger(service);
            Message message = Message.obtain(null, Constance.QD_JOIN);
            message.replyTo = reply;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //发送起点的appID
            sendQDAppId();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG, "onServiceDisconnected");
        }
    };

    final Messenger reply = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mExecutor.execute(new CommRunnable(msg.what, msg.getData()));
        }
    });

    final Executor mExecutor =  Executors.newFixedThreadPool(Constance.MAX_THREAD);

    class CommRunnable implements Runnable{
        private int code;
        private Bundle bundle;

        public CommRunnable(int code, Bundle bundle) {
            this.code = code;
            this.bundle = bundle;
        }

        @Override
        public void run() {
            LogUtil.d(TAG, "run", "code = " + code, "bundle = " + bundle);
            switch (code) {
                case Constance.CREATE_SIGN:
                    createSign(bundle);
                    break;
                case Constance.DECRYPT:
                    decryptEpub();
                    break;
                case Constance.DOWN_KS:
                    downKs();
                    break;
                case Constance.DOWN_VIP_TXT_DECRYPT:
                    downVIPAndDecrypt(bundle);
                    break;
                case Constance.TEST:
                    break;
            }
        }
    }

    private void test() {
    }

    /**
     * 下载ks文件
     **/
    private void downKs() {
        //  public static QDHttpResp a(long j, String str, int i) {
        final long id = 1001535085L;
        final String pathName = "/storage/emulated/0/rwz/epub/325587223/1001535085/1001535085.ks";
//        final String pathName = "/mnt/shared/Other/325587223/1001535085/1001535085.ks";
        //args = 1001535085,/storage/emulated/0/QDReader/epub/325587223/1001535085/1001535085.ks,1
        final int onlytrial = 1;
        LogUtil.d(TAG, "start download", "id = " + id, "pathName = " + pathName, "onlytrial = " + onlytrial);
        Observable.just(pathName)
                .subscribeOn(Schedulers.io())
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        File file = new File(s);
                        boolean exists = file.getParentFile().exists();
                        LogUtil.d(TAG, "exists = " + exists);
                        if (!exists) {
                            boolean mkdirs = file.getParentFile().mkdirs();
                            LogUtil.d(TAG, "mkdirs = " + mkdirs);
                            return mkdirs;
                        }
                        return true;
                    }
                })
                .map(new Function<String, Object>() {
                    @Override
                    public Object apply(String pathName) {
                        Class cl = XposedHelpers.findClass("com.qidian.QDReader.component.api.d", mClassLoader);
                        //第三个参数1：onlytrial是否只下载预览章节
                        //args = 1001535085,/storage/emulated/0/QDReader/epub/325587223/1001535085/1001535085.ks,1
                        Object response = XposedHelpers.callStaticMethod(cl, "a", id, pathName, onlytrial);
                        return response;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object response) throws Exception {
                        Object code = ReflectUtil.getDeclaredField(response, "a");
                        if (code instanceof Integer && (Integer) code == 200) {
                            LogUtil.d("下载ks成功：" + pathName);
                        } else {
                            Object msg = ReflectUtil.getDeclaredField(response, "b");
                            outputLog("下载ks失败：", "id = " + id, "msg = " + msg);
                        }
                    }
                });

    }

    private void createSign(Bundle args) {
        LogUtil.d(TAG, "createSign", messenger);
        if (messenger == null)
            return;
        LogUtil.d(TAG, "createSign", args);
        String url = args.getString(Constance.URL);
        String requestMethod = args.getString(Constance.METHOD);
        String requestBody = args.getString(Constance.REQUEST_BODY);
        String key = args.getString(Constance.REQUEST_KEY);
        try {
            long startTime = System.currentTimeMillis();
            //构建一个requestBody出来
            Object body = null;
            if (requestBody != null) {
                Class buildClass = XposedHelpers.findClass("okhttp3.FormBody.Builder", mClassLoader);
                Object build = buildClass.newInstance();
                String[] split = requestBody.split("&");
                for (String s : split) {
                    String[] arr = s.split("=");
                    if (arr.length > 1)
                        build = XposedHelpers.callMethod(build, "add", arr[0], arr[1]);
                }
                body = XposedHelpers.callMethod(build, "build");
            }
            // public static String a(String str, String str2, RequestBody requestBody, int i, boolean z)
            String signClassName = "com.qidian.QDReader.core.network.e";
            Class signClass = XposedHelpers.findClass(signClassName, mClassLoader);
            Object qdSign = XposedHelpers.callStaticMethod(signClass, "a", url, requestMethod, body, 1, false);

            // com.qidian.QDReader.core.config.a
            String qdInfoClassName = "com.qidian.QDReader.core.config.a";
            Class<?> qdInfoClass = XposedHelpers.findClass(qdInfoClassName, mClassLoader);
            Object a = XposedHelpers.callStaticMethod(qdInfoClass, "a");
            Object qdInfo = XposedHelpers.callMethod(a, "h");

            LogUtil.d(TAG, "<createSignResult>", "Signature = " + key,
                    "qdSign = " + qdSign, "\n qdInfo = " + qdInfo + "\n dTime = " + (System.currentTimeMillis() - startTime) + "ms");

            Message message = Message.obtain(null, Constance.CREATE_SIGN_RESULT);
            Bundle bundle = new Bundle();
            bundle.putString(Constance.QD_SIGN, qdSign + "");
            bundle.putString(Constance.QD_INFO, qdInfo + "");
            bundle.putString(Constance.REQUEST_KEY, key);
            message.setData(bundle);
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            outputLog("Code = " + Constance.CREATE_SIGN_RESULT, "Signature = " + key , "exception:" + e.getMessage());
        }
    }

    /**
     * 输出日志
     *
     * @param params 参数
     */
    private void outputLog(Object... params) {
        if(!Constance.showLog)
            return;
        if (messenger == null)
            return;
        StringBuilder sb = new StringBuilder();
        for (Object param : params) {
            sb.append(param).append(",");
        }
        Message msg = Message.obtain(null, Constance.LOG);
        Bundle bundle = new Bundle();
        bundle.putString(Constance.MSG, sb.toString());
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解压epub文件
     */
    private void decryptEpub() {
        try {
            LogUtil.d(TAG, "decryptEpub start...");
            //args1
            Class<?> bookItemClass = XposedHelpers.findClass("com.qidian.QDReader.component.entity.BookItem", mClassLoader);
            Object bookItem = bookItemClass.newInstance();
            initBookItem(bookItem);
            LogUtil.d(TAG, "bookItem = " + bookItem);

            //args2
            Class inf = XposedHelpers.findClass("com.qidian.QDReader.readerengine.g.d", mClassLoader);
            Constructor constructor = inf.getDeclaredConstructor(bookItemClass);
            Object d = constructor.newInstance(bookItem);
            LogUtil.d(TAG, "d = " + d);

            //  int a = com.qidian.QDReader.readerengine.f.c.a(d.this.e).a(d.this.d, d.this);
            Class cl = XposedHelpers.findClass("com.qidian.QDReader.readerengine.f.c", mClassLoader);
            Object c = XposedHelpers.callStaticMethod(cl, "a", 1001535109L);
            LogUtil.d(TAG, "c = " + c);
            Object result = XposedHelpers.callMethod(c, "a", bookItem, d);
            LogUtil.d(TAG, "result = " + result);
            if (!("0".equals(result + ""))) {
                outputLog("解压失败, result = " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            outputLog("解压失败: " + (e == null ? "" : e.getMessage()));
        }
    }

    private void initBookItem(Object bookItem) {
        String fieldValue = "/storage/emulated/0/rwz/epub/santi1/1001535109.trial";
        File file = new File(fieldValue);
        LogUtil.d(TAG, "fieldValue = " + fieldValue, "exists = " + file.exists(), "canRead = " + file.canRead());
        setField(bookItem, "FilePath", fieldValue);
//        setField(bookItem, "FilePath", "/mnt/shared/Other/1001535109.qteb");
    }

    private void setField(Object obj, String fieldName, Object fieldValue) {
        if(obj == null)
            return;
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, fieldValue);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(TAG, fieldName + " setField : " + e.getMessage());
        }
    }

    private void downVIPAndDecrypt(Bundle bundle) {
        try {
            LogUtil.d(TAG, "downVIPAndDecrypt", "bundle = " + bundle);
            long bookID = bundle.getLong(Constance.BOOK_ID);
            List<String> chapterIDs = bundle.getStringArrayList(Constance.CHAPTER_IDS);
            if(chapterIDs == null)
                chapterIDs = new ArrayList<>();
            if (chapterIDs.size() == 0) {
                long chapterID = bundle.getLong(Constance.CHAPTER_ID);
                chapterIDs.add(String.valueOf(chapterID));
            }
            for (String l : chapterIDs) {
                try {
                    long chapterID = Long.parseLong(l);
                    Class cl = XposedHelpers.findClass("com.qidian.QDReader.component.bll.c", mClassLoader);
                    // public c(long j, long j2, boolean z, boolean z2, b bVar)
                    Class cl2 = XposedHelpers.findClass("com.qidian.QDReader.component.bll.callback.b", mClassLoader);
                    Constructor constructor = cl.getConstructor(long.class, long.class, boolean.class, boolean.class, cl2);
                    Object obj = constructor.newInstance(bookID, chapterID, true, false, null);
                    XposedHelpers.callMethod(obj, "a");
                } catch (Exception e) {
                    e.printStackTrace();
                    outputLog("exception:" + e.getMessage(), "章节id : " +  l + " 无效");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            outputLog("exception:" + e.getMessage());
        }
    }

}
