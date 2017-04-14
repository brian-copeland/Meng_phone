package local.ultrasonicping;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    Listener lService;
    Background bService;
    Bluetooth cService;

    boolean bBound = false;
    boolean lBound = false;
    boolean cBound = false;
    TextView textView;
    TextView textViewTwo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Intent pingService = new Intent(this, Background.class);
        startService(pingService);
        bindService(pingService, backgroundConnection, Context.BIND_AUTO_CREATE);

        Intent listenerService = new Intent(this, Listener.class);
        startService(listenerService);
        bindService(listenerService, listenerConnection, Context.BIND_AUTO_CREATE);

//        Intent bluetoothService = new Intent(this, Bluetooth.class);
//        startService(bluetoothService);
//        bindService(bluetoothService, bluetoothConnection, Context.BIND_AUTO_CREATE);

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);

        textView = (TextView) findViewById(R.id.outputString);
        textViewTwo = (TextView) findViewById(R.id.outputString2);


        Timer timer = new Timer();
        timer.schedule(new UpdateTimeTask(), 100, 100);

//        Timer timer2 = new Timer();
//        timer2.schedule(new UpdateTimeTask2(), 500, 500);

    }

    public void updateText(){

        long a = lService.getStartLastSignal();
        if (a != 0L) {

            long nano = (a % 1000);
            long micro = (a / 1000) % 1000;
            long milli = (a / 1000000) % 1000;
            long second = (a / 1000000000) % 60;
            long minute = (a / (60000000000L)) % 60;
            long hour = (a / (3600000000000L)) % 24;

            textView.setText("Last signal heard at : " + String.format("%02d:%02d:%02d:%02d:%02d:%d", hour, minute, second, milli,
                    micro, nano));
        }
//        textView.setText("Last signal heard at: " + String.valueOf(SystemClock.elapsedRealtime() /*+
//                SystemClock.elapsedRealtimeNanos()*/));
//        textView.setText(String.valueOf(SystemClock.elapsedRealtimeNanos()));
    }

//    public void updateText(){
//        boolean advertising = cService.isAdvertising;
//
//        if (advertising){
//            textView.setText("");
//        } else {
//            textView.setText("Currently looking for beacons");
//        }
//    }

//    public void updateText2(){
//        boolean connected = cService.connected;
//        Integer rssi = cService.getRssi();
//
//        if (rssi != null){
//            textViewTwo.setText(String.valueOf(rssi));
//        } else {
//            textViewTwo.setText("Haven't found any devices");
//        }
//    }

    class UpdateTimeTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateText();

                }
            });

        }
    }

//    class UpdateTimeTask2 extends TimerTask {
//        public void run() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    updateText2();
//
//                }
//            });
//
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (bBound) {
            unbindService(backgroundConnection);
            bBound = false;
        }
        if (lBound) {
            unbindService(listenerConnection);
            lBound = false;
        }

        if (cBound) {
            unbindService(bluetoothConnection);
            cBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection backgroundConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Background.BackgroundBinder binder = (Background.BackgroundBinder) service;
            bService = binder.getService();
            bBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bBound = false;
        }
    };

    private ServiceConnection listenerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Listener.ListenerBinder binder = (Listener.ListenerBinder) service;
            lService = binder.getService();
            bBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bBound = false;
        }
    };

    private ServiceConnection bluetoothConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Bluetooth.BluetoothBinder binder = (Bluetooth.BluetoothBinder) service;
            cService = binder.getService();
            cBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            cBound = false;
        }
    };



}
