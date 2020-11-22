package com.zxh.hookplugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * 占位Activity
 */
public class StubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stub);
    }
}