package com.example.heartratemonitor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;

public class MainActivity extends AppCompatActivity {

    Charting ecg_chart, hr_chart;
    Thread thr; //del
    boolean run = false;//del

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //AppBar
        Toolbar myToolbar = findViewById(R.id.appbar_layout);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        LineChart lCh = findViewById(R.id.cardio_chart);
        LineChart lCh1 = findViewById(R.id.bpm_chart);
        ecg_chart = new Charting(lCh, Charting.ChartType.ECG);
        ecg_chart.setMaxViewPoints(2000);

        hr_chart = new Charting(lCh1, Charting.ChartType.HR);//Heart Rate chart
        hr_chart.setMaxViewPoints(150);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (!run) {
            menu.findItem(R.id.menu_run).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
        } else{
            menu.findItem(R.id.menu_run).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
        }


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
            case R.id.menu_run:
                run = true;
                showChartData();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_stop:
                run = false;
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Поток для непрерывного отображения данных
    private void showChartData() {
        thr = new Thread(){
            public void run() {
                while (run) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addChartData();
                            }
                        });
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thr.start();
    }

    byte[] d = new byte[20];
    private void addChartData() {


            hr_chart.drawChart(null, (int) (Math.random()*100));

            for(int y = 0; y<20; y++){
                d[y] = (byte)(Math.random()*255);
            }
            ecg_chart.drawChart(d,0);

    }
}
