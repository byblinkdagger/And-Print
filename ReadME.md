> 按照连接方式主要分为USB打印、IP打印、蓝牙打印，打印指令数据传输的本质是一样的，一般都是通用的字节码表示特定的操作（切纸、走纸、换行、鸣笛等等）


#### 1.USB打印
 通过UsbManager扫描本机USB串口连接着的设备，然后请求权限建立连接，一般会在打印Service中动态注册一个广播，在连接成功的情况下进行打印信息的发送。需要注意的是
 * 1.在Manifest中配置权限：
 ```
    <uses-permission android:name="android.hardware.usb.accessory" />
    <uses-feature android:name="android.hardware.usb.host" />
 ```
 * 2.在打印机完成后关闭各项连接，避免引起一些奇怪的错误。
 * 3.USB连接方式传输数据最大是16K左右，所以要打印大图的话是行不通的（可以拆成多张小图或其他方式）
 

#### 2.IP打印
ip打印的话首先要确保android设备和打印机设备在同一网关下，且端口正确（这部分可以使用窗口工具修改DHCP设置），然后使用socket发起连接，和传输数据。
```
    socket = new Socket(ip, port);
    socket.setSoTimeout(5000);
    OutputStream outputStream = socket.getOutputStream();
    outputStream.write(bytes);
    outputStream.flush();
    outputStream.close();
```

#### 3.蓝牙打印
难点在于处理好蓝牙的搜索、断开连接、重新连接这些回调，传输的时候本质都是一样的。