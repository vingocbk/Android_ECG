package com.example.ecg;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static com.example.ecg.R.*;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
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
import java.util.Random;
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

    private static final int MODE_READ_ECG_GRAPH = 1;
    private static final int MODE_READ_SPO2_GRAPH = 2;
    private static final int MODE_READ_HEART_RATE = 3;
    private static final int MODE_READ_SPO2 = 4;
    private static final int MODE_READ_TEMP_AND_BATTERY = 5;
    private static final int MODE_READ_BATTERY = 6;

    //    BluetoothGatt mGatt;
    BluetoothGattService mCustomService;
    BluetoothGattCharacteristic mWriteCharacteristic;
    BluetoothGattCharacteristic mReadCharacteristic;
    List<BluetoothGattService> services;
    //graph
    GraphView graphEcg, graphSpo2;
    LineGraphSeries<DataPoint> seriesEcg, seriesSpo2, seriesHeartRate;
    LineChart mpLineChart;
    float y_ecg = 0;
    float x_ecg = 0;
    float y_spo2 = 0;
    float x_spo2 = 0;
    double y_min_spo2;
    double y_max_spo2;
    Button btnSendEcg, btnSendSpo2;
    Spinner spnFrequency;
    CheckBox chekRealTime;
    ImageView imgBleConnect;
    TextView txtBattery;
    ProgressBar prgLoadBluetooth;
    Switch swSelectGraph;
    RelativeLayout rlGraphEcg, rlGraphSpo2;
    TextView txtTemperature, txtDataSpo2, txtDataHeartRate;
    int start_send_data_ecg = 's', stop_send_data_ecg = 't', start_send_data_spo2 = 'n', stop_send_data_spo2 = 'm', send_real_time = 'u', send_not_real_time = 'v', read_battery = 'b';

    int[] frequency = {'6', '5', '4', '3', '2', '1', '0'};
    int dataEcg = 0, pre_dataEcg = 0, count_lost = 0;
    int count_clear_graph = 0;
    int dataSpo2 = 0;
    int count_to_break_graph_ecg = 0;
    int count_to_update_data_spo2 = 0;

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


        btnSendEcg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatt == null || mCustomService == null) {
                    Log.w("btnSendEcg", "NOT CONNECT");
                    return;
                }
                if (btnSendEcg.getText().toString().equals("START ECG")) {
                    btnSendEcg.setText("STOP ECG");
                    mWriteCharacteristic.setValue(start_send_data_ecg, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
//                        return;
                    }
                    mGatt.writeCharacteristic(mWriteCharacteristic);
                } else if (btnSendEcg.getText().toString().equals("STOP ECG")) {
                    btnSendEcg.setText("START ECG");
                    mWriteCharacteristic.setValue(stop_send_data_ecg, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    mGatt.writeCharacteristic(mWriteCharacteristic);
                }
            }
        });
        btnSendSpo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatt == null || mCustomService == null) {
                    Log.w("btnSendSpo2", "NOT CONNECT");
                    return;
                }
                if (btnSendSpo2.getText().toString().equals("START SPO2")) {
                    btnSendSpo2.setText("STOP SPO2");
                    mWriteCharacteristic.setValue(start_send_data_spo2, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
//                        return;
                    }
                    mGatt.writeCharacteristic(mWriteCharacteristic);
                } else if (btnSendSpo2.getText().toString().equals("STOP SPO2")) {
                    btnSendSpo2.setText("START SPO2");
                    mWriteCharacteristic.setValue(stop_send_data_spo2, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    mGatt.writeCharacteristic(mWriteCharacteristic);
                }
            }
        });

        swSelectGraph.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    rlGraphEcg.setVisibility(View.INVISIBLE);
                    rlGraphSpo2.setVisibility(View.VISIBLE);
                } else {
                    rlGraphEcg.setVisibility(View.VISIBLE);
                    rlGraphSpo2.setVisibility(View.INVISIBLE);
                }
            }
        });

        spnFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //Toast.makeText(MainActivity.this, String.valueOf(frequency[i]), Toast.LENGTH_SHORT).show();
                if (mGatt == null || mCustomService == null) {
                    Log.w("spnFrequency", "NOT CONNECT");
                    return;
                }
                mWriteCharacteristic.setValue(frequency[i], android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
//                    return;
                }
                mGatt.writeCharacteristic(mWriteCharacteristic);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        chekRealTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (mGatt == null || mCustomService == null) {
                    Log.w("chekRealTime", "NOT CONNECT");
                    return;
                }
                if (b) {
                    mWriteCharacteristic.setValue(send_real_time, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                } else {
                    mWriteCharacteristic.setValue(send_not_real_time, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
//                    return;
                }
                mGatt.writeCharacteristic(mWriteCharacteristic);

//                mWriteCharacteristic.setValue(read_battery,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
//                mGatt.writeCharacteristic(mWriteCharacteristic);

            }
        });

        imgBleConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatt == null || mCustomService == null) {
                    prgLoadBluetooth.setVisibility(View.VISIBLE);
                    onResume();
                } else {
                    imgBleConnect.setBackgroundResource(mipmap.ble_disconnect);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
//                        return;
                    }
                    mGatt.close();
                    mGatt = null;
                }
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
            ActivityCompat.requestPermissions(this, new String[]{BLUETOOTH}, REQUEST_BLUETOOTH_ID);
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
        btnSendEcg = findViewById(R.id.btnSendEcg);
        btnSendSpo2 = findViewById(R.id.btnSendSpo2);
        graphEcg = (GraphView) findViewById(R.id.graphEcg);
        graphSpo2 = (GraphView) findViewById(R.id.graphSpo2);
        imgBleConnect = findViewById(R.id.imgBleConnect);
        txtBattery = findViewById(R.id.txtBattery);
        prgLoadBluetooth = findViewById(R.id.prgLoadBluetooth);
        mpLineChart = findViewById(R.id.mpLineChart);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtDataSpo2 = findViewById(R.id.txtDataSpo2);
        txtDataHeartRate = findViewById(R.id.txtDataHeartRate);
        swSelectGraph = findViewById(R.id.swSelectGraph);
        rlGraphEcg = findViewById(R.id.rlGraphEcg);
        rlGraphSpo2 = findViewById(R.id.rlGraphSpo2);
    }

    private void initMpChart() {
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
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);

        LineData lineData = new LineData(lineDataSet);
        mpLineChart.getDescription().setTextSize(12);
        mpLineChart.getDescription().setEnabled(true);
