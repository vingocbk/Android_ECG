package com.example.ecg;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static com.example.ecg.R.*;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.fitness.data.DataPoint;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@TargetApi(21)
public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    String LogFunction = "function";
    String deviceName = "ElcomEcg";
    private static final int REQUEST_BLUETOOTH_ADMIN_ID = 1;
    private static final int REQUEST_LOCATION_ID = 2;
    private static final int REQUEST_BLUETOOTH_ID = 3;

//    BluetoothGatt mGatt;
    BluetoothGattService mCustomService;
    BluetoothGattCharacteristic mWriteCharacteristic;
    BluetoothGattCharacteristic mReadCharacteristic;
    List<BluetoothGattService> services;
    //graph
    GraphView graph;
    LineGraphSeries<DataPoint> series;
    LineChart mpLineChart;
    float y = 0;
    float x = 0;
    Button btnSend;
    Spinner spnFrequency;
    CheckBox chekRealTime;
    ImageView imgBleConnect;
    TextView txtBattery;
    ProgressBar prgLoadBluetooth;
    int start_send_data             = 's'
            , stop_send_data        = 't'
            , send_real_time        = 'u'
            , send_not_real_time    = 'v'
            , read_battery          = 'b';

    int[] frequency = {'0','1','2','3'};
    int dataEcg = 0, pre_dataEcg = 0, count_lost = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        //hide action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


        Log.i(LogFunction, "onCreate");
        bleCheck();
        locationCheck();
        initLayout();
        initMpChart();
        setDataBegin();


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                for(int i = 0; i < 1000; i++){
//                    x = x+1;
//                    y = x*x;
//                    series.appendData(new DataPoint(x, y),true, 1000);
//                }
//                graph.getViewport().scrollToEnd();
//                graph.addSeries(series);

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        for(int i =0; i < 1000; i++){
//                            y = Math.sin(x);
//                            int finalI = i;
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
////                            addEntry();
//                                    LineData data = mpLineChart.getData();
//                                    data.addEntry(new Entry(finalI, (float) Math.random()), 0);
//                                    //notify chart data data changed
//                                    mpLineChart.notifyDataSetChanged();
//                                    mpLineChart.setVisibleXRange(0,100);
//                                    mpLineChart.moveViewToX(data.getXMax());
//
//                                }
//                            });
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }).start();

                if(mGatt == null || mCustomService == null){
                    Log.w("writeCharacteristic", "NOT CONNECT");
                    return;
                }
                if(btnSend.getText().toString().equals("START")){
                    btnSend.setText("STOP");
                    mWriteCharacteristic.setValue(start_send_data,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
                    mGatt.writeCharacteristic(mWriteCharacteristic);
                }
                else if(btnSend.getText().toString().equals("STOP")){
                    btnSend.setText("START");
                    mWriteCharacteristic.setValue(stop_send_data,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
                    mGatt.writeCharacteristic(mWriteCharacteristic);
                }
            }
        });

        spnFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //Toast.makeText(MainActivity.this, String.valueOf(frequency[i]), Toast.LENGTH_SHORT).show();
                if(mGatt == null || mCustomService == null){
                    Log.w("writeCharacteristic", "NOT CONNECT");
                    return;
                }
                mWriteCharacteristic.setValue(frequency[i],android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
                mGatt.writeCharacteristic(mWriteCharacteristic);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        chekRealTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(mGatt == null || mCustomService == null){
                    Log.w("writeCharacteristic", "NOT CONNECT");
                    return;
                }
                if(b){
                    mWriteCharacteristic.setValue(send_real_time,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
                }
                else{
                    mWriteCharacteristic.setValue(send_not_real_time,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
                }
                mGatt.writeCharacteristic(mWriteCharacteristic);

//                mWriteCharacteristic.setValue(read_battery,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
//                mGatt.writeCharacteristic(mWriteCharacteristic);

            }
        });

        imgBleConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prgLoadBluetooth.setVisibility(View.VISIBLE);
                onResume();
            }
        });

    }


    private void bleCheck() {
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth permission has not been granted.
            ActivityCompat.requestPermissions(this,new String[]{BLUETOOTH},REQUEST_BLUETOOTH_ID);
        }
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth admin permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_ADMIN_ID);
        }
    }

    private void locationCheck() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Location permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION_ID);
        }
    }

    private void initLayout() {
        chekRealTime = findViewById(R.id.chekRealTime);
        spnFrequency = findViewById(R.id.spnFrequency);
        btnSend = findViewById(R.id.btnSend);
        graph = (GraphView) findViewById(R.id.graph);
        imgBleConnect = findViewById(R.id.imgBleConnect);
        txtBattery = findViewById(R.id.txtBattery);
        prgLoadBluetooth = findViewById(R.id.prgLoadBluetooth);
        mpLineChart = findViewById(R.id.mpLineChart);
    }

    private void initMpChart() {
        List<Entry> lineEntries = getDataSet();
//        LineDataSet lineDataSet = new LineDataSet(lineEntries, "ECG");
        LineDataSet lineDataSet = new LineDataSet(null, "ECG");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setLineWidth(2);
        lineDataSet.setDrawValues(false);
        lineDataSet.setColor(Color.CYAN);
        lineDataSet.setCircleRadius(6);
        lineDataSet.setCircleHoleRadius(3);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawHighlightIndicators(true);
        lineDataSet.setHighlightEnabled(true);
        lineDataSet.setHighLightColor(Color.CYAN);
        lineDataSet.setValueTextSize(12);
        lineDataSet.setValueTextColor(Color.DKGRAY);
        lineDataSet.setMode(LineDataSet.Mode.STEPPED);

        LineData lineData = new LineData(lineDataSet);
        mpLineChart.getDescription().setTextSize(12);
        mpLineChart.getDescription().setEnabled(false);
        mpLineChart.animateY(1000);
        mpLineChart.setData(lineData);
//        mpLineChart.data
//        mpLineChart.add

        // Setup X Axis
        XAxis xAxis = mpLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(1.0f);
        xAxis.setXOffset(1f);
        xAxis.setLabelCount(25);
        xAxis.setAvoidFirstLastClipping(true);
//        xAxis.setAxisMinimum(0);
//        xAxis.setAxisMaximum(24);

        // Setup Y Axis
        YAxis yAxis = mpLineChart.getAxisLeft();
//        yAxis.setAxisMinimum(0);
//        yAxis.setAxisMaximum(40);
        yAxis.setGranularity(1f);

//        ArrayList<String> yAxisLabel = new ArrayList<>();
//        yAxisLabel.add(" ");
//        yAxisLabel.add("Rest");
//        yAxisLabel.add("Work");
//        yAxisLabel.add("2-up");
//
//        mpLineChart.getAxisLeft().setCenterAxisLabels(true);
//        mpLineChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getAxisLabel(float value, AxisBase axis) {
//                if(value == -1 || value >= yAxisLabel.size()) return "";
//                return yAxisLabel.get((int) value);
//            }
//        });

//        mpLineChart.getAxisRight().setEnabled(false);
//        mpLineChart.invalidate();





    }
    private List<Entry> getDataSet() {
        List<Entry> lineEntries = new ArrayList<>();
//        lineEntries.add(new Entry(0, 1));
//        lineEntries.add(new Entry(1, 2));
//        lineEntries.add(new Entry(2, 3));
//        lineEntries.add(new Entry(3, 1));
//        lineEntries.add(new Entry(4, 1));
//        lineEntries.add(new Entry(5, 1));
//
//        lineEntries.add(new Entry(6, 2));
//        lineEntries.add(new Entry(7, 2));
//        lineEntries.add(new Entry(8, 2));
//        lineEntries.add(new Entry(9, 2));
//        lineEntries.add(new Entry(10, 2));
//
//        lineEntries.add(new Entry(11, 1));
//        lineEntries.add(new Entry(12, 1));
//
//        lineEntries.add(new Entry(13, 2));
//        lineEntries.add(new Entry(14, 2));
//        lineEntries.add(new Entry(15, 2));
//
//        lineEntries.add(new Entry(16, 1));
//        lineEntries.add(new Entry(17, 1));
//
//        lineEntries.add(new Entry(18, 2));
//        lineEntries.add(new Entry(19, 2));
//        lineEntries.add(new Entry(20, 2));
//        lineEntries.add(new Entry(21, 2));
//
//        lineEntries.add(new Entry(22, 1));
//        lineEntries.add(new Entry(23, 1));
//        lineEntries.add(new Entry(24, 1));
        return lineEntries;
    }

    //need to create method to add entry to the line chart
    private void addEntry(){
        LineData data = mpLineChart.getData();
        if(data != null){
//            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
//            if(set == null){
//                //creation if null
//                set = createSet();
//                data.addDataSet(set);
//            }
            //add a new random value
            data.addEntry(new Entry(50, 1), 0);
            //notify chart data data changed
            mpLineChart.notifyDataSetChanged();
            //limit number of visible entry
            mpLineChart.setVisibleXRange(0,16);
            //scroll  to the last entry
            mpLineChart.moveViewToX(data.getXMax()-7);
        }
    }

    //method to createSet
    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null, "SPL Db");
        set.setDrawCircles(true);

        return set;
    }





    private void setDataBegin() {
        List<String> list = new ArrayList<>();
        list.add("125Hz");
        list.add("250Hz");
        list.add("500Hz");
        list.add("1KHz");
        //ArrayAdapter spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        ArrayAdapter spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spnFrequency.setAdapter(spinnerAdapter);
        spnFrequency.setSelection(0);
        customGraph();
    }

    protected void customGraph() {
        series = new LineGraphSeries<DataPoint>();
//        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScalable(true);
//        graph.getViewport().setScalableY(true);

        graph.getViewport().setScrollable(true);
        graph.getViewport().setScrollableY(true);
//        graph.getViewport().setMaxXAxisSize(1000);

        graph.getViewport().setMinY(1000);
        graph.getViewport().setMaxY(3300);

        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (ms)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Voltage (mV)");

        graph.addSeries(series);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LogFunction, "onResume");
