package com.oragee.kneemeasure.blt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.oragee.kneemeasure.R;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

/**
 * Created by fanjianfeng on 2017/6/22.
 */

public class BlueDeviceObservable extends Observable<BluetoothDevice> {
    private static final String TAG = "BlueDeviceObservable";
    private Context mContext;
    private String mAddress;
    private BluetoothAdapter mBluetoothAdapter;

    public BlueDeviceObservable(Context context, String address) {
        super();
        mContext = context;
        mAddress = address;
    }

    @Override
    protected void subscribeActual(Observer<? super BluetoothDevice> observer) {
        if (mContext == null) {
            observer.onError(new BlueException("Context cannot be null"));
            return;
        }
        if (TextUtils.isEmpty(mAddress)) {
            observer.onError(new BlueException(mContext.getString(R.string.address_invalid)));
            return;
        }
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            observer.onError(new BlueException(mContext.getString(R.string.not_support_ble)));
            return;
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            observer.onError(new RuntimeException(mContext.getString(R.string.not_support_bluetooth)));
            return;
        }
        // Device scan callback.
        final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                Log.d(TAG, "device: " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
                if (mAddress.equalsIgnoreCase(bluetoothDevice.getAddress())) {
                    scanDevice(false, this);
                    observer.onNext(bluetoothDevice);
                    observer.onComplete();
                }
            }
        };
        scanDevice(true, leScanCallback);

        observer.onSubscribe(new MainThreadDisposable() {
            @Override
            protected void onDispose() {
                scanDevice(false, leScanCallback);
            }
        });
    }

    private void scanDevice(boolean enable, BluetoothAdapter.LeScanCallback leScanCallback) {
        if (enable) {
            mBluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
}
