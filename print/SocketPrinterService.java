package com.oragee.posclient.print;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.oragee.posclient.app.App;
import com.oragee.posclient.app.AppConstant;
import com.oragee.posclient.util.BitmapUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;

public class SocketPrinterService extends IntentService {

    private final static String TAG = "SocketPrinterService";
    private Context mContext;
    private String printInfo;
    Socket socket = null;

    public SocketPrinterService() {
        super("SocketPrinterService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        printInfo = intent.getStringExtra("print_info");
        //小票打印机是ESC指令
        EscCommand esc = new EscCommand();
        esc.addText(printInfo); // 打印文字
        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        try {
//            SendMsgCommand(AppConstant.printIP,AppConstant.printPort,bytes);
            boolean b = SendMsgCommand("192.168.8.211", AppConstant.printPort, bytes);
            Log.d(TAG,"SendMsgCommand "+b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean SendMsgCommand(final String ip, final int port, byte[] bytes)
            throws UnknownHostException, IOException {
        try {
            Log.d(TAG,"socket  doing");
            socket = new Socket(ip, port);
            socket.setSoTimeout(5000);
            OutputStream outputStream = socket.getOutputStream();
            //这边是输入指令根据自己的需求进行输入
            Log.d(TAG,"bytes :"+bytes);
            Log.d(TAG,"bytes :"+bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
            Log.d(TAG,"socket  done");
            socket.close();
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            if (socket != null) {
                socket.close();
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            if (socket != null) {
                socket.close();
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
