package com.oragee.kneemeasure.blt;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.oragee.kneemeasure.R;
import com.oragee.kneemeasure.util.ByteUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;
import io.reactivex.exceptions.UndeliverableException;

public class BlueWriteObservable extends Observable<BluetoothDevice> {
    private static final String TAG = "BlueWriteObservable";
    private static final String RESULT_SUCCESS = "success";
    private static final long WRITE_DELAY_TIME = 2000;
    private Context mContext;
    private BluetoothDevice mBluetoothDevice;
    private List<String> mData;
    private BlueService mBlueService;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    public BlueWriteObservable(Context context, BluetoothDevice bluetoothDevice, String... data) {
        super();
        mContext = context;
        mBluetoothDevice = bluetoothDevice;
        mData = new ArrayList<>();
        for (String command : data) {
            if (!TextUtils.isEmpty(command)) {
                mData.add(command.trim());
            }
        }
    }

    @Override
    protected void subscribeActual(Observer<? super BluetoothDevice> observer) {
        if (mContext == null) {
            observer.onError(new RuntimeException("context cannot be null"));
            return;
        }
        if (mBluetoothDevice == null) {
            observer.onError(new RuntimeException(mContext.getString(R.string.device_is_null)));
            return;
        }
        final ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                mBlueService = ((BlueService.LocalBinder) service).getService();
                if (!mBlueService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                }
                // Automatically connects to the device upon successful start-up initialization.
                boolean result = mBlueService.connect(mBluetoothDevice.getAddress());
                Log.d(TAG, "Connect request result=" + result);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBlueService = null;
            }
        };

        final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BlueService.ACTION_GATT_CONNECTED.equals(action)) {
                    Log.d(TAG, "Connected");
                } else if (BlueService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    Log.d(TAG, "Disconnected");
//                    observer.onError(new BlueException(mContext.getString(R.string.connect_interrupted)));
                } else if (BlueService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    List<BluetoothGattService> services = mBlueService.getSupportedGattServices();
                    if (services == null || services.isEmpty()) {
                        observer.onError(new BlueException(mContext.getString(R.string.not_found_write_service)));
                        return;
                    }
                    mWriteCharacteristic = getCharacteristic(services, BluetoothGattCharacteristic.PROPERTY_WRITE);
                    mNotifyCharacteristic = getCharacteristic(services, BluetoothGattCharacteristic.PROPERTY_NOTIFY);
                    if (mWriteCharacteristic == null) {
                        observer.onError(new BlueException(mContext.getString(R.string.not_found_write_service)));
                        return;
                    }
                    if (mNotifyCharacteristic == null) {
                        observer.onError(new BlueException(mContext.getString(R.string.not_found_notify_service)));
                        return;
                    }
                    Log.d("luck", "setCharacteristicNotification");
                    mBlueService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    sendNextCommand(observer);
                } else if (BlueService.ACTION_DATA_AVAILABLE.equals(action)) {
                    if (mNotifyCharacteristic == null) {
                        observer.onError(new BlueException(mContext.getString(R.string.not_found_notify_service)));
                        return;
                    }
                    String result = new String(mNotifyCharacteristic.getValue());
//                    Log.d("luck", "mNotifyCharacteristic.getValue() :" + ByteUtil.bytesToHex(mNotifyCharacteristic.getValue()));
//                    mBlueService.readCharacteristic(mNotifyCharacteristic);
//                    if (RESULT_SUCCESS.equalsIgnoreCase(result)) {
//                        sendNextCommand(observer);
//                    } else {
//                        observer.onError(new BlueException(mContext.getString(R.string.write_data_failed)));
//                    }
                }
            }
        };
        mContext.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());

        Intent gattServiceIntent = new Intent(mContext, BlueService.class);
        mContext.bindService(gattServiceIntent, serviceConnection, Service.BIND_AUTO_CREATE);

        observer.onSubscribe(new MainThreadDisposable() {
            @Override
            protected void onDispose() {
                mContext.unregisterReceiver(gattUpdateReceiver);
                mBlueService.disconnect();
                mContext.unbindService(serviceConnection);
            }
        });

    }

    private void sendNextCommand(Observer observer) {
        if (mData == null || mData.isEmpty()) {
            observer.onNext(mBluetoothDevice);
            observer.onComplete();
        } else {
            Log.d("Karel", mData.get(0));
            mWriteCharacteristic.setValue(mData.get(0));
            mBlueService.writeCharacteristic(mWriteCharacteristic);
            mData.remove(0);
        }
    }

    private BluetoothGattCharacteristic getCharacteristic(List<BluetoothGattService> services, int property) {
        BluetoothGattCharacteristic targetCharacteristic = null;
        for (BluetoothGattService service : services) {
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            if (characteristics == null || characteristics.isEmpty()) {
                continue;
            }
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                if ((characteristic.getProperties() & property) > 0) {
                    targetCharacteristic = characteristic;
                    break;
                }
            }
            if (targetCharacteristic != null) {
                break;
            }
        }
        return targetCharacteristic;
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BlueService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BlueService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BlueService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BlueService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