//        imgBleConnect.setBackgroundResource(mipmap.ble_disconnect);
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(LogFunction, "onResume -> enableBtIntent");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                Log.i(LogFunction, "onResume -> mLEScanner");
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LogFunction, "onPause");
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
            mGatt.disconnect();
        }

    }
    @Override
    protected void onDestroy() {
        Log.i(LogFunction, "onDestroy");
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LogFunction, "onActivityResult");
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        Log.i(LogFunction, "scanLeDevice " + enable);
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        Log.i(LogFunction, "run stopLeScan");
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        Log.i(LogFunction, "run stopScan");
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                Log.i(LogFunction, "startLeScan");
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                Log.i(LogFunction, "startScan");
//                mLEScanner.startScan(mScanCallback);
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                Log.i(LogFunction, "stopLeScan");
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                Log.i(LogFunction, "stopScan");
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(LogFunction, "onScanResult");
            Log.i("callbaogckType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            //Log.i("getName", result.getDevice().getName());
            if(result.getDevice().getName() != null)
            {
                if(result.getDevice().getName().equals(deviceName) ){
                    BluetoothDevice btDevice = result.getDevice();
                    connectToDevice(btDevice);
                }
            }

        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(LogFunction, "onBatchScanResults");
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.i(LogFunction, "onScanFailed");
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    Log.i(LogFunction, "onLeScan");
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            Log.i(LogFunction, "connectToDevice");
            mGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            Log.i(LogFunction, "onConnectionStateChange");
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    //Toast.makeText(MainActivity.this, "CONNECTED to " + gatt.getDevice().getName().toString(), Toast.LENGTH_SHORT).show();
                    Log.i("gattCallback", "STATE_CONNECTED");
                    prgLoadBluetooth.setVisibility(View.INVISIBLE);
                    imgBleConnect.setBackgroundResource(R.mipmap.ble_connect);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    imgBleConnect.setBackgroundResource(mipmap.ble_disconnect);
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LogFunction, "onServicesDiscovered");
                services = gatt.getServices();

                mGatt = gatt;
                /*get the service characteristic from the service*/
