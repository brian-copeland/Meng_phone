package local.ultrasonicping;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by brian on 2/18/16.
 */
public class Background extends IntentService {
    boolean contin = true;
    Thread pinger;

    public Background() {
        super("Background");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();
        final MediaPlayer mPlayer = MediaPlayer.create(this, R.raw.short_inaudible);

        pinger = new Thread(new Runnable() {
            public void run() {
                double newTime = System.currentTimeMillis() + 1000;
//                ToneGenerator toneGenerator = new ToneGenerator(0, 50);

                while (contin) {
                    if (System.currentTimeMillis() > newTime) {
//                        toneGenerator.startTone(2, 500);
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.start();
                        newTime += 1000;
                        Log.d("pinger", "can see this?");

                    } else {
                        SystemClock.sleep(100);
                    }
                }
            }
        });
        pinger.start();
    }

    @Override
    public void onDestroy() {
        contin = false;
        pinger.interrupt();

    }

    private final IBinder mBinder = new BackgroundBinder();

    public class BackgroundBinder extends Binder {
        Background getService() {
            // Return this instance of LocalService so clients can call public methods
            return Background.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


}