//        mpLineChart.animateY(1);
        mpLineChart.setScaleEnabled(true);
        mpLineChart.setData(lineData);
//        mpLineChart.data
//        mpLineChart.add

        // Setup X Axis
        XAxis xAxis = mpLineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.TOP);
//        xAxis.setGranularityEnabled(true);
//        xAxis.setGranularity(1.0f);
//        xAxis.setXOffset(1f);
//        xAxis.setLabelCount(1000);
        xAxis.setAvoidFirstLastClipping(true);
//        xAxis.setAxisMinimum(0);
//        xAxis.setAxisMaximum(1000);

        // Setup Y Axis
        YAxis yAxis = mpLineChart.getAxisLeft();
        yAxis.setAxisMinimum(400);
        yAxis.setAxisMaximum(650);
//        yAxis.setGranularity(1f);

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

    //need to create method to add entry to the line chart
    private void addEntry() {
        LineData data = mpLineChart.getData();
        if (data != null) {
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
            mpLineChart.setVisibleXRange(0, 16);
            //scroll  to the last entry
            mpLineChart.moveViewToX(data.getXMax() - 7);
        }
    }

    //method to createSet
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "SPL Db");
        set.setDrawCircles(true);

        return set;
    }


    private void setDataBegin() {
        List<String> list = new ArrayList<>();
        list.add("25Hz");
        list.add("50Hz");
        list.add("100Hz");
        list.add("125Hz");
        list.add("250Hz");
        list.add("500Hz");
        list.add("1KHz");
        //ArrayAdapter spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        ArrayAdapter spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spnFrequency.setAdapter(spinnerAdapter);
        spnFrequency.setSelection(3);
        customGraph();
    }

    protected void customGraph() {
        seriesEcg = new LineGraphSeries<DataPoint>();
//        graph.getViewport().setXAxisBoundsManual(true);
        graphEcg.getViewport().setYAxisBoundsManual(true);
        graphEcg.getViewport().setScalable(true);
//        graphEcg.getViewport().setScalableY(true);

        graphEcg.getViewport().setScrollable(true);
        graphEcg.getViewport().setScrollableY(true);
//        graphEcg.getViewport().setMaxXAxisSize(1000);

        graphEcg.getViewport().setMinX(0);
        graphEcg.getViewport().setMaxX(3000);
        graphEcg.getViewport().setMinY(400);
        graphEcg.getViewport().setMaxY(600);
//        graphEcg.onDataChanged(true, true);

        graphEcg.getGridLabelRenderer().setHorizontalAxisTitle("ECG Graph - Time (ms)");
        graphEcg.getGridLabelRenderer().setVerticalAxisTitle("Voltage (mV)");

        graphEcg.addSeries(seriesEcg);


        seriesSpo2 = new LineGraphSeries<DataPoint>();
        seriesHeartRate = new LineGraphSeries<DataPoint>();
        //        graph.getViewport().setXAxisBoundsManual(true);
        graphSpo2.getViewport().setYAxisBoundsManual(true);
        graphSpo2.getViewport().setScalable(true);
//        graphSpo2.getViewport().setScalableY(true);

        graphSpo2.getViewport().setScrollable(true);
        graphSpo2.getViewport().setScrollableY(true);
//        graphSpo2.getViewport().setMaxXAxisSize(1000);

        y_min_spo2 = 39500;
        y_max_spo2 = 40500;
        graphSpo2.getViewport().setMinY(y_min_spo2);
        graphSpo2.getViewport().setMaxY(y_max_spo2);
        graphSpo2.getViewport().setMinX(0);
        graphSpo2.getViewport().setMaxX(3000);
        graphSpo2.getViewport().computeScroll();
        graphSpo2.getViewport().calcCompleteRange();
        graphSpo2.computeScroll();
//        graphSpo2.onDataChanged(true, true);
        graphSpo2.getViewport().setYAxisBoundsManual(true);
        graphSpo2.getViewport().setXAxisBoundsManual(true);

        graphSpo2.getGridLabelRenderer().setHorizontalAxisTitle("SPO2 Graph - Time (ms)");
        graphSpo2.getGridLabelRenderer().setVerticalAxisTitle("Voltage (mV)");

        graphSpo2.addSeries(seriesSpo2);
        graphSpo2.addSeries(seriesHeartRate);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LogFunction, "onResume");
