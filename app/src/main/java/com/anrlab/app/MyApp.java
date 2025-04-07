package com.anrlab.app;

import android.app.Application;
import android.util.Log;import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import androidx.annotation.NonNull;

import com.anrlab.app.v1.ANRError;
import com.anrlab.app.v1.ANRWatchDog;
import com.anrlab.app.v2.ANRWatcher;

import io.embrace.android.embracesdk.Embrace;


public class MyApp extends Application {

    ANRWatchDog anrWatchDog = new ANRWatchDog(2000);

    int duration = 4;
    final String TAG = "ANR-Watchdog-Demo";

    final ANRWatchDog.ANRListener silentListener = new ANRWatchDog.ANRListener() {
        @Override
        public void onAppNotResponding(@NonNull ANRError error) {
            Log.e(TAG, "", error);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Embrace.getInstance().start(this);

        anrWatchDog.setANRListener(new ANRWatchDog.ANRListener() {
            @Override
            public void onAppNotResponding(@NonNull ANRError error) {
                Log.e(TAG, "Detected Application Not Responding!");

                // Some tools like ACRA are serializing the exception, so we must make sure the exception serializes correctly
                try {
                    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(error);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                Log.i(TAG, "Error was successfully serialized");

                Log.d(TAG, "ANR Detected-"+error.getStackTrace().toString());
            }
        });

        new ANRWatcher(
                100,
                700
        ).startMonitoring();
        anrWatchDog.start();
    }
}
