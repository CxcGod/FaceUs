package com.microsoft.projectoxford.face.samples;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;

import java.util.List;

public class ResultActivity extends AppCompatActivity {

    GridView mGridView;
    MyAdapter mAdapter;

    //要显示的图片
    private List<String> mImgs;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mImgs = getIntent().getBundleExtra("bundle_result").getStringArrayList("result");

        mGridView = (GridView) findViewById(R.id.gv_result);
        mAdapter = new MyAdapter(getApplicationContext(), mImgs, R.layout.grid_item);
        mGridView.setAdapter(mAdapter);
    }

}
