package com.sogou.teemo.asmdemo;

import android.content.Context;

public class TestManager {
    private static class Holder {
        private  static TestManager holder = new TestManager();
    }

    private static Context sContext;
    public static TestManager getInstance(Context context) {
        sContext = context;
        return Holder.holder;
    }
}
