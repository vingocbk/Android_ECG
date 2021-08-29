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
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
    double y = 0;
    double x = 0;
    Button btnSend;
    Spinner spnFrequency;
    CheckBox chekRealTime;
    ImageView imgBleConnect;
    ProgressBar prgLoadBluetooth;
    int start_send_data             = 's'
            , stop_send_data        = 't'
            , send_real_time        = 'u'
            , send_not_real_time    = 'v';

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
        setDataBegin();


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i = 0; i < 1000; i++){
                    x = x+1;
                    y = x*x;
                    series.appendData(new DataPoint(x, y),true, 1000);
                }
//                graph.getViewport().scrollToEnd();
//                graph.addSeries(series);

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
        btnSend = findViewById(id.btnSend);
        graph = (GraphView) findViewById(id.graph);
        imgBleConnect = findViewById(id.imgBleConnect);
        prgLoadBluetooth = findViewById(R.id.prgLoadBluetooth);
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
        spnFrequency.setSelection(1);
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

//        graph.getViewport().setMinY(-2);
//        graph.getViewport().setMaxY(2);

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
                if(result.getDevice().getName().equals("Chat_1_2") ){
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
                series.appendData(new DataPoint(x, (double)dataEcg),true, 1000);   //add data to graph
            }
            else{
                Log.d("onCharacteristicChanged", "onCharacteristicChanged" + Arrays.toString(characteristic.getValue()));
                DataPoint[] DataPoint_ecg = new DataPoint[characteristic.getValue().length/2];
                if(characteristic.getValue().length >= 19){
                    for(int i = 0; i < characteristic.getValue().length; i=i+2){
                        x += 4;
                        dataEcg = (Byte.toUnsignedInt( characteristic.getValue()[i] ) << 8) + Byte.toUnsignedInt( characteristic.getValue()[i+1]);
                        if(pre_dataEcg != dataEcg - 1){
                            count_lost++;
                            Log.d("onCharacteristicChanged", "value lost: " + count_lost);
                        }
                        pre_dataEcg = dataEcg;
                        Log.d("onCharacteristicChanged", "value: " + dataEcg);
                        DataPoint v = new DataPoint(x, (double)dataEcg);
                        DataPoint_ecg[i/2] = v;
                        series.appendData(new DataPoint(x, (double)dataEcg),true, 1000);   //add data to graph
                    }
                    //series.appendData(DataPoint_ecg,true, 1000);   //add data to graph
                    //series.resetData(DataPoint_ecg);
                }

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt,    characteristic, status);
            Log.d("onCharacteristicWrite", "Characteristic " + Arrays.toString(characteristic.getValue()) + " written");

//            if(!mGatt.readCharacteristic(mReadCharacteristic)){
//                Log.w("readCharacteristic", "Failed to read characteristic");
//            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.getStringValue(0));
        }
    };


}

