package local.ultrasonicping;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by brian on 3/2/16.
 */
public class Bluetooth extends IntentService {

    public Bluetooth() {
        super("Bluetooth");
    }


    private boolean mScanning = false;
    private Handler mHandler;
    List<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    final UUID bluetoothId = UUID.fromString("892fda79-6511-4077-be9f-a973d586ce5f");
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothSocket socket = null;
    private BluetoothGatt mBluetoothGatt;
    Thread switcher = null;
    BluetoothLeAdvertiser advertiser = null;
    AdvertiseData advertiseData = new AdvertiseData.Builder().build();
    boolean isAdvertising = false;
    boolean connected = false;
    Integer device_rssi = null;
    boolean updated = false;

    private AdvertiseCallback callback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode){}
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect){}
    };

    public void sendSignal(){
        if (mBluetoothGatt != null && mBluetoothGatt.getDevice() != null){
            BluetoothDevice connectedDevice = mBluetoothGatt.getDevice();
            
        }
    }

//    public Integer getRssi(){
//        if (device_rssi != null){
//            updated = false;
//            return device_rssi;
//        } else {
//            return null;
//        }
//    }


    protected void onHandleIntent(Intent workIntent){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler();

        switcher = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    isAdvertising = false;
                    int randomTime = ((int) (Math.random()*10000)) + 2000;
                    scanLeDevice(true, randomTime);
                    SystemClock.sleep(randomTime);
//                    long timeToStop = SystemClock.currentThreadTimeMillis() + randomTime;
                    //Then become beacon
                    isAdvertising = true;
                    randomTime = ((int) (Math.random()*10000)) + 2000;

                    advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    AdvertiseSettings settings = new AdvertiseSettings.Builder().build();
                    advertiser.startAdvertising(settings, advertiseData, callback );
                    SystemClock.sleep(randomTime);
                }
            }
        });
        switcher.start();
    }

    private void scanLeDevice(final boolean enable, int SCAN_PERIOD) {
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.cancelDiscovery();
//                    scanner.stopScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

//            scanner.startScan(new ArrayList<ScanFilter>(),
//                            new ScanSettings.Builder().build(), mLeScanCallback);

            mBluetoothAdapter.startDiscovery();

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        } else {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
//                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                Log.d("bluetooth", device.getName());
                if (device.getName() != null && device.getName().contains("bcope")) {
                    Log.d("ultrasonic_bluetooth ", device.getName() + "\n" + device.getAddress());
                    device_rssi = Integer.valueOf(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));
                    updated = true;
//                    macAddress = device.getAddress();
                    try {
                        bluetoothDevices.add(device);
                        Log.d("ultrasonic_bluetooth","found device: " + device.getName());

                        startConnection(device);
                        connected = true;

//                        socket = device.createRfcommSocketToServiceRecord(java.util.UUID.randomUUID());
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

        }
    };

//    private LeDeviceListAdapter mLeDeviceListAdapter;

    // Device scan callback.

    private ScanCallback mLeScanCallback =
            new ScanCallback() {

                public void onScanResult(int callbackType, ScanResult result){
                    BluetoothDevice device = result.getDevice();
                    if (device.getName() != null && device.getName().contains("bcope")) {
                        Log.d("ultrasonic_bluetooth ", device.getName() + "\n" + device.getAddress());
//                    macAddress = device.getAddress();
                        try {
                            bluetoothDevices.add(device);
                            Log.d("ultrasonic_bluetooth", "found device: " + device.getName());

                            startConnection(device);
                            connected = true;

//                        socket = device.createRfcommSocketToServiceRecord(java.util.UUID.randomUUID());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            };
//

    private void startConnection(BluetoothDevice device){
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        device.createBond();
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {

            };


//
//    String macAddress = null;
//    BluetoothSocket socket = null;
//    Thread server = null;
//
//    List<String> mArrayAdapter = new ArrayList<String>();
//

//
//    protected void onHandleIntent(Intent workIntent) {
//        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (!mBluetoothAdapter.isEnabled()) {
//            //This is not good, lets hardcode around this for now
////            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
////            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            Log.d("bluetooth", "Bluetooth is not enabled");
//        }
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//        // If there are paired devices
//        server = new Thread(new Runnable() {
//            public void run() {
//                try {
//                    BluetoothServerSocket serverSocket =
//                            mBluetoothAdapter.listenUsingRfcommWithServiceRecord("server connection", bluetoothId);
//                    BluetoothSocket temp = serverSocket.accept();
//                    if (socket ==null){
//                        socket = temp;
//                    }
//                    serverSocket.close();
//                } catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        });
//        server.start();
//
//        if (pairedDevices.size() > 0) {
//            // Loop through paired devices
//            for (BluetoothDevice device : pairedDevices) {
//                // Add the name and address to an array adapter to show in a ListView
//                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                if (device.getName().contains("bcope") && socket == null) {
//                    Log.d("bluetooth ", device.getName() + "\n" + device.getAddress());
//                    //Also want to pair here
//                    macAddress = device.getAddress();
//                    try {
//                        socket = device.createRfcommSocketToServiceRecord(bluetoothId);
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//        mBluetoothAdapter.startDiscovery();
//        // Register the BroadcastReceiver
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
//
//
//    }
//
//    // Create a BroadcastReceiver for ACTION_FOUND
//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            // When discovery finds a device
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Get the BluetoothDevice object from the Intent
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                // Add the name and address to an array adapter to show in a ListView
////                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
////                Log.d("bluetooth", device.getName());
//                if (device.getName() != null && device.getName().contains("bcope") && socket == null) {
//                    Log.d("bluetooth ", device.getName() + "\n" + device.getAddress());
//                    macAddress = device.getAddress();
//                    try {
//                        socket = device.createRfcommSocketToServiceRecord(java.util.UUID.randomUUID());
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//        }
//    };

    private final IBinder mBinder = new BluetoothBinder();

    public class BluetoothBinder extends Binder {
        Bluetooth getService() {
            // Return this instance of LocalService so clients can call public methods
            return Bluetooth.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
