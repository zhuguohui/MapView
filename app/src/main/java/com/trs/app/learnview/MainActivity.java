package com.trs.app.learnview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.ArrayMap;
import android.view.View;
import android.widget.TextView;

import com.trs.app.learnview.view.MapView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ArrayMap<String, Integer> map = new ArrayMap<>();

    {
        map.put("非洲", R.raw.ic_african);
        map.put("日本", R.raw.ic_japan);
        map.put("美国", R.raw.ic_american);
    }

    MapView mapView;
    TextView tvTitle;
    int mapIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        tvTitle = findViewById(R.id.tv_title);
        changeMap();
    }

    private void changeMap() {
        mapIndex++;
        if (mapIndex >= map.size()) {
            mapIndex = 0;
        }
        String key = map.keyAt(mapIndex);
        mapView.setMapId(map.get(key));
        tvTitle.setText(key);
    }

    public void changeMap(View view) {
        changeMap();
    }
}