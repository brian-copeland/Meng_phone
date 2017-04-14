package local.ultrasonicping;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by brian on 2/25/16.
 */
public class Listener extends IntentService {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    boolean contin = true;
    Thread listener;
    AudioRecord ar;
    boolean inSignal = false;
    Long startLastSignal = (long) 0;
    double weightedAverage = 5;
    long initialTime = 0;
    boolean updated = false;
    private MediaRecorder mRecorder = null;
//    FileOutputStream unfilteredOutputStream;
//    FileOutputStream filteredOutputStream;
    WaveWriter

    public Long getStartLastSignal(){
        if (updated) {
            updated = false;
            return startLastSignal;
        } else {
            return 0L;
        }

    }

    public Listener() {
        super("Listener");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
//        File unfiltered = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "unfiltered");
        final File filtered = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "filtered");
//        filtered.getAbsolutePath();
        Log.d("file_stuffs", filtered.getAbsolutePath());

        if (filtered.exists())
            filtered.delete();
        try {
            filtered.createNewFile();
        } catch (Exception e){
            Log.e("recording creating file", e.getMessage());
            e.printStackTrace();
        }


        final long time = SystemClock.currentThreadTimeMillis();

        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        Pair<AudioRecord, Integer> pair = findAudioRecord();
        ar = pair.first;

        final Integer readSize = pair.second;
        Log.d("fft", String.valueOf(readSize));


        listener = new Thread(new Runnable() {
            public void run() {

                byte[] buffer = new byte[readSize];
                ar.startRecording();
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(filtered.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                while (contin) {
                    ar.read(buffer, 0, readSize);

                    if (SystemClock.currentThreadTimeMillis() < time + 10000){
//                        byte[] data = new byte[2*buffer.length];
//                        for (int j = 0; j < buffer.length; j += 1){
//                            data[2*j] = (byte)(buffer[j] & 0xff);
//                            data[2*j + 1] = (byte)(buffer[j] >> 8 & 0xff);
//                        }
                        try {
                            os.write(buffer, 0, buffer.length);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            os.close();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    //First we need to filter the results
                    int filterTimes = 1;
                    int frequency = 15000;
                    double[] transformed = new double[buffer.length];

                    for (int j=0;j<buffer.length;j++) {
                        transformed[j] = (double)buffer[j];
                    }
                    double[] filtered = filterResults(transformed, frequency, 1.0 / 44100.0);
                    for (int i = 0; i < filterTimes; i ++) {
                         filtered = filterResults(filtered, frequency, 1.0 / 44100.0);
                    }

                    //Now we need to
                    ArrayList<Integer> peaks = getPeaks(filtered);
                    if (peaks.size() > 0) {
                        int lastPeak = peaks.get(peaks.size() - 1);
                        startLastSignal = SystemClock.elapsedRealtimeNanos()- (peaks.size() - lastPeak)*1000000000L/44100;
                        updated = true;
                    }
                }
            }
        });
        listener.start();
    }

    private ArrayList<Integer> getPeaks(double[] data){
        double avg = 0;
        for (int i = 0; i < data.length; i ++){
            avg += Math.abs(data[i]);
//            Log.d("filter_ultra","avg: " + String.valueOf(avg));
        }
        avg = avg/(1.0*data.length);

        ArrayList<Integer> returnList = new ArrayList<>();
        int j = 0;
        int count = 0;
        while (j < data.length){

            if (Math.abs(data[j]) >= 10000*avg){
//                count +=1;
//                if (count > 3) {
                    returnList.add(j);
                    Log.d("filter_ultra", "avg: " + String.valueOf(avg));
                    Log.d("filter_ultra", "data: " + String.valueOf(data[j]));

                    j = j + 10000;
                    count = 0;
//                }
            }
            j += 1;
        }
        return returnList;
    }

    private double[] filterResults(double[] x, int Fc, double dt){
        double[] z = new double[x.length];
        z[0] = x[0];
        double Rc = 1.0/(2.0*Math.PI*(1/(double)(Fc)));
//        double alpha = Rc/(Rc + dt);
        double alpha = 1/(2*Math.PI*dt*Fc + 1);
//        Log.d("filter_ultra","alpha: " + String.valueOf(alpha));
        alpha = .2597;

        for (int i = 1; i < x.length; i ++){
//            z[i] = x[i] - x[i - 1];
            z[i] = alpha*(z[i-1] + x[i] - x[i - 1]);
        }
//        for (int i = 0; i < z.length; i ++){
//            z[i] = 2.5*z[i];
//        }
        return z;
    }



    public Pair<AudioRecord, Integer> findAudioRecord() {
        int[] mSampleRates = new int[] { 44100};

        int rate = 44100;
//        for (int rate : mSampleRates) {

            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            int readSize = (int) Math.pow(2, (int) (Math.log(bufferSize)/Math.log(2) + 1.1));

                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
                            Log.d("recording", String.valueOf(recorder.getSampleRate()));
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                Log.d("audio recorder", "found right config!");

                                return new Pair(recorder, bufferSize);
                        }
                    } catch (Exception e) {
                        Log.d("audio recorder", "didn't find audio");
                    }
                }
            }
//        }
        return null;
    }

    @Override
    public void onDestroy() {
        contin = false;
        listener.interrupt();
        ar.release();

    }

    private final IBinder mBinder = new ListenerBinder();

    public class ListenerBinder extends Binder {
        Listener getService() {
            // Return this instance of LocalService so clients can call public methods
            return Listener.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



}
