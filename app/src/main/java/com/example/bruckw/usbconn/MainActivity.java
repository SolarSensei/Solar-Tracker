package com.example.bruckw.usbconn;

        import android.content.Context;
        import android.content.Intent;
        import android.hardware.usb.UsbDevice;
        import android.hardware.usb.UsbDeviceConnection;
        import android.hardware.usb.UsbEndpoint;
        import android.hardware.usb.UsbInterface;
        import android.hardware.usb.UsbManager;
        import android.os.AsyncTask;
        import android.os.Handler;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.SeekBar;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.felhr.usbserial.UsbSerialDevice;
        import com.felhr.usbserial.UsbSerialInterface;

        import java.io.UnsupportedEncodingException;
        import java.util.Collections;
        import java.nio.charset.Charset;
        import java.util.HashMap;
        import java.util.Map;


public class MainActivity extends AppCompatActivity {
    //UI
    private static SeekBar tilt_bar;
    private static SeekBar pan_bar;
    private static TextView tilt_text;
    private static TextView pan_text;
    private static TextView debug_text;
    private Button reset;
    private Button scan;

    //USB Serial
    private static UsbDevice device;
    private static UsbInterface intf;
    private static UsbEndpoint endpoint;
    private static UsbManager mUsbManager;
    private static UsbDeviceConnection connection;
    private static UsbSerialDevice serialPort;
    private static byte[] bytes;

    private static int TIMEOUT = 0;
    private static boolean forceClaim = true;
    private static boolean deviceConnected = false;

    //Position and brightness
    private static int xPos;
    private static int yPos;
    private static HashMap<String, Double> brightMap;


    public byte[] newPos;
    public String currPos;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tilt_text = (TextView) findViewById(R.id.textView1);
        pan_text = (TextView) findViewById(R.id.textView2);
        debug_text = (TextView) findViewById(R.id.textView3);

        scan = (Button) findViewById(R.id.scanBtn);
        reset = (Button) findViewById(R.id.resetBtn);
        reset.setOnClickListener(resetClick);
        scan.setOnClickListener(scanClick);

        xPos = -3072;
        yPos = 500;
        brightMap = new HashMap<>();


        establishConn();
        seekbar();
    }

    public void establishConn() {
        device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            deviceConnected = true;
            intf = device.getInterface(0);
            endpoint = intf.getEndpoint(0);
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            connection = mUsbManager.openDevice(device);
            connection.claimInterface(intf, forceClaim);

            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            serialPort.open();
            serialPort.setBaudRate(9600);
            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

            //Set the pan speed
            String panSpeed = "PS" + 4000 + " ";
            bytes = panSpeed.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);

            //Set the tilt speed
            String tiltSpeed = "PS" + 8000 + " ";
            bytes = tiltSpeed.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);
        }
    }

    public void seekbar() {
        tilt_bar = (SeekBar) findViewById(R.id.seekBar1);
        pan_bar = (SeekBar) findViewById(R.id.seekBar2);

        pan_bar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int progress_value;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progress_value = progress - 3072;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        pan_text.setText("Pan Position: " + progress_value);
                        String output = "PP" + progress_value + " ";
                        bytes = output.getBytes(Charset.forName("UTF-8"));
                        serialPort.write(bytes);
                        debug_text.setText(output);

                    }
                }
        );

        tilt_bar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int progress_value;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progress_value = progress - 1800;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        tilt_text.setText("Tilt Position: " + progress_value);
                        String output = "TP" + progress_value + " ";
                        bytes = output.getBytes(Charset.forName("UTF-8"));
                        serialPort.write(bytes);
                        debug_text.setText(output);


                    }
                }
        );
    }

    public void autoClick(View v) {
        brightMap.clear();
        String output = "PP-3072 ";
        bytes = output.getBytes(Charset.forName("UTF-8"));
        serialPort.write(bytes);

        String output1 = "TP500 ";
        bytes = output1.getBytes(Charset.forName("UTF-8"));
        serialPort.write(bytes);

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(getApplicationContext(), Auto.class);
        startActivity(intent);
    }

    private View.OnClickListener resetClick = new View.OnClickListener(){
        public void onClick(View arg0) {
            String output = "r ";
            bytes = output.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);
            debug_text.setText(output);
        }
    };

    private View.OnClickListener scanClick = new View.OnClickListener() {
        public void onClick(View arg0) {
            scan(brightMap);
        }

    };

    //Image Processing Methods
    public static void scan(HashMap<String, Double> values) {
        new ScanTask().execute();

    }

    public static class ScanTask extends AsyncTask<Void, String, Void> {
        String output;
        @Override
        protected Void doInBackground(Void... params) {

            yPos = 500;
            boolean rotate = true;
            while (yPos >= -1300) {
                output = "TP" + yPos + " ";
                bytes = output.getBytes(Charset.forName("UTF-8"));
                serialPort.write(bytes);
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (rotate) {
                    xPos = -3072;
                    while (xPos <= 3072) {
                        output = "PP" + xPos + " ";
                        bytes = output.getBytes(Charset.forName("UTF-8"));
                        serialPort.write(bytes);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String location = String.valueOf(xPos) + "," + String.valueOf(yPos);
                        brightMap.put(location, Auto.getMaxVal());
                        xPos += 768;
                    }
                    rotate = false;
                } else {
                    xPos = 3072;
                    while (xPos >= -3072) {
                        output = "PP" + xPos + " ";
                        bytes = output.getBytes(Charset.forName("UTF-8"));
                        serialPort.write(bytes);

                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String location = String.valueOf(xPos) + "," + String.valueOf(yPos);
                        brightMap.put(location, Auto.getMaxVal());
                        xPos -= 768;
                    }
                    rotate = true;
                }
                if (yPos == -1300) {
                    break;
                } else {
                    yPos -= 900;
                }
            }

            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }


        protected void onProgressUpdate(String... progress) {
            debug_text.setText(progress[0]);
            pan_bar.setProgress(xPos + 3072);
        }

        protected void onPostExecute(Void unused) {
            Map.Entry<String, Double> maxEntry = null;

            for (Map.Entry<String, Double> entry : brightMap.entrySet())
            {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }

            String maxPos = maxEntry.getKey();
            String[] xAndY = maxPos.split(",");
            int xMax = Integer.valueOf(xAndY[0]);
            int yMax = Integer.valueOf(xAndY[1]);

            output = "PP" + xMax + " ";
            bytes = output.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);

            output = "TP" + yMax + " ";
            bytes = output.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);

        }
    }



    public static int getxPos() {
        return xPos;
    }

    public static int getyPos() {
        return yPos;
    }

    public static HashMap<String, Double> getBrightMap() {
        return brightMap;
    }

    public static boolean isDeviceConnected() {
        return deviceConnected;
    }


}