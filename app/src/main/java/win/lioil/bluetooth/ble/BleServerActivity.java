package win.lioil.bluetooth.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.UUID;

import win.lioil.bluetooth.APP;
import win.lioil.bluetooth.R;
import win.lioil.bluetooth.util.Tools;

/**
 * BLE服务端(从机/外围设备/peripheral)
 * @author LeiHuang
 */
public class BleServerActivity extends Activity {
    public static final UUID UUID_SERVICE = UUID.fromString("0003CDD0-0000-1000-8000-00805F9B0131");
    public static final UUID UUID_CHAR_WRITE = UUID.fromString("0003cdd2-0000-1000-8000-00805f9b0131");
    public static final UUID UUID_CHAR_NOTIFY = UUID.fromString("0003cdd1-0000-1000-8000-00805f9b0131");
    public static final UUID UUID_DESC_NOTITY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //    public static final UUID UUID_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
//    private static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb");
//    private static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//    private static final UUID UUID_CHAR_WRITE = UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb");
//    private UUID UUID_DESC_NOTITY = UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb");
//    private UUID UUID_CHAR_WRITE = UUID.fromString("0000ff2-0000-1000-8000-00805f9b34fb");
    private static final String TAG = BleServerActivity.class.getSimpleName();
    private TextView mTips;
    private BluetoothGattCharacteristic mCharacteristicNotification;
    /**
     * BLE广播
     */
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /**
     * BLE服务端
     */
    private BluetoothGattServer mBluetoothGattServer;

    /**
     * BLE广播Callback
     */
    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            logTv("BLE广播开启成功");
        }

        @Override
        public void onStartFailure(int errorCode) {
            logTv("BLE广播开启失败,错误码:" + errorCode);
        }
    };

    /**
     * BLE服务端Callback
     */
    private final BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (checkPermission()){
                return;
            }
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", device.getName(), device.getAddress(), status, newState));
            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), device));
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.i(TAG, String.format("onServiceAdded:%s,%s", status, service.getUuid()));
            logTv(String.format(status == 0 ? "添加服务[%s]成功" : "添加服务[%s]失败,错误码:" + status, service.getUuid()));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (checkPermission()){
                return;
            }
            Log.i(TAG, String.format("onCharacteristicReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, characteristic.getUuid()));
            // 模拟数据
            String response = "CHAR_" + (int) (Math.random() * 100);
            // 响应客户端
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());
            logTv("客户端读取Characteristic[" + characteristic.getUuid() + "]:\n" + response);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            if (checkPermission()){
                return;
            }
            // 获取客户端发过来的数据
//            String requestStr = new String(requestBytes);
            String requestStr = Tools.Bytes2HexString(requestBytes, requestBytes.length);
            Log.i(TAG, String.format("onCharacteristicWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
                    preparedWrite, responseNeeded, offset, requestStr));
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);// 响应客户端
            logTv("客户端写入Characteristic[" + characteristic.getUuid() + "]:\n" + requestStr);

//            if ("1B2100000000".equals(requestStr)) {
//                // 简单模拟通知客户端Characteristic变化
////            String response = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"; //模拟数据
//                byte[] response = Tools.HexString2Bytes("1B210001002121");// 电池电量模拟数据
//                mCharacteristicNotification.setValue(response);
//                mBluetoothGattServer.notifyCharacteristicChanged(device, mCharacteristicNotification, false);
//                logTv("onCharacteristicWriteRequest 通知客户端改变Characteristic[" + mCharacteristicNotification.getUuid() + "]:\n" + "1B2100012121");
//            }
            if ("1B00210000".equals(requestStr)) {
                writeFile(device);
            }

        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            if (checkPermission()){
                return;
            }
            Log.i(TAG, String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
            String response = "DESC_" + (int) (Math.random() * 100); //模拟数据
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes()); // 响应客户端
            logTv("客户端读取Descriptor[" + descriptor.getUuid() + "]:\n" + response);
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (checkPermission()){
                return;
            }
            // 获取客户端发过来的数据
            String valueStr = Arrays.toString(value);
            Log.i(TAG, String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, descriptor.getUuid(),
                    preparedWrite, responseNeeded, offset, valueStr));
            // 响应客户端
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            logTv("客户端写入Descriptor[" + descriptor.getUuid() + "]:\n" + valueStr);

            // 简单模拟通知客户端Characteristic变化
