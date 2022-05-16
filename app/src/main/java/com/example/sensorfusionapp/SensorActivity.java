package com.example.sensorfusionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.sensorfusionapp.databinding.ActivitySensorBinding;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SensorActivity extends AppCompatActivity implements SensorEventListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivitySensorBinding binding;
    private SensorManager sensorManager;
    private Sensor mPressure;
    WifiManager wifiManager;
    List<ScanResult> scanResults = new ArrayList<>();
    ListView simpleListView;
    BroadcastReceiver wifiScanReceiver;
    Map<String, String> map = new HashMap<>();

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void scanSuccess() {
        scanResults.clear();
        scanResults = wifiManager.getScanResults();
        System.out.println("=====SCAN SUCCESS=====");
        //... use new scan results ...
        List<String> list = new ArrayList<>();
        list.add("=========WIFIs=======");
        for (ScanResult r : scanResults) {
            list.add(r.toString());
        }

        updateListView(list);
        return;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void scanRTT() {
        // else do RTT
        System.out.println("=====DO RTT Next =========");
        WifiRttManager mgr =
                (WifiRttManager) getApplicationContext().getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        map.clear();
        RangingRequest.Builder builder2 = new RangingRequest.Builder();
        int count = 0;

        for (int i = 0; i < scanResults.size(); i++) {
            ScanResult sr = scanResults.get(i);
            //if (i % 2 != 0) continue;
            builder2.addAccessPoint(sr);
            System.out.println("ADDED FOR Range Req:" + sr.SSID + "-Mac:" + sr.BSSID);
            map.put(sr.BSSID, sr.SSID);
            count++;
            if (count >= 3) break;
        }
        RangingRequest req2 = builder2.build();
        System.out.println("====RANG REQ ====" + req2);
        Executor executor = getApplicationContext().getMainExecutor();//(ThreadPoolExecutor) Executors.newCachedThreadPool();

        mgr.startRanging(req2, executor, new RangingResultCallback() {

            @Override
            public void onRangingFailure(int code) {
                System.out.println("++++++ FAILURE TO WIFI RTT +++++++");
            }

            @Override
            public void onRangingResults(@NonNull List<RangingResult> list) {
                System.out.println("++++++ SUCCESS TO WIFI RTT +++++++");
                List<String> viewlist = new ArrayList<>();
                for (RangingResult rr : list) {
                    viewlist.add(map.get(rr.getMacAddress().toString()) + "-->>"+ rr.toString());
                    System.out.println("=====" + rr + "=====");
                    if (rr.getStatus() == RangingResult.STATUS_SUCCESS) {
                        System.out.println("*****LOC:" + rr.getUnverifiedResponderLocation() + "****");
                        //System.out.println("*****ALT:" + rr.getUnverifiedResponderLocation().getAltitude() + "****");
                    }
                }
                updateListView(viewlist);
            }
        });
    }

    private void scanFailure() {
        System.out.println("=====SCAN FAILED=====");
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();
        //... potentially use older scan results ...
        for (ScanResult r : results) {
            System.out.println(r);
        }
    }
    private void scanWifi() {
        scanResults.clear();
        wifiScanReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    // scan failure handling
                    scanFailure();
                }
            }
        };
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

        boolean success = wifiManager.startScan();
        if (!success) {
            // scan failure handling
            scanFailure();
        }
    }
    private void scanSensors() {
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        List<String> list = new ArrayList<>();
        list.add("=========SENSORS=======");
        for (Sensor s : deviceSensors) {
            list.add(s.toString());
        }
        updateListView(list);
    }
    private void updateListView(List<String> list) {
        String[] sArr = new String[list.size()];
        int i = 0;
        for(String s : list) {
            sArr[i++] = s;
        }
        simpleListView = (ListView) findViewById(R.id.list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                R.layout.item_view, R.id.itemTextView, sArr);
        simpleListView.setAdapter(arrayAdapter);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        binding = ActivitySensorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Button b1 = (Button) findViewById(R.id.button1);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("=======ON Click=====");
                scanSensors();
            }
        });
        Button b2 = (Button) findViewById(R.id.button2);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        Button b3 = (Button) findViewById(R.id.button3);
        b3.setOnClickListener(new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onClick(View view) {
            scanRTT();
        }
    });
}

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The pressure sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        float lux = event.values[0];
        // Do something with this sensor value.
        TextView sensorValueTextView = (TextView) findViewById(R.id.pressureValue);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < event.values.length; i++) sb.append(event.values[i]+",");
        sensorValueTextView.setText("Val="+event.sensor.toString() + " values="+sb.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

}