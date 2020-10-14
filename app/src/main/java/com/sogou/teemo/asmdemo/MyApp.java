//package com.sogou.teemo.testgc;
//
//import android.app.Application;
//import android.content.Context;
//import android.util.Log;
//
//import com.squareup.leakcanary.LeakCanary;
//import com.squareup.leakcanary.RefWatcher;
//
//public class MyApp extends Application {
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.e("MyApp", "onCreate");
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return;
//        }
//        refWatcher = LeakCanary.install(this);
//    }
//
//    @Override
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(base);
//        Log.e("MyApp", "attachBaseContext");
//    }
//
//    private RefWatcher refWatcher;
//    public static RefWatcher getRefWatch(Context context) {
//        MyApp app = (MyApp) context.getApplicationContext();
//        return app.refWatcher;
//    }
//}