//            final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//            String response = "CHAR_12345678901234567890123456789012345678901234567890"; //模拟数据
//            characteristic.setValue(response);
//            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
//            logTv("onDescriptorWriteRequest 通知客户端改变Characteristic[" + characteristic.getUuid() + "]:\n" + response);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            if (checkPermission()){
                return;
            }
            Log.i(TAG, String.format("onExecuteWrite:%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, execute));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            if (checkPermission()){
                return;
            }
            Log.i(TAG, String.format("onNotificationSent:%s,%s,%s", device.getName(), device.getAddress(), status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            if (checkPermission()){
                return;
            }
            Log.i(TAG, String.format("onMtuChanged:%s,%s,%s", device.getName(), device.getAddress(), mtu));
        }
    };

    private void write(BluetoothDevice device, byte[] send) {
        if (checkPermission()) {
            return;
        }
        mCharacteristicNotification.setValue(send);
        mBluetoothGattServer.notifyCharacteristicChanged(device, mCharacteristicNotification, false);
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 写入数据成功会回调->onCharacteristicWrite()
    public void writeFile(BluetoothDevice device) {
        try {
            int each = 200;
            int sleep = 80;
            FileInputStream fileInputStream = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/app-debug.apk");
            int length = fileInputStream.available();
            byte[] data = new byte[length];
            fileInputStream.read(data);

            int times = length / each;
            int last = length % each;

            int count = 0;

            for (int i = 0; i < times; i++) {
                byte[] send = new byte[each];
                arrayCopy(send, data, 0, each * i, each);
                write(device, send);
                count += each;
                Thread.sleep(sleep);
            }
            if (last > 0) {
                byte[] send = new byte[last];
                arrayCopy(send, data, 0, each * times, last);
                write(device, send);
                count += last;
                Log.i(TAG, "[writeFile] 已发送: " + count);
                logTv("发送完成:" + count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void arrayCopy(byte[] desBuf, byte[] srcBuf, int desOffset, int srcOffset, int count) {
        for (int i = 0; i < count; i++) {
            desBuf[desOffset + i] = srcBuf[srcOffset + i];
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkPermission()){
            return;
        }
        setContentView(R.layout.activity_bleserver);
        mTips = findViewById(R.id.tv_tips);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // ============启动BLE蓝牙广播(广告) =================================================================================
        //广播设置(必须)
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                // 广播模式: 低功耗,平衡,低延迟
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                // 发射功率级别: 极低,低,中,高
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();
        //广播数据(必须，广播启动就会发送)
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                // 包含蓝牙名称
                .setIncludeDeviceName(true)
                // 包含发射功率级别
                .setIncludeTxPowerLevel(true)
                // 设备厂商数据，自定义
                .addManufacturerData(1, new byte[]{23, 33})
                .build();
        //扫描响应数据(可选，当客户端扫描时才发送)
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                // 设备厂商数据，自定义
                .addManufacturerData(2, new byte[]{66, 66})
                // 服务UUID
                .addServiceUuid(new ParcelUuid(UUID_SERVICE))
                // 服务数据，自定义
//                .addServiceData(new ParcelUuid(UUID_SERVICE), new byte[]{2})
                .build();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);

        // 注意：必须要开启可连接的BLE广播，其它设备才能发现并连接BLE服务端!
        // =============启动BLE蓝牙服务端=====================================================================================
        BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //添加可写characteristic
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHAR_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);
        //添加可读+通知characteristic
        mCharacteristicNotification = new BluetoothGattCharacteristic(UUID_CHAR_NOTIFY, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        mCharacteristicNotification.addDescriptor(new BluetoothGattDescriptor(UUID_DESC_NOTITY, BluetoothGattCharacteristic.PERMISSION_WRITE));
        service.addCharacteristic(mCharacteristicNotification);
        if (bluetoothManager != null) {
            mBluetoothGattServer = bluetoothManager.openGattServer(this, mBluetoothGattServerCallback);
        }
        mBluetoothGattServer.addService(service);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (checkPermission()){
            return;
        }
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.close();
        }
    }

    private void logTv(final String msg) {
        if (isDestroyed()) {
            return;
        }
        runOnUiThread(() -> {
            APP.getInstance().toast(msg, 0);
            mTips.append(msg + "\n\n");
        });
    }

    /**
     * 检查BLE是否满足必需权限
     * @return true 不满足；false 满足
     */
    private boolean checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                APP.getInstance().toast("缺少BLUETOOTH_CONNECT权限", Toast.LENGTH_SHORT);
                return true;
            }
        }
        return false;
    }
}