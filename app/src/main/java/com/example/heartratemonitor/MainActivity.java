package com.example.heartratemonitor;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HeartRateMonitorLogTag";

    private static final String BT_NAME_KEY = "key_bt_name_preferences";
    private static final String BT_ADDR_KEY = "key_bt_addr_preferences";

    private static final String HC08_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String HC08_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private static final String CHECKBOX_SCR_ON_KEY = "key_screenon_preferences";

    Charting ecg_chart, hr_chart;
    Thread thr; //del
    private boolean mRun = false;

    Intent gattServiceIntent;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private boolean mBound = false;
    private BluetoothGattCharacteristic ourGattCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private TextView textView;
    private Button btn;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Для версий андроида больше 6, система требует предоставления разрешений в рантайме, а не при установке,
        // как это было ранее.
        // For versions of android greater than 6, the system requires permissions at runtime,
        // rather than during installation, as it was before.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// Android M Permission check 
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                // Permission has already been granted
                if (savedInstanceState != null){
                    return;
                }

            }
        } else {

            if (savedInstanceState != null){
                return;
            } // Для андроид 5 и ниже // for android 5 and below
        }

        //AppBar
        Toolbar myToolbar = findViewById(R.id.appbar_layout);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        LineChart lCh = findViewById(R.id.cardio_chart);
        LineChart lCh1 = findViewById(R.id.bpm_chart);
        ecg_chart = new Charting(lCh, Charting.ChartType.ECG);
        ecg_chart.setMaxViewPoints(2000);

        hr_chart = new Charting(lCh1, Charting.ChartType.HR);//Heart Rate chart
        hr_chart.setMaxViewPoints(100);

        textView = findViewById(R.id.bpm);
        btn = findViewById(R.id.clearchart_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Clear Heart Rate Chart")
                        .setMessage("Are you sure you want to clear heart rate chart?")

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                hr_chart.clear();
                            }
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();


            }
        });



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
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0));
                updateCardioChart(intent.getBundleExtra(BluetoothLeService.EXTRA_DATA2));


            }
        }

        private void displayData(int point_count) {
            if (point_count == -1)return;
            else {
                float bpm = (500 / (float) point_count) * 60;
                textView.setText(String.format("%.1f", bpm));
                hr_chart.drawChart(null, bpm);
            }
        }

        private void updateCardioChart(Bundle b) {
            byte[] receivedData = b.getByteArray("ReceivedData");
            ecg_chart.drawChart(receivedData, 0);
        }

        private void connectToGATTCharacteristic(List<BluetoothGattService> supportedGattServices) {
            // Loops through available GATT Services.
            for (BluetoothGattService gattService : supportedGattServices) {
                if (gattService.getUuid().toString().equals(HC08_SERVICE_UUID)){
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
                        if (gattCharacteristic.getUuid().toString().equals(HC08_CHARACTERISTIC_UUID))
                        {
                            int charaProp = gattCharacteristic.getProperties();
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ) {
                                // If there is an active notification on a characteristic, clear
                                // it first so it doesn't update the data field on the user interface.
                                if (mNotifyCharacteristic != null) {
                                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                                    mNotifyCharacteristic = null;
                                }
                                mBluetoothLeService.readCharacteristic(gattCharacteristic);
                            }
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                                mNotifyCharacteristic = gattCharacteristic;
                                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                            }
                            mBluetoothLeService.readCharacteristic(gattCharacteristic);
                        }
                    }
                }
            }
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
                Toast.makeText(this, "Bluetooth device is not found. Please scan to find.", Toast.LENGTH_SHORT).show();
                return;
            }

            getSupportActionBar().setTitle(mDeviceName);
            gattServiceIntent = new Intent(this, BluetoothLeService.class);
            gattServiceIntent.putExtra("CompareLine_value", 140);
            boolean bindservise = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            Log.d(TAG, "ServiceBinding=" + Boolean.toString(bindservise));
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
            //mBluetoothLeService.connect(mDeviceAddress);
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        switch (item.getItemId()){
            case R.id.menu_ble_settings:
                startActivity(new Intent(this, BLESettingsActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_run:
                if (mBluetoothLeService != null){
                    mBluetoothLeService.connect(mDeviceAddress);
                    if (sharedPreferences.getBoolean(CHECKBOX_SCR_ON_KEY, false))
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else Toast.makeText(this,"No BT Device. mBluetoothLeService = null",Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_stop:
                mBluetoothLeService.disconnect();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Поток для непрерывного отображения данных
    // Stream for continuous data mapping
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
