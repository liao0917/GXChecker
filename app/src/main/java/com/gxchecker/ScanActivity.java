package com.GXChecker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.cognex.dataman.sdk.CameraMode;
import com.cognex.dataman.sdk.ConnectionState;
import com.cognex.dataman.sdk.PreviewOption;
import com.cognex.dataman.sdk.exceptions.CameraPermissionException;
import com.cognex.mobile.barcode.sdk.ReadResult;
import com.cognex.mobile.barcode.sdk.ReadResults;
import com.cognex.mobile.barcode.sdk.ReaderDevice;
import com.GXChecker.Entity.UggViewModel;
import com.manateeworks.MWOverlay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

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
    @BindView(R.id.btn_read)
    Button btnReader;
    @BindView(R.id.btn_help)
    ImageButton btnHelp;
    @BindView(R.id.txt_SN)
    EditText txtSN;
    @BindView(R.id.list_result)
    RecyclerView recyclerView;
    ReaderDevice readerDevice;
    private boolean isScanning=false;
    ArrayList<HashMap<String, String>> scanResults;
    InfoAdapter resultListAdapter;
    boolean availabilityListenerStarted = false;
    private static final int TASK_COMPLETE=1;
    private static final int TASK_FAILED=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        scanResults = new ArrayList<HashMap<String, String>>();
        recyclerView.setHasFixedSize(true);
        resultListAdapter = new InfoAdapter(scanResults);
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("resultText", "NO Result Found");
        item.put("resultType", "");
        scanResults.add(item);
        recyclerView.setAdapter(resultListAdapter);
//        {

//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                View view = super.getView(position, convertView, parent);
//                ((TextView) view.findViewById(android.R.id.text1)).setTextSize(18);
//                ((TextView) view.findViewById(android.R.id.text1)).setTextColor(Color.WHITE);
//                ((TextView) view.findViewById(android.R.id.text2)).setTextColor(Color.LTGRAY);
//                return view;
//            }
//        };
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readerDevice != null) {
                    toggleScanner();
                }
            }
        });
        btnReader.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (txtSN.getText().length()>0) {
                    txtSN.endBatchEdit();
                    txtSN.clearFocus();
                    hideInput();
                    getThread(txtSN.getText().toString()).start();
                }
                else{
                    Toast.makeText(ScanActivity.this, "Please enter SN",Toast.LENGTH_SHORT).show();
                }
