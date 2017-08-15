package com.github.ipcjs.screenshottile;

import org.junit.Test;

import static com.github.ipcjs.screenshottile.TestUtil.p;

public class UtilsTest {
    @Test
    public void testP() throws Exception {
        p("msg");
        p("msg", "arg0");
        p(1, "arg0");
    }

    @Test
    public void testEnv() {
        p(System.getenv());
    }
}