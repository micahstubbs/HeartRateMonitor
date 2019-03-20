package com.example.heartratemonitor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;

public class MainActivity extends AppCompatActivity {

    Charting cardio_chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //AppBar
        Toolbar myToolbar = findViewById(R.id.appbar_layout);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        LineChart lCh = findViewById(R.id.cardio_chart);
        cardio_chart = new Charting(lCh);

        cardio_chart.setMaxViewPoints(2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.menu_run).setVisible(true);
        menu.findItem(R.id.menu_stop).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_ble_settings:
                startActivity(new Intent(this, BLESettingsActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