//                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/dotcode.png");
//                long fileSize = file.length();
//                if (fileSize > Integer.MAX_VALUE) {
//                    System.out.println("file too big...");
//                    return;
//                }
//                FileInputStream fi = null;
//                try {
//                    fi = new FileInputStream(file);
//                    byte[] buffer = new byte[(int) fileSize];
//                    int offset = 0;
//                    int numRead = 0;
//                    while (offset < buffer.length
//                            && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
//                        offset += numRead;
//                    }
//                    // 确保所有数据均被读取
//                    if (offset != buffer.length) {
//                    }
//                    fi.close();
//                    BarcodeScanner.MWBscanGrayscaleImage(buffer, 100, 100);//dotcode扫描
//                } catch (FileNotFoundException e) {
//                    Toast.makeText(ScanActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
            }
        });
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScanActivity.this, HelpActivity.class));
            }
        });
        recyclerView.setAdapter(resultListAdapter);
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
        Log.e("fff",reader.getConnectionState().name());
        isScanning = false;
        updateUIByConnectionState();
    }

    @Override
    public void onReadResultReceived(ReaderDevice readerDevice, ReadResults readResults) {
        clearResult();
        btnReader.setVisibility(View.VISIBLE);
        if (readResults.getSubResults() != null && readResults.getSubResults().size() > 0) {
            for (ReadResult subResult : readResults.getSubResults()) {
                createResultItem(subResult);
            }
        } else if (readResults.getCount() > 0) {
            ReadResult result = readResults.getResultAt(0);
            if(result.getReadString()==""||result.getReadString()==null)
                createResultItem(readResults.getResultAt(0));
            else
            {
                Log.d("fff",readerDevice.getConnectionState().name());
                final String resultString = result.getReadString();
                Log.d("fff",resultString);
                getThread(resultString).start();
            }
        }

        isScanning = false;
        btnScan.setText("START SCANNING");
        resultListAdapter.notifyDataSetChanged();
    }
    private Thread getThread(String sn)
    {
        Thread The = new Thread(new Runnable() {
            @Override
            public void run() {
                Message message=new Message();
                HttpsURLConnection Conn = null;
                try {
                    URL url = new URL("https://www.gxprintmall.com.cn:8083/api/OrderViews/"+sn);
                    Conn = (HttpsURLConnection) url.openConnection();
                    Conn.setReadTimeout(15000);
                    Conn.setConnectTimeout(15000);
                    Conn.setDoInput(true);
//                            Conn.setDoOutput(true);
                    Conn.setRequestMethod("GET");
                    InputStream is = new BufferedInputStream(Conn.getInputStream());
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader buffereader = new BufferedReader(reader);
                    StringBuffer buffer = new StringBuffer();
                    String temp = null;
                    while ((temp = buffereader.readLine()) != null) {
                        //取水--如果不为空就一直取
                        buffer.append(temp);
                    }
                    buffereader.close();//记得关闭
                    reader.close();
                    is.close();
                    try {
                        UggViewModel OrderView = JSON.parseObject(buffer.toString(), UggViewModel.class);
                        Log.d("MineMsg",OrderView.toString());
                        message.what = TASK_COMPLETE;
                        message.obj = OrderView;
                    }
                    catch (Exception e)
                    {
                        Log.e("MineMsg","00:"+e.getMessage());
                        message.what = TASK_FAILED;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("MineMsg","11:"+e.getMessage());
                    message.what = TASK_FAILED;
                }
                handler.sendMessage(message);
            }
        });
        return The;
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
                PreviewOption.HIGH_RESOLUTION|PreviewOption.HIGH_FRAME_RATE,
                layout);
        readerDevice.setReaderDeviceListener(this);
        connectToReaderDevice();
        updateUIByConnectionState();
    }
    private void toggleScanner() {
        if (isScanning) {
            readerDevice.stopScanning();
            btnReader.setVisibility(View.VISIBLE);
            btnScan.setText("START SCANNING");
        } else {
            readerDevice.startScanning();
            btnReader.setVisibility(View.INVISIBLE);
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
//        readerDevice.getDataManSystem().sendCommand("GET DEVICE.TYPE", new DataManSystem.OnResponseReceivedListener() {
//            @Override
//            public void onResponseReceived(DataManSystem dataManSystem, DmccResponse response) {
//                if (response.getError() == null) {
//                    Log.d("Device type", response.getPayLoad());
//                }
//            }
//        });

//        readerDevice.getDataManSystem().sendCommand("GET DEVICE.FIRMWARE-VER", new DataManSystem.OnResponseReceivedListener() {
//            @Override
//            public void onResponseReceived(DataManSystem dataManSystem, DmccResponse response) {
//                if (response.getError() == null) {
//                    Log.d("Firmware version", response.getPayLoad());
//                }
//            }
//        });

        //---------------------------------------------------------------------------
        // We are going to explicitly turn off image results (although this is the
        // default). The reason is that enabling image results with an MX-1xxx
        // scanner is not recommended unless your application needs the scanned
        // image--otherwise scanning performance can be impacted.
        //---------------------------------------------------------------------------
        readerDevice.enableImage(false);
        readerDevice.enableImageGraphics(false);
        //---------------------------------------------------------------------------
        // Device specific configuration examples
        //---------------------------------------------------------------------------

            //---------------------------------------------------------------------------
            // Phone/tablet
            //---------------------------------------------------------------------------
            // Set the SDK's decoding effort to level 3
        readerDevice.getDataManSystem().sendCommand("SET DECODER.EFFORT 2");
        readerDevice.getDataManSystem().sendCommand("SET FOCUS.FOCUSTIME 2");
        readerDevice.getDataManSystem().sendCommand("SET DECODER.TARGET-DECODING ON");
        readerDevice.getDataManSystem().sendCommand("SET DECODER.CENTERING-WINDOW 50 50 30 20");//设置对焦框大小和位置
        MWOverlay.targetRectLineColor = Color.RED;
//        readerDevice.getDataManSystem().sendCommand("SET DECODER.1D-SYMBOLORIENTATION 1");
//        MWOverlay.targetRectLineWidth = 2;
//        readerDevice.getDataManSystem().sendCommand("SET DECODER.ROI-PERCENT 10 80 10 80");//X W Y H
//        MWOverlay.locationLineColor = Color.BLUE;
//        MWOverlay.locationLineWidth = 6;

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
    private void createResultItem(final ReadResult result) {
        HashMap<String, String> item = new HashMap<String, String>();
        if (result.isGoodRead()) {
            item.put("resultText", result.getReadString());
            ReaderDevice.Symbology sym = result.getSymbology();
            if (sym != null)
            {
                item.put("resultType", result.getSymbology().getName());
            }
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
    ///影藏键盘
    protected void hideInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View v = getWindow().peekDecorView();
        if (null != v) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }}

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            scanResults.clear();
            switch (msg.what)
            {
                case TASK_COMPLETE:
                    UggViewModel model = (UggViewModel)msg.obj;
                    if(model.getUgg()==null)
                    {
                        HashMap<String, String> item = new HashMap<String, String>();
                        item.put("resultText", "NO Result Found");
                        item.put("resultType", "");
                        scanResults.add(item);
                    }
                    else
                    {
                        HashMap<String, String> item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getDeckersPo());
                        item.put("resultType", "DeckersPo");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getPo());
                        item.put("resultType", "Factory Po");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getId());
                        item.put("resultType", "SN");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getSku());
                        item.put("resultType", "Sku");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getShipId());
                        item.put("resultType", "Ship To-ID");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getGender());
                        item.put("resultType", "Gender");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getStyle_Name());
                        item.put("resultType", "Style Name");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getColor());
                        item.put("resultType", "Color");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getSize());
                        item.put("resultType", "Size");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getPo_Total_Qty());
                        item.put("resultType", "Po Total Qty");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getFactory());
                        item.put("resultType", "Factory");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getPo_Create_Date());
                        item.put("resultType", "Po Create Date");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getBuy_Season());
                        item.put("resultType", "Buy Season");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getBuy_Month());
                        item.put("resultType", "Buy Month");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getBrand());
                        item.put("resultType", "Brand");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getLoss());
                        item.put("resultType", "Loss");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", model.getUgg().getRPrint());
                        item.put("resultType", "Actual printing quantity");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", "MADE IN VIETNAM");
                        item.put("resultType", "Original country");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", String.valueOf(model.getUggQueries().size()));
                        item.put("resultType", "Scanned Times");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", "");
                        item.put("resultType", "");
                        scanResults.add(item);
                        item = new HashMap<String, String>();
                        item.put("resultText", "");
                        item.put("resultType", "");
                        scanResults.add(item);
                    }
                    resultListAdapter.notifyDataSetChanged();
                    break;
                case TASK_FAILED:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
}
