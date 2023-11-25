package win.lioil.bluetooth.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import win.lioil.bluetooth.R;
import win.lioil.bluetooth.db.BtMacDao;
import win.lioil.bluetooth.db.BtMacDaoDao;

/**
 * @author LeiHuang
 */
@SuppressLint("MissingPermission")
public class BleDevAdapter extends RecyclerView.Adapter<BleDevAdapter.ViewHolder> {
    private static final String TAG = BleDevAdapter.class.getSimpleName();
    private final Listener mListener;
    private final List<BleDev> mDevices = new ArrayList<>();
    public volatile boolean isScanning;
    private final Context context;
    private BtMacDaoDao btMacDaoDao;
    private BluetoothLeScanner bluetoothLeScanner;
    /**
     * 扫描Callback
     */
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BleDev dev = new BleDev(result.getDevice(), result);
            // 过滤Name为null的Ble设备
            if (TextUtils.isEmpty(dev.getName()) || !dev.getName().contains("RFID-reader")) {
                return;
            }
            if (!mDevices.contains(dev)) {
                mDevices.add(dev);
                notifyItemInserted(mDevices.size() - 1);
            }
            String address = dev.getDev().getAddress();
            List<BtMacDao> list = btMacDaoDao.queryBuilder().where(BtMacDaoDao.Properties.MacAdr.eq(address)).list();
            if (list.size() == 0) {
                BtMacDao btMacDao = new BtMacDao();
                btMacDao.setMacAdr(address);
                btMacDaoDao.insert(btMacDao);
                mListener.onDevAdded();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed, errorCode=" + errorCode);
        }
    };

    private final CountDownTimer timer = new CountDownTimer(20000, 100) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            //停止扫描
            bluetoothLeScanner.stopScan(mScanCallback);
            isScanning = false;
            mListener.onScanFinish();
        }
    };

    BleDevAdapter(BtMacDaoDao btMacDaoDao, @NonNull Context context, Listener listener) {
        this.btMacDaoDao = btMacDaoDao;
        this.context = context;
        mListener = listener;
        scanBle();
    }

    /**
     * 重新扫描
     */
    public void reScan() {
        int size = mDevices.size();
        mDevices.clear();
        notifyItemRangeRemoved(0, size);
        scanBle();
    }

    /**
     * 扫描BLE蓝牙(不会扫描经典蓝牙)
     */
    private void scanBle() {
        if (isScanning) {
            Log.e(TAG, "Ble scan is running");
            return;
        }
        isScanning = true;
        BluetoothAdapter bluetoothAdapter;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        // Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，而新API帮我们解析成ScanRecord类
        bluetoothLeScanner.startScan(mScanCallback);
        timer.start();
    }

    /**
     * 停止BLE扫描
     */
    public void stopScanBle() {
        if (!isScanning) {
            Log.e(TAG, "Ble scan is not running");
            return;
        }
        timer.cancel();
        //停止扫描
        bluetoothLeScanner.stopScan(mScanCallback);
        isScanning = false;
        mListener.onScanFinish();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dev, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        BleDev dev = mDevices.get(position);
        String name = dev.getName();
        String address = dev.getDev().getAddress();
        holder.name.setText(String.format("%s, %s, Rssi=%s", name, address, dev.scanResult.getRssi()));
        holder.address.setText(String.format("广播数据{%s}", dev.scanResult.getScanRecord()));
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView name;
        final TextView address;

        ViewHolder(final View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            Log.d(TAG, "onClick, getAdapterPosition=" + pos);
            if (pos >= 0 && pos < mDevices.size()) {
                mListener.onItemClick(mDevices.get(pos));
            }
        }
    }

    public interface Listener {
        /**
         * 点击条目回调事件
         *
         * @param dev 被点击的蓝牙设备信息
         */
        void onItemClick(BleDev dev);

        /**
         * BLE扫描完成
         */
        void onScanFinish();

        /**
         * 扫描到dev
         */
        void onDevAdded();
    }

    public static class BleDev {
        private final BluetoothDevice dev;
        private final ScanResult scanResult;

        BleDev(BluetoothDevice device, ScanResult result) {
            dev = device;
            scanResult = result;
        }

        @SuppressLint("MissingPermission")
        public String getName() {
            return dev.getName();
        }

        public BluetoothDevice getDev() {
            return dev;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BleDev bleDev = (BleDev) o;
            return Objects.equals(dev, bleDev.dev);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dev);
        }
    }
}
