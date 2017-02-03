package com.github.ipcjs.screenshottile;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Utils {

    public static void p(Object... args) {
        if (!BuildConfig.DEBUG) return;
//        System.out.println("Object... args");
        p(TextUtils.join(", ", args));
    }

    public static void p(String msg) {
        if (!BuildConfig.DEBUG) return;
//        System.out.println("String msg");
        Log.i("Screenshot", msg);
    }

    public static void p(String format, Object... args) {
        if (!BuildConfig.DEBUG) return;
//        System.out.println("String format, Object... args");
        p(String.format(format, args));
    }

    public static void runCmd(String cmd, boolean isRoot) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(isRoot ? "su" : "sh");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            bw.write(cmd);
            bw.write("\nexit\n");
            bw.flush();
            p("%s: %d, %s, %s", cmd,
                    process.waitFor(),
                    is2String(process.getInputStream()),
                    is2String(process.getErrorStream())
            );
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String is2String(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String tmp = null;
        while ((tmp = br.readLine()) != null) {
            sb.append(tmp).append('\n');
        }
        return sb.toString();
    }
}