//                mCustomService = gatt.getService(UUID.fromString("D973F2E0-B19E-11E2-9E96-0800200C9A66"));
                mCustomService = gatt.getService(services.get(2).getUuid());

                /*get the write characteristic from the service*/
//                mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("D973F2E2-B19E-11E2-9E96-0800200C9A66"));
                mWriteCharacteristic = mCustomService.getCharacteristic(services.get(2).getCharacteristics().get(1).getUuid());

                /*get the read characteristic from the service*/
//                mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString("d973f2e1-b19e-11e2-9e96-0800200c9a66"));
                mReadCharacteristic = mCustomService.getCharacteristic(services.get(2).getCharacteristics().get(0).getUuid());

                /*turn on notification to listen data on ReadCharacteristic*/
                if (!mGatt.setCharacteristicNotification(mReadCharacteristic,true)) {
                    Log.w("writeCharacteristic", "Failed to setCharacteristicNotification");
                }
                BluetoothGattDescriptor descriptor = mReadCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?

            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Log.d("onCharacteristicChanged", "onCharacteristicChanged" + Arrays.toString(characteristic.getValue()));
            if(characteristic.getValue().length == 3){
                switch (characteristic.getValue()[0]) {
                    //Battery
                    case (byte)0xFF:
                        int dataBattery = (Byte.toUnsignedInt( characteristic.getValue()[1] ) << 8) + Byte.toUnsignedInt( characteristic.getValue()[2]);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String battery = String.valueOf(dataBattery) + "mV";
                                txtBattery.setText(battery);
                            }
                        });
                        break;
                    default:
                        break;
                }
            }
            if(chekRealTime.isChecked()){
                dataEcg = (Byte.toUnsignedInt( characteristic.getValue()[0] ) << 8) + Byte.toUnsignedInt( characteristic.getValue()[1] );
                //Log.d("onCharacteristicChanged", "data: " + dataEcg);
                if(dataEcg - pre_dataEcg != 1){
                    count_lost += (dataEcg - pre_dataEcg - 1);
                    Log.d("onCharacteristicChanged", "data lost: " + count_lost);
                }
                pre_dataEcg = dataEcg;
                switch (spnFrequency.getSelectedItemPosition()){
                    case 0:     //125 time per second
                        x += 8;
                        break;
                    case 1:     //250 time per second
                        x += 4;
                        break;
                    case 2:     //500 time per second
                        x += 2;
                        break;
                    case 3:     //1000 time per second
                        x += 1;
                        break;
                    default:
                        break;
                }
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        series.appendData(new DataPoint(x, (double)dataEcg),true, 1000);   //add data to graph

//                        try {
//                            series.wait();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                    }
                });

