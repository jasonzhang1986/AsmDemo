package com.sogou.teemo.asmdemo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
//
//        FrameLayout layout = findViewById(R.id.layout_test);
//        layout.addView(new TestView(this));
        Log.e("MainActivity", "checkVPN="+checkVPN());
    }

    private boolean checkVPN() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ConnectivityManager cm  = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = cm.getAllNetworks();
            if (networks!=null) {
                for (Network network: networks) {
                    NetworkInfo networkInfo = cm.getNetworkInfo(network);
                    if (networkInfo.getType() == ConnectivityManager.TYPE_VPN && networkInfo.isConnectedOrConnecting()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}