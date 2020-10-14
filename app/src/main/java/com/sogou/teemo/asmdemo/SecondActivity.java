package com.sogou.teemo.asmdemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class SecondActivity  extends AppCompatActivity {
    private TextView textView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        textView = findViewById(R.id.tv_text);
        new BackgroundTask().execute();
//        test1();
//        test2();
    }

    private void test1() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Activity activity = SecondActivity.this;
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d("MainActivity","current activity="+activity.toString());
                }
            }
        }.start();
    }

    private void test2() {
        TestManager manager = TestManager.getInstance(this);
    }

    private class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            textView.setText("abc");
        }
    }
}
