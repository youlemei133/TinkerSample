package com.example.tinker;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.tinker.tinker.util.TinkerManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void patch(View view) {
        TinkerManager.patch(this);
    }

    public void test(View view) {
        Toast.makeText(this, "合成前的样子", Toast.LENGTH_SHORT).show();
//        view.setBackgroundColor(Color.RED);
//        Toast.makeText(this, "合成后的样子", Toast.LENGTH_SHORT).show();
    }
}