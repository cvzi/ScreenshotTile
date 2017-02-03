package com.github.ipcjs.screenshottile;

import org.junit.Test;

import static com.github.ipcjs.screenshottile.Utils.p;

public class UtilsTest {
    @Test
    public void testP() throws Exception {
        p("msg");
        p("msg", "arg0");
        p(1, "arg0");
    }
    @Test
    public void testEnv(){
        System.out.println(System.getenv());
    }

    static String join(Object... objs) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : objs) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(obj);
        }
        return sb.toString();
    }
}