//                LineData data = mpLineChart.getData();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        data.addEntry(new Entry(x, (float) dataEcg), 0);
//                        //notify chart data data changed
//                        mpLineChart.notifyDataSetChanged();
//                        mpLineChart.setVisibleXRange(0,1000);
//                        mpLineChart.moveViewToX(data.getXMax());
//                    }
//                });
            }
            else{
                Log.d("onCharacteristicChanged", "onCharacteristicChanged" + Arrays.toString(characteristic.getValue()));
                DataPoint[] DataPoint_ecg = new DataPoint[characteristic.getValue().length/2];
                if(characteristic.getValue().length >= 19){
                    LineData data = mpLineChart.getData();
                    for(int i = 0; i < characteristic.getValue().length; i=i+2){
                        switch (spnFrequency.getSelectedItemPosition()){
                            case 0:     //125 time per second
                                x += 8;
                                break;
                            case 1:     //250 time per second
                                x += 4;
                                break;
                            case 2:     //500 time per second
                                x += 2;
                                break;
                            case 3:     //1000 time per second
                                x += 1;
                                break;
                            default:
                                break;
                        }
                        dataEcg = (Byte.toUnsignedInt( characteristic.getValue()[i] ) << 8) + Byte.toUnsignedInt( characteristic.getValue()[i+1]);
                        if(pre_dataEcg != dataEcg - 1){
                            count_lost++;
//                            Log.d("onCharacteristicChanged", "value lost: " + count_lost);
                        }
                        pre_dataEcg = dataEcg;
                        Log.d("onCharacteristicChanged", "value: " + dataEcg);
//                        Log.d("onCharacteristicChanged", "value lost: " + count_lost);
                        DataPoint v = new DataPoint(x, (double)dataEcg);
                        DataPoint_ecg[i/2] = v;
                        final CountDownLatch latch = new CountDownLatch(1);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("onCharacteristicChanged", "value---: " + dataEcg);
                                series.appendData(new DataPoint(x, (double)dataEcg),true, 1000);   //add data to graph
                                latch.countDown();
//                                latch.
//                                try {
//                                    series.wait(1);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                data.addEntry(new Entry(x, (float) dataEcg), 0);
//                                //notify chart data data changed
//                                mpLineChart.notifyDataSetChanged();
//                                mpLineChart.setVisibleXRange(0,1000);
//                                mpLineChart.moveViewToX(data.getXMax());
                            }
                        });
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt,    characteristic, status);
            Log.d("onCharacteristicWrite", "Characteristic " + Arrays.toString(characteristic.getValue()) + " written");
//                if(characteristic.getValue()[0] == read_battery){
//                    if(!mGatt.readCharacteristic(mReadCharacteristic)){
//                        Log.w("readCharacteristic", "Failed to read_battery");
//                    }
//                }

        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", Arrays.toString(characteristic.getValue()));

        }
    };


}

