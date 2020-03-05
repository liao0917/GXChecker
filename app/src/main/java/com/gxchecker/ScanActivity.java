package com.gxchecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cognex.dataman.sdk.CameraMode;
import com.cognex.dataman.sdk.ConnectionState;
import com.cognex.dataman.sdk.DataManDeviceClass;
import com.cognex.dataman.sdk.DataManSystem;
import com.cognex.dataman.sdk.DmccResponse;
import com.cognex.dataman.sdk.PreviewOption;
import com.cognex.dataman.sdk.exceptions.CameraPermissionException;
import com.cognex.mobile.barcode.sdk.ReadResult;
import com.cognex.mobile.barcode.sdk.ReadResults;
import com.cognex.mobile.barcode.sdk.ReaderDevice;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ScanActivity extends AppCompatActivity implements
        ReaderDevice.OnConnectionCompletedListener, ReaderDevice.ReaderDeviceListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    static final int REQUEST_PERMISSION_CODE = 12322;
    @BindView(R.id.ScanWindow)
    ConstraintLayout layout;
    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.list_result)
    ListView listResult;
    ReaderDevice readerDevice;
    private boolean isScanning=false;
    ArrayList<HashMap<String, String>> scanResults;
    SimpleAdapter resultListAdapter;
    boolean availabilityListenerStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        scanResults = new ArrayList<HashMap<String, String>>();
        resultListAdapter = new SimpleAdapter(this, scanResults, android.R.layout.simple_list_item_2, new String[]{"resultText", "resultType"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text1)).setTextSize(18);
                ((TextView) view.findViewById(android.R.id.text1)).setTextColor(Color.WHITE);
                ((TextView) view.findViewById(android.R.id.text2)).setTextColor(Color.LTGRAY);
                return view;
            }
        };
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readerDevice != null) {
                    toggleScanner();
                }
            }
        });
        listResult.setAdapter(resultListAdapter);
        CreateCameraDevice();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (readerDevice != null
                && readerDevice.getAvailability() == ReaderDevice.Availability.AVAILABLE
                && readerDevice.getConnectionState() != ConnectionState.Connecting && readerDevice.getConnectionState() != ConnectionState.Connected) {
            connectToReaderDevice();
        }
    }

    @Override
    public void onConnectionCompleted(ReaderDevice readerDevice, Throwable error) {
        if (error != null) {

            // ask for Camera Permission if necessary
            if (error instanceof CameraPermissionException)
                ActivityCompat.requestPermissions(((ScanActivity) this), new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CODE);

            updateUIByConnectionState();
        }
    }

    @Override
    public void onConnectionStateChanged(ReaderDevice reader) {
        clearResult();
        if (reader.getConnectionState() == ConnectionState.Connected) {
            // We just connected, so now configure the device how we want it
            configureReaderDevice();
        }

        isScanning = false;
        updateUIByConnectionState();
    }

    @Override
    public void onReadResultReceived(ReaderDevice readerDevice, ReadResults readResults) {
        clearResult();

        if (readResults.getSubResults() != null && readResults.getSubResults().size() > 0) {
            for (ReadResult subResult : readResults.getSubResults()) {
                createResultItem(subResult);
            }
        } else if (readResults.getCount() > 0) {
            createResultItem(readResults.getResultAt(0));
        }

        isScanning = false;
        btnScan.setText("START SCANNING");
        resultListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAvailabilityChanged(ReaderDevice reader) {
        if (reader.getAvailability() == ReaderDevice.Availability.AVAILABLE) {
            connectToReaderDevice();
        } else if (reader.getAvailability() == ReaderDevice.Availability.UNAVAILABLE) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert
                    .setTitle("Device became unavailable")
                    .setPositiveButton("OK", null)
                    .create()
                    .show();
        }
    }

    private void CreateCameraDevice()
    {
        if (availabilityListenerStarted) {
            readerDevice.stopAvailabilityListening();

            availabilityListenerStarted = false;
        }

        if (readerDevice != null) {
            readerDevice.disconnect();

            readerDevice = null;
        }
        this.readerDevice = ReaderDevice.getPhoneCameraDevice(
                this,
                CameraMode.NO_AIMER,
                PreviewOption.DEFAULTS,
                layout);
        readerDevice.setReaderDeviceListener(this);
        connectToReaderDevice();
        updateUIByConnectionState();
    }
    private void toggleScanner() {
        if (isScanning) {
            readerDevice.stopScanning();
            btnScan.setText("START SCANNING");
        } else {
            readerDevice.startScanning();
            btnScan.setText("STOP SCANNING");
        }

        isScanning = !isScanning;
    }
    private void connectToReaderDevice() {
        readerDevice.connect(ScanActivity.this);
        Log.e("Fff",readerDevice.getConnectionState().name());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (readerDevice != null &&
                readerDevice.getConnectionState() == ConnectionState.Connected) {
            readerDevice.stopScanning();
        }
    }
    private void configureReaderDevice() {
        readerDevice.setSymbologyEnabled(ReaderDevice.Symbology.DOTCODE, true, new ReaderDevice.OnSymbologyListener() {
            @Override
            public void onSymbologyEnabled(ReaderDevice reader, ReaderDevice.Symbology symbology, Boolean enabled, Throwable error) {
                if (error != null)
                    Log.e(this.getClass().getSimpleName(), "Failed to enable DotCode");
            }
        });
        //---------------------------------------------------------------------------
        // Below are examples of sending DMCC commands and getting the response
        //---------------------------------------------------------------------------
        readerDevice.getDataManSystem().sendCommand("GET DEVICE.TYPE", new DataManSystem.OnResponseReceivedListener() {
            @Override
            public void onResponseReceived(DataManSystem dataManSystem, DmccResponse response) {
                if (response.getError() == null) {
                    Log.d("Device type", response.getPayLoad());
                }
            }
        });

        readerDevice.getDataManSystem().sendCommand("GET DEVICE.FIRMWARE-VER", new DataManSystem.OnResponseReceivedListener() {
            @Override
            public void onResponseReceived(DataManSystem dataManSystem, DmccResponse response) {
                if (response.getError() == null) {
                    Log.d("Firmware version", response.getPayLoad());
                }
            }
        });

        //---------------------------------------------------------------------------
        // We are going to explicitly turn off image results (although this is the
        // default). The reason is that enabling image results with an MX-1xxx
        // scanner is not recommended unless your application needs the scanned
        // image--otherwise scanning performance can be impacted.
        //---------------------------------------------------------------------------
        readerDevice.enableImage(true);
        readerDevice.enableImageGraphics(true);
        //---------------------------------------------------------------------------
        // Device specific configuration examples
        //---------------------------------------------------------------------------

            //---------------------------------------------------------------------------
            // Phone/tablet
            //---------------------------------------------------------------------------
            // Set the SDK's decoding effort to level 3
        readerDevice.getDataManSystem().sendCommand("SET DECODER.EFFORT 3");

    }

    private void clearResult() {
        scanResults.clear();
        resultListAdapter.notifyDataSetChanged();
    }
    private void updateUIByConnectionState() {
        if (readerDevice != null && readerDevice.getConnectionState() == ConnectionState.Connected) {
//            tvConnectionStatus.setText("Connected");
//            tvConnectionStatus.setBackgroundResource(R.drawable.connection_status_bg);

            btnScan.setEnabled(true);
        } else {
//            tvConnectionStatus.setText("Disconnected");
//            tvConnectionStatus.setBackgroundResource(R.drawable.connection_status_bg_disconnected);

            btnScan.setEnabled(false);
        }

//        btnScan.setText(btnScan.isEnabled() ? "START SCANNING" : "(NOT CONNECTED)");
    }
    private void createResultItem(ReadResult result) {
        HashMap<String, String> item = new HashMap<String, String>();
        if (result.isGoodRead()) {
            item.put("resultText", result.getReadString());

            ReaderDevice.Symbology sym = result.getSymbology();
            if (sym != null)
                item.put("resultType", result.getSymbology().getName());
            else
                item.put("resultType", "UNKNOWN SYMBOLOGY");
        } else {
            item.put("resultText", "NO READ");
            item.put("resultType", "");
        }

        scanResults.add(item);
    }
    //Camera Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Check result from permission request. If it is allowed by the user, connect to readerDevice
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (readerDevice != null && readerDevice.getConnectionState() != ConnectionState.Connected)
                    readerDevice.connect(ScanActivity.this);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(((ScanActivity) this), Manifest.permission.CAMERA)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setMessage("You need to allow access to the Camera")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface,
                                        int i) {
                                    ActivityCompat.requestPermissions(ScanActivity.this, new String[]{Manifest.permission.CAMERA},
                                            REQUEST_PERMISSION_CODE);
                                }
                            })
                            .setNegativeButton("Cancel", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
    }
}
