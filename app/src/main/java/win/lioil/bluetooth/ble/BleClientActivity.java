package win.lioil.bluetooth.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import win.lioil.bluetooth.APP;
import win.lioil.bluetooth.R;
import win.lioil.bluetooth.databinding.ActivityBleclientBinding;
import win.lioil.bluetooth.db.BtMacDao;
import win.lioil.bluetooth.db.BtMacDaoDao;
import win.lioil.bluetooth.db.DBManager;
import win.lioil.bluetooth.db.DaoSession;
import win.lioil.bluetooth.util.CodeUtil;
import win.lioil.bluetooth.util.ExcelUtil;
import win.lioil.bluetooth.util.Tools;

/**
 * BLE客户端(主机/中心设备/Central)
 *
 * @author LeiHuang
 */
@SuppressLint("MissingPermission")
public class BleClientActivity extends Activity {
    private static final String TAG = BleClientActivity.class.getSimpleName();

    private UUID UUID_CHAR_READ = null;
    private UUID UUID_CHAR_NOTIFY = null;
    private UUID UUID_DESC_NOTITY = null;
    private UUID UUID_CHAR_WRITE = null;
    private UUID selectedServiceUuid = null;

    private BleDevAdapter mBleDevAdapter;
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;

    // 与服务端连接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            String devInfo = dev.getName() + "," + dev.getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices(); //启动服务发现
                runOnUiThread(() -> {
                    showOperateLayout();
                    logTv(String.format("与[%s]连接成功", devInfo));
                });
            } else {
                isConnected = false;
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                runOnUiThread(() -> {
                    showConnectLayout();
                    resetUUID();
                    logTv(String.format(status == 0 ? "与[%s]连接断开" : "与[%s]连接出错,错误码:" + status, devInfo));
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered");
            String address = gatt.getDevice().getAddress();
            String name = gatt.getDevice().getName();
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", name, address, status));
//            boolean requestMtu = gatt.requestMtu(240);
//            Log.i(TAG, "onServicesDiscovered requestMtu: " + requestMtu);
            if (status == BluetoothGatt.GATT_SUCCESS) { //BLE服务发现成功
                selectServiceUuid(gatt.getServices());
                // 遍历获取BLE服务Services/Characteristics/Descriptors的全部UUID
//                for (BluetoothGattService service : gatt.getServices()) {
//                    // Returns the UUID of this service
//                    String uuidService = service.getUuid().toString();
//                    Log.i(TAG, "[onServicesDiscovered] uuidService: " + uuidService);
//                    // 只获取指定的uuid的service下的characteristics
//                    if (!uuidService.equals(selectedServiceUuid.toString())) {
//                        continue;
//                    }
//                    StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + uuidService);
//                    // Returns a list of characteristics included in this service.
//                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
//                        // Returns the UUID of this characteristic
//                        UUID characteristicUuid = characteristic.getUuid();
//                        Log.i(TAG, "[onServicesDiscovered] characteristicUuid: " + characteristicUuid);
//                        allUUIDs.append(",\nC=").append(characteristicUuid);
//                        int properties = characteristic.getProperties();
//                        // Returns a list of descriptors for this characteristic.
//                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors())
//                        // Returns the UUID of this descriptor.
//                        {
//                            UUID descriptorUuid = descriptor.getUuid();
//                            Log.i(TAG, "[onServicesDiscovered] descriptorUuid: " + descriptorUuid);
//                            allUUIDs.append(",\nD=").append(descriptorUuid);
//                            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                                UUID_DESC_NOTITY = descriptorUuid;
//                            }
//                        }
//                        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            UUID_CHAR_READ = characteristicUuid;
//                        }
//                        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//
//                            UUID_CHAR_NOTIFY = characteristicUuid;
//                            SystemClock.sleep(200);
//                            setNotify(null);
//                        }
//                        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
//                            UUID_CHAR_WRITE = characteristicUuid;
//                        }
//                    }
//                    allUUIDs.append("}");
//                    Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());
//                    logTv("发现服务" + allUUIDs);
//
////                    SystemClock.sleep(200);
////                    setNotify(null);
//                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
//            String valueStr = new String(characteristic.getValue());
            String valueStr = Tools.Bytes2HexString(characteristic.getValue(), characteristic.getValue().length);
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("读取Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
//            String valueStr = new String(characteristic.getValue());
            String valueStr = Tools.Bytes2HexString(characteristic.getValue(), characteristic.getValue().length);
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("写入Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
//            String valueStr = new String(characteristic.getValue());
            String valueStr = Tools.Bytes2HexString(characteristic.getValue(), characteristic.getValue().length);
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));
            logTv("通知Characteristic[" + uuid + "]:\n" + valueStr);

//            try {
//                mRandomAccessFile.seek(mRandomAccessFile.length());
//                mRandomAccessFile.write(characteristic.getValue());
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("读取Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("写入Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            logTv("收到MTU改变" + mtu);
        }
    };

    private win.lioil.bluetooth.databinding.ActivityBleclientBinding binding;
    private BtMacDaoDao btMacDaoDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBleclientBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        showConnectLayout();

        DaoSession daoSession = DBManager.getInstance(BleClientActivity.this).getWriteDaoSession();
        btMacDaoDao = daoSession.getBtMacDaoDao();
        QueryBuilder<BtMacDao> qb = btMacDaoDao.queryBuilder();
        List<BtMacDao> list = qb.list();
        int size = list.size();
        binding.textView.setText(String.valueOf(size));

        binding.rvBle.setLayoutManager(new LinearLayoutManager(this));
        mBleDevAdapter = new BleDevAdapter(btMacDaoDao, this, new BleDevAdapter.Listener() {
            @Override
            public void onItemClick(BleDevAdapter.BleDev dev) {
//                mBleDevAdapter.stopScanBle();
//                closeConn();
//                // 连接蓝牙设备
//                mBluetoothGatt = dev.getDev().connectGatt(BleClientActivity.this, false, mBluetoothGattCallback);
//                APP.getInstance().toast(String.format("与[%s]开始连接............", dev.getDev()), 0);
//                Bitmap bitmap = CodeUtil.creatBarcode(
//                        BleClientActivity.this, dev.getDev().getAddress(), 1000,
//                        160, new PointF(0, 180), true);
//                ImageView imageView = new ImageView(BleClientActivity.this);
//                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
//                imageView.setLayoutParams(params);
//                imageView.setImageBitmap(bitmap);
//                AlertDialog alertDialog = new AlertDialog.Builder(BleClientActivity.this).create();
//                Window window = alertDialog.getWindow();
//                if (window != null) {
//                    window.getDecorView().setPadding(0, 0, 0, 0);
//                    WindowManager.LayoutParams layoutParams = window.getAttributes();
//                    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
//                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                    window.setAttributes(layoutParams);
//                    window.getDecorView().setBackgroundColor(Color.WHITE);
//                }
//                alertDialog.setView(imageView);
//                alertDialog.show();
            }

            @Override
            public void onScanFinish() {
                APP.getInstance().toast(getString(R.string.scan_finish), 0);
                binding.btnScan.setText(getString(R.string.reScan));
            }

            @Override
            public void onDevAdded() {
                QueryBuilder<BtMacDao> qb = btMacDaoDao.queryBuilder();
                List<BtMacDao> list = qb.list();
                int size = list.size();
                binding.textView.setText(String.valueOf(size));
            }
        });
        binding.btnScan.setText(getString(R.string.scanning));

        binding.rvBle.setAdapter(mBleDevAdapter);
        binding.tvTips.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleDevAdapter.stopScanBle();
        closeConn();
    }

    // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133
    private void closeConn() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    // 扫描BLE
    public void reScan(View view) {
        if (mBleDevAdapter.isScanning) {
//            APP.getInstance().toast("正在扫描...", 0);
            mBleDevAdapter.stopScanBle();
        } else {
            binding.btnScan.setText(getString(R.string.scanning));
            mBleDevAdapter.reScan();
        }
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 读取数据成功会回调->onCharacteristicChanged()
    public void read(View view) {
        if (UUID_CHAR_READ == null) {
            APP.getInstance().toast("没有选择读取Characteristic的UUID", 0);
            return;
        }
        BluetoothGattService service = getGattService(selectedServiceUuid);
        if (service != null) {
            //通过UUID获取可读的Characteristic
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_READ);
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 写入数据成功会回调->onCharacteristicWrite()
    public void write(View view) {
        if (UUID_CHAR_WRITE == null) {
            APP.getInstance().toast("没有选择写入Characteristic的UUID", 0);
            return;
        }
        BluetoothGattService service = getGattService(selectedServiceUuid);
        if (service != null) {
            String text = binding.etWrite.getText().toString();
//            String text = "1B1A000800BB0003000100047E00";
            byte[] bytes = Tools.HexString2Bytes(text.trim());
            //通过UUID获取可写的Characteristic
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);
            //单次最多20个字节
            characteristic.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    // 设置通知Characteristic变化会回调->onCharacteristicChanged()
    public void setNotify(View view) {
        if (UUID_CHAR_NOTIFY == null) {
            APP.getInstance().toast("没有选择通知Characteristic的UUID", 0);
            return;
        }
        if (UUID_DESC_NOTITY == null) {
            APP.getInstance().toast("没有选择通知Descriptor的UUID", 0);
            return;
        }
        BluetoothGattService service = getGattService(selectedServiceUuid);
        if (service != null) {
            // 设置Characteristic通知，通过UUID获取可通知的Characteristic
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_NOTIFY);
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);

            // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESC_NOTITY);
            // 和通知类似,但服务端不主动发数据,只指示客户端读取数据
            // descriptor$setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    // 获取Gatt服务
    private BluetoothGattService getGattService(UUID uuid) {
        if (!isConnected) {
            APP.getInstance().toast("没有连接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null) {
            APP.getInstance().toast("没有找到服务UUID=" + uuid, 0);
        }
        return service;
    }

    // 输出日志
    private void logTv(final String msg) {
        if (isDestroyed()) {
            return;
        }
        runOnUiThread(() -> {
            APP.getInstance().toast(msg, 0);
            binding.tvTips.append(msg + "\n\n");
            Layout layout = binding.tvTips.getLayout();
            int desired;
            if (layout == null) {
                desired = 0;
            } else {
                desired = layout.getLineTop(binding.tvTips.getLineCount());
            }
            int padding = binding.tvTips.getCompoundPaddingTop() + binding.tvTips.getCompoundPaddingBottom();
            int offset = desired + padding;
            if (offset > binding.tvTips.getHeight() && binding.tvTips.getHeight() > 0) {
                binding.tvTips.scrollTo(0, offset - binding.tvTips.getHeight());
            }
        });
    }

    /**
     * 检查BLE是否满足必需权限
     *
     * @return true 不满足；false 满足
     */
    private void checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                APP.getInstance().toast("缺少BLUETOOTH_CONNECT权限", Toast.LENGTH_SHORT);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 12580);
            }
        }
    }

    private void selectServiceUuid(List<BluetoothGattService> services) {
        String[] serviceUuids = new String[services.size()];
        for (int i = 0; i < services.size(); i++) {
            serviceUuids[i] = services.get(i).getUuid().toString();
        }
        runOnUiThread(() -> new AlertDialog.Builder(BleClientActivity.this, R.style.MyDialogTheme)
                .setTitle("选择Service UUID")
                .setSingleChoiceItems(serviceUuids, 0, (dialog, which) -> {
                    selectedServiceUuid = UUID.fromString(serviceUuids[which]);
                    logTv("选择Service UUID:" + serviceUuids[which]);
                    selectReadUuid(services.get(which));
                    dialog.dismiss();
                })
                .show());
    }

    private void selectReadUuid(BluetoothGattService service) {
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        String[] characteristicUuids = new String[characteristics.size()];
        for (int i = 0; i < characteristics.size(); i++) {
            characteristicUuids[i] = characteristics.get(i).getUuid().toString();
        }
        runOnUiThread(() -> new AlertDialog.Builder(BleClientActivity.this, R.style.MyDialogTheme)
                .setTitle("选择 通知 CharacteristicUUID")
                .setSingleChoiceItems(characteristicUuids, 0, (dialog, which) -> {
                    UUID_CHAR_NOTIFY = UUID.fromString(characteristicUuids[which]);
                    logTv("选择通知CharacteristicUUID:" + characteristicUuids[which]);
                    for (BluetoothGattDescriptor descriptor : characteristics.get(which).getDescriptors()) {
                        UUID descriptorUuid = descriptor.getUuid();
                        UUID_DESC_NOTITY = descriptorUuid;
                        logTv("选择通知DescriptorUUID:" + descriptorUuid.toString());
                        break;
                    }
                    setNotify(null);
                    selectWriteUuid(service);
                    dialog.dismiss();
                })
                .show());
    }

    private void selectWriteUuid(BluetoothGattService service) {
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        String[] characteristicUuids = new String[characteristics.size()];
        for (int i = 0; i < characteristics.size(); i++) {
            characteristicUuids[i] = characteristics.get(i).getUuid().toString();
        }
        runOnUiThread(() -> new AlertDialog.Builder(BleClientActivity.this, R.style.MyDialogTheme)
                .setTitle("选择 写入 CharacteristicUUID")
                .setSingleChoiceItems(characteristicUuids, 0, (dialog, which) -> {
                    UUID_CHAR_WRITE = UUID.fromString(characteristicUuids[which]);
                    logTv("选择写入CharacteristicUUID:" + characteristicUuids[which]);
                    dialog.dismiss();
                })
                .show());
    }

    public void disconnect(View view) {
        mBluetoothGatt.disconnect();
    }

    private void showConnectLayout() {
        binding.btnDisconnect.setVisibility(View.GONE);
        binding.btnNotify.setVisibility(View.GONE);
        binding.etWrite.setVisibility(View.GONE);
        binding.btnWrite.setVisibility(View.GONE);
        binding.tvTips.setVisibility(View.GONE);
        binding.btnScan.setVisibility(View.VISIBLE);
        binding.rvBle.setVisibility(View.VISIBLE);
    }

    private void showOperateLayout() {
        binding.btnDisconnect.setVisibility(View.VISIBLE);
        binding.btnNotify.setVisibility(View.VISIBLE);
        binding.etWrite.setVisibility(View.VISIBLE);
        binding.btnWrite.setVisibility(View.VISIBLE);
        binding.tvTips.setVisibility(View.VISIBLE);
        binding.btnScan.setVisibility(View.GONE);
        binding.rvBle.setVisibility(View.GONE);
    }

    private void resetUUID() {
        UUID_CHAR_READ = null;
        UUID_CHAR_NOTIFY = null;
        UUID_DESC_NOTITY = null;
        UUID_CHAR_WRITE = null;
        selectedServiceUuid = null;
    }
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());

    public void ex(View view) {
        binding.btnScan.performClick();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String filePath = Environment.getExternalStorageDirectory() + "/Download/";
                    String fileName = "Tag_" + dateFormat.format(new Date()) + ".xls";
                    String[] title = {"Mac"};
                    ExcelUtil.initExcel(filePath, fileName, title);
                    QueryBuilder<BtMacDao> qb = btMacDaoDao.queryBuilder();
                    List<BtMacDao> list = qb.list();
                    ExcelUtil.writeObjListToExcel(getRecordData(list), filePath + fileName);
                    notifySystemToScan(filePath + fileName);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            APP.getInstance().toast("导出完成", Toast.LENGTH_SHORT);
                        }
                    });
                    btMacDaoDao.deleteAll();
                    qb = btMacDaoDao.queryBuilder();
                    list = qb.list();
                    int size = list.size();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.textView.setText(String.valueOf(size));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            APP.getInstance().toast("导出失败\n" + e.getMessage(), Toast.LENGTH_SHORT);
                        }
                    });
                }
            }
        }).start();
    }

    private ArrayList<ArrayList<String>> getRecordData(List<BtMacDao> infos) {
        ArrayList<ArrayList<String>> recordList = new ArrayList<>();
        for (int i = 0; i < infos.size(); i++) {
            ArrayList<String> beanList = new ArrayList<>();
            BtMacDao info = infos.get(i);
            beanList.add(info.getMacAdr());
            recordList.add(beanList);
        }
        return recordList;
    }

    public void notifySystemToScan(String filePath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        this.sendBroadcast(intent);
    }
}