package com.anrlab.app;

import android.app.Application;
import android.util.Log;import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import androidx.annotation.NonNull;

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

                Log.d(TAG, "ANR Detected-"+error);
            }
        }).setANRInterceptor(new ANRWatchDog.ANRInterceptor() {
            @Override
            public long intercept(long duration) {
                long ret = MyApp.this.duration * 1000 - duration;
                if (ret > 0)
                    Log.w(TAG, "Intercepted ANR that is too short (" + duration + " ms), postponing for " + ret + " ms.");
                return ret;
            }
        });

        anrWatchDog.start();
    }
}
