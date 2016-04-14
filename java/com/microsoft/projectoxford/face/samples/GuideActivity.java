package com.microsoft.projectoxford.face.samples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Created by CXC on 2016/4/13.
 */
public class GuideActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        new Thread(){
            @Override
            public void run() {
                try {
                    sleep(2000);
                    mHandler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Intent i = new Intent(GuideActivity.this,FaceChoose.class);
                startActivity(i);
                GuideActivity.this.finish();
            }
        }
    };
}
