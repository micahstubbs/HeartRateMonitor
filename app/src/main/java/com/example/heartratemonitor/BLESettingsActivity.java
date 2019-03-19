package com.example.heartratemonitor;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BLESettingsActivity extends AppCompatActivity {

    private boolean mScanning = false;
    private boolean mScanningVisibility = false;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    ListView listView;

    private static final long SCAN_PERIOD = 10000;

    TextView bt_name, bt_addr;

    SharedPreferences sharedPreferences;
    private static final String BT_NAME_KEY = "key_bt_name_preferences";
    private static final String BT_ADDR_KEY = "key_bt_addr_preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blesettings);

        final Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("BLE Settings");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor shped = sharedPreferences.edit();

        bt_name = (TextView)findViewById(R.id.bt_name);
        bt_addr = (TextView)findViewById(R.id.bt_addr);

        bt_addr.setText(sharedPreferences.getString(BT_ADDR_KEY, ""));
        bt_name.setText(sharedPreferences.getString(BT_NAME_KEY, ""));

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)  {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            //getActivity().finish();
            mScanningVisibility = false;}
        else mScanningVisibility = true;
        invalidateOptionsMenu();

        if (mLeDeviceListAdapter == null) {
            mLeDeviceListAdapter = new LeDeviceListAdapter();
        }
        listView = findViewById(R.id.listview);
        listView.setAdapter(mLeDeviceListAdapter);

        mHandler = new Handler();

        //Обработка нажатия на лист
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                String devAddr = device.getAddress();
                String devName = device.getName();
                shped.putString(BT_ADDR_KEY, devAddr);
                shped.putString(BT_NAME_KEY, devName);
                shped.commit();
                bt_addr.setText(sharedPreferences.getString(BT_ADDR_KEY, ""));
                bt_name.setText(sharedPreferences.getString(BT_NAME_KEY, ""));
                finish();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_menu, menu);
        if(!mScanningVisibility){
            menu.findItem(R.id.menu_stopscan).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }else{
            if (!mScanning) {
                menu.findItem(R.id.menu_stopscan).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                menu.findItem(R.id.menu_stopscan).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.progressbar_layout);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.menu_scan:
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent();
                    enableBtIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(enableBtIntent);
                    return false;
                }

                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stopscan:
                scanLeDevice(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            //TODO: add the myHandler.postDelayed(myRunnable, SPLASH_DISPLAY_LENGTH); or myHandler.removeCallbacks(myRunnable); to cancel the postdelayed track
            mScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
    }


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(),"Scan Error: "+String.valueOf(errorCode),Toast.LENGTH_SHORT).show();
        }
    };



    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflater;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflater = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflater.inflate(R.layout.list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            final String deviceAddress = device.getAddress();
            if (deviceName != null && deviceName.length() > 0) viewHolder.deviceName.setText(deviceName);
            else viewHolder.deviceName.setText(R.string.unknown_device);
            if (deviceAddress !=null && deviceAddress.length() >0) viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }





}

