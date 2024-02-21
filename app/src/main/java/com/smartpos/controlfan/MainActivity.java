package com.smartpos.controlfan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.cloudpos.DeviceException;
import com.cloudpos.OperationListener;
import com.cloudpos.OperationResult;
import com.cloudpos.POSTerminal;
import com.cloudpos.sdk.util.Logger;
import com.cloudpos.serialport.SerialPortDevice;
import com.cloudpos.serialport.SerialPortOperationResult;

public class MainActivity extends AppCompatActivity {
    private SerialPortDevice device = null;
    private Context mContext;
    private int timeout = 1000; // ms
    private TextView textView_tips;

    /**
     * 1.Control FAN commands
     * Turn on fan :09 04 00 95 01 6a 0d
     * Turn off fan:09 04 00 95 00 6b 0d
     * 2. firmware version to 0x0E or update
     * <p>
     * fanStatus1 = 0x01;
     * fanStatus2 = 0x6a;
     */
    byte[] startFanCmd = {0x09, 0x04, 0x00, (byte) 0x95, 0x01, 0x6a, 0x0D};
    byte[] stopFanCmd = {0x09, 0x04, 0x00, (byte) 0x95, 0x00, 0x6b, 0x0D};
    byte[] getVersionCmd = {0x09, 0x03, 0x00, (byte) 0x92, (byte) 0x6E, (byte) 0x0D};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mContext = this;
        textView_tips = findViewById(R.id.tips);


    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    textView_tips.setText(msg.obj.toString());
                    break;
            }
        }
    };

    public void getversion(View view) {
        write(getVersionCmd);
        listenForRead();
    }


    public void startFAN(View view) {
        write(startFanCmd);
        listenForRead();
    }

    public void stopFAN(View view) {
        write(stopFanCmd);
        listenForRead();
    }

    public void open() {

        try {
            /*
             *  int ID_USB_SLAVE_SERIAL = 0;
             *  int ID_USB_HOST_SERIAL = 1;
             *  int ID_SERIAL_EXT = 2;
             */
            if (device == null) {
                device = (SerialPortDevice) POSTerminal.getInstance(mContext).getDevice("cloudpos.device.serialport");
            }
            device.open(SerialPortDevice.ID_SERIAL_EXT);
        } catch (DeviceException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] cmd) {
        open();

        try {
            int offset = 0;
            device.write(cmd, offset, cmd.length);
        } catch (DeviceException e) {
            e.printStackTrace();
        }
    }

    public void listenForRead() {
        final byte[] arryData = new byte[16];
        try {
            OperationListener listener = arg0 -> {
                Logger.debug("listenForRead getResultCode = " + arg0.getResultCode() + "");
                byte[] data = new byte[0];
                if (arg0.getResultCode() == OperationResult.SUCCESS) {
                    data = ((SerialPortOperationResult) arg0).getData();
                } else if (arg0.getResultCode() == OperationResult.ERR_TIMEOUT) {
                    data = ((SerialPortOperationResult) arg0).getData();
                } else {
                    Logger.debug("listenForRead getResultCode = " + arg0.getResultCode() + "");
                }
                String bytes2Str = buf2StringCompact(data);
                Logger.debug("listenForRead bytes2Str = " + bytes2Str + "");

                if (bytes2Str.startsWith("09 04 01 95 00")) {
                    handler.obtainMessage(1, "Control FAN success.").sendToTarget();
                } else if (bytes2Str.startsWith("09 06 01 92 00 00 ")) {
                    handler.obtainMessage(1, "getVersion = " + bytes2Str.substring(15, 21)).sendToTarget();
                }

                close();
            };
            device.listenForRead(arryData.length, listener, timeout);
        } catch (DeviceException e) {
            e.printStackTrace();
            close();
        }
    }

    public static String buf2StringCompact(byte[] buf) {
        int i, index;
        StringBuilder sBuf = new StringBuilder();
//        sBuf.append("[");
        for (i = 0; i < buf.length; i++) {
            index = buf[i] < 0 ? buf[i] + 256 : buf[i];
            if (index < 16) {
                sBuf.append("0").append(Integer.toHexString(index));
            } else {
                sBuf.append(Integer.toHexString(index));
            }
            sBuf.append(" ");
        }
        String substring = sBuf.substring(0, sBuf.length() - 1);
//        return (substring + "]").toUpperCase();
        return (substring).toUpperCase();
    }

    public static byte[] subByteArray(byte[] byteArray, int length) {
        byte[] arrySub = new byte[length];
        if (length >= 0) System.arraycopy(byteArray, 0, arrySub, 0, length);
        return arrySub;
    }

    public void close() {
        try {
            if (device != null) {
                device.close();
            }
        } catch (DeviceException e) {
            e.printStackTrace();
        }
    }
}