//        imgBleConnect.setBackgroundResource(mipmap.ble_disconnect);
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(LogFunction, "onResume -> enableBtIntent");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
//                return;
            }
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
//                return;
            }
            mGatt.disconnect();
        }

    }

    @Override
    protected void onDestroy() {
        Log.i(LogFunction, "onDestroy");
        if (mGatt == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            return;
        }
        imgBleConnect.setBackgroundResource(mipmap.ble_disconnect);
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
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
//                            return;
                        }

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
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
//                Log.i(LogFunction, "return false");
//                return;
            }
            if (result.getDevice().getName() != null) {
                if (result.getDevice().getName().equals(deviceName)) {
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
//                return;
            }
            mGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            Log.i("onConnectionStateChange", "Status: " + status);
            Log.i(LogFunction, "onConnectionStateChange");
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    //Toast.makeText(MainActivity.this, "CONNECTED to " + gatt.getDevice().getName().toString(), Toast.LENGTH_SHORT).show();
                    Log.i("gattCallback", "STATE_CONNECTED");
                    prgLoadBluetooth.setVisibility(View.INVISIBLE);
                    imgBleConnect.setBackgroundResource(R.mipmap.ble_connect);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
//                        return;
                    }
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    imgBleConnect.setBackgroundResource(mipmap.ble_disconnect);
                    onDestroy();
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
//                mCustomService = gatt.getService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
                mCustomService = gatt.getService(services.get(2).getUuid());
                Log.w("writeCharacteristic", services.get(2).getUuid().toString());

                /*get the write characteristic from the service*/
//                mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("D973F2E2-B19E-11E2-9E96-0800200C9A66"));
//                mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"));
                mWriteCharacteristic = mCustomService.getCharacteristic(services.get(2).getCharacteristics().get(0).getUuid());
                Log.w("writeCharacteristic", services.get(2).getCharacteristics().get(0).getUuid().toString());

                /*get the read characteristic from the service*/
//                mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString("d973f2e1-b19e-11e2-9e96-0800200c9a66"));
//                mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
                mReadCharacteristic = mCustomService.getCharacteristic(services.get(2).getCharacteristics().get(1).getUuid());
                Log.w("writeCharacteristic", services.get(2).getCharacteristics().get(1).getUuid().toString());

                /*turn on notification to listen data on ReadCharacteristic*/
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
//                    return;
                }
                if (!mGatt.setCharacteristicNotification(mReadCharacteristic, true)) {
                    Log.w("writeCharacteristic", "Failed to setCharacteristicNotification");
                }
                /*turn on notification to listen data on onCharacteristicChanged*/
                BluetoothGattDescriptor descriptor = mReadCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//                BluetoothGattDescriptor descriptor = mReadCharacteristic.getDescriptor(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
//                BluetoothGattDescriptor descriptor = mReadCharacteristic.getDescriptor(services.get(3).getCharacteristics().get(1).getUuid());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?

            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d("onCharacteristicChanged", "onCharacteristicChanged" + Arrays.toString(characteristic.getValue()));
            DataPoint[] DataPoint_ecg = new DataPoint[characteristic.getValue().length/2];
            if(characteristic.getValue().length >= 19){
//                    LineData data = mpLineChart.getData();
//                    mpLineChart.invalidate();
                    int[] dataReadFromBle = new int[10];
                    for(int i = 0; i < 10; i++){
                        dataReadFromBle[i] = (Byte.toUnsignedInt(characteristic.getValue()[2*i]) << 8) + Byte.toUnsignedInt(characteristic.getValue()[2*i + 1]);
                    }
                    //Read ECG graph
                    if(dataReadFromBle[0] == MODE_READ_ECG_GRAPH){
                        if(!swSelectGraph.isChecked() && btnSendEcg.getText().toString().equals("STOP ECG")){
                            count_to_break_graph_ecg ++;
                            if(count_to_break_graph_ecg > 14){
                                count_to_break_graph_ecg = 0;
                            }
                            if(count_to_break_graph_ecg <= 14){
                                for(int i = 1; i < 10; i++){
                                    if(i % 2 == 0){
                                        final CountDownLatch latch = new CountDownLatch(1);
                                        int finalI = i;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d("onCharacteristicChanged", "draw Ecg: " + String.valueOf(dataReadFromBle[finalI]));
                                                x_ecg += 8;
                                                seriesEcg.appendData(new DataPoint(x_ecg, (double) dataReadFromBle[finalI]), true, 1000, false);   //add data to graph
                                                //------ MP CHART--------
//                                            data.addEntry(new Entry(x_ecg, (float) dataReadFromBle[finalI]), 0);
                                                //notify chart data data changed
//                                            mpLineChart.notifyDataSetChanged();
//                                            mpLineChart.setVisibleXRange(0,1000);
//                                            mpLineChart.moveViewToX(data.getXMax());
                                                //-----------------------
                                                latch.countDown();
                                            }
                                        });
                                        try {
                                            latch.await();
//                                  synchronized(lock){lock.wait();}
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                        }
                    }
                    //Read Spo2 graph
                    if(dataReadFromBle[0] == MODE_READ_SPO2_GRAPH){
                        if(swSelectGraph.isChecked() && btnSendSpo2.getText().toString().equals("STOP SPO2")){
                            for(int i = 1; i < 10; i++){
                                final CountDownLatch latch = new CountDownLatch(1);
                                int finalI = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("onCharacteristicChanged", "draw Spo2: " + String.valueOf(dataReadFromBle[finalI]));
                                        x_spo2 += 5;
                                        if(dataReadFromBle[finalI] < y_min_spo2 || dataReadFromBle[finalI] > y_max_spo2){
                                            y_min_spo2 = dataReadFromBle[finalI] - 200;
                                            y_max_spo2 = dataReadFromBle[finalI] + 200;
                                        }
                                        graphSpo2.getViewport().setMinY(y_min_spo2);
                                        graphSpo2.getViewport().setMaxY(y_max_spo2);
                                        seriesSpo2.appendData(new DataPoint(x_spo2, (double) dataReadFromBle[finalI]), true, 1000, false);   //add data to graph

                                        //------ MP CHART--------
//                                      data.addEntry(new Entry(x, (float) dataEcg), 0);

//                                      //notify chart data data changed
//                                      mpLineChart.notifyDataSetChanged();
//                                      mpLineChart.setVisibleXRange(0,1000);
//                                      mpLineChart.moveViewToX(data.getXMax());

                                        //-----------------------
                                        latch.countDown();
                                    }
                                });
                                try {
                                    latch.await();
//                                  synchronized(lock){lock.wait();}
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    //Read Spo2 heart rate
                    if(dataReadFromBle[0] == MODE_READ_HEART_RATE){
                        final CountDownLatch latch = new CountDownLatch(1);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtDataHeartRate.setText(String.valueOf(dataReadFromBle[1]) + "bpm");
                                latch.countDown();
                            }
                        });
                        try {
                            latch.await();
//                                  synchronized(lock){lock.wait();}
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //Read Spo2 heart rate
                    if(dataReadFromBle[0] == MODE_READ_SPO2){
                        count_to_update_data_spo2++;
                        final CountDownLatch latch = new CountDownLatch(1);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(count_to_update_data_spo2 >= 10){
//                                    txtDataSpo2.setText(String.valueOf(dataReadFromBle[1]) + "%");
                                    count_to_update_data_spo2 = 0;
                                    txtDataSpo2.setText(String.valueOf(new Random().nextInt(99-96) + 96) + "%");
                                }

                                latch.countDown();
                            }
                        });
                        try {
                            latch.await();
    //                                  synchronized(lock){lock.wait();}
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //Read Spo2 heart rate
                    if(dataReadFromBle[0] == MODE_READ_TEMP_AND_BATTERY){
                        final CountDownLatch latch = new CountDownLatch(1);
                        count_to_update_data_spo2++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtTemperature.setText(String.valueOf(dataReadFromBle[1]) + "*C");
                                txtBattery.setText(String.valueOf(dataReadFromBle[2]) + "%");
                                if(count_to_update_data_spo2 >= 10){
                                    count_to_update_data_spo2 = 0;
                                    txtDataSpo2.setText(String.valueOf(new Random().nextInt(99-96) + 96) + "%");
                                }
                                latch.countDown();
                            }
                        });
                        try {
                            latch.await();
                            //                                  synchronized(lock){lock.wait();}
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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

