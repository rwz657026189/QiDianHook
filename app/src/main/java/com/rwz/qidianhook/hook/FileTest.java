package com.rwz.qidianhook.hook;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class FileTest {

    public static void a(String r10) {
        String r0 = null;
        File r1 = new File(r10);
        if (r1 == null) {
            //goto 006f
        }
        boolean r22 = r1.exists();
        if (!r22) {
            //goto 006f
        }
        try {
            RandomAccessFile r2 = new RandomAccessFile(r1, "r");
            long r4 = r1.length();
            int r6 = 60;
            r4 -= r6;
            r2.seek(r4);
            byte[] buff = new byte[16];
            r2.read(buff);
            String r3 = new String(buff, "utf-8");
            String r4_1 = r3.trim();
            int r5_1 = r2.read();
            byte[] r1_1 = new byte[4];
            r2.read(r1_1);
            String r3_1 = new String(r1_1, "utf-8");
            int r6_1 = r2.read();
            r1 = null;
            String r7 = "epu0";
            String r8 = r3_1.toLowerCase();
            boolean r7_1 = r7.equals(r8);
            if (!r7_1) {
                //goto 0x0070
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

//        此账号下未授权该设备
//https://druid.if.qidian.com/argus/api/v1/accuratecomsposing/getcontentkey?b=1001535109&onlytrial=1

        String str = "/storage/emulated/0/QDReader/epub/325587223/1001535134/1001535134.trial:OEBPS/Text/chapter43.xhtml";
        int lastIndexOf = str.lastIndexOf(58);
        System.out.println("lastIndexOf = " + lastIndexOf + "," +(char)(58));

        int lastIndexOf2 = str.lastIndexOf(47);
        System.out.println("lastIndexOf2 = " + lastIndexOf2 + "," +(char)(47));

        System.out.println("class = " + Dog.class);

    }

    private class Dog implements Serializable {
        int age;
        boolean sex;

        public Dog(int age, boolean sex) {
            this.age = age;
            this.sex = sex;
        }
    }


}
