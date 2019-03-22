package com.example.heartratemonitor;

import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HeartRateMonitorLogTag";

    private static final String BT_NAME_KEY = "key_bt_name_preferences";
    private static final String BT_ADDR_KEY = "key_bt_addr_preferences";

    Charting ecg_chart, hr_chart;
    Thread thr; //del
    private boolean mRun = false;

    Intent gattServiceIntent;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private boolean mBound = false;


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

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                connectToGATTCharacteristic(mBluetoothLeService.getSupportedGattServices());
                Toast.makeText(getApplicationContext(), "BLE Services discovered", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //updateChart(intent.getBundleExtra(BluetoothLeService.EXTRA_DATA2));

            }
        }

        private void connectToGATTCharacteristic(List<BluetoothGattService> supportedGattServices) {

        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        if (!mBound){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPreferences.contains(BT_NAME_KEY) && sharedPreferences.contains(BT_ADDR_KEY)) {
                mDeviceName = sharedPreferences.getString(BT_NAME_KEY, "0");
                mDeviceAddress = sharedPreferences.getString(BT_ADDR_KEY, "0");
            }else{
                Toast.makeText(this, "Bluetooth device is not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            getSupportActionBar().setTitle(mDeviceName);
            gattServiceIntent = new Intent(this, com.example.heartratemonitor.BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }


    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            mBound = true;
            Log.d(TAG, "MainActivity onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            mBound = false;
            Log.d(TAG, "MainActivity onServiceDisconnected");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (!mConnected) {
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
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_stop:
                mBluetoothLeService.disconnect();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Поток для непрерывного отображения данных
    private void showChartData() {
        thr = new Thread(){
            public void run() {
                while (mRun) {
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
