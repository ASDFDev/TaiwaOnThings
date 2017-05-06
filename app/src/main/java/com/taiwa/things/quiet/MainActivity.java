package com.taiwa.things.quiet;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.nio.charset.Charset;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class MainActivity extends AppCompatActivity {

    private Subscription frameSubscription = Subscriptions.empty();
    private String redLed = "BCM6";
    private String TAG = "Taiwa";
    private Gpio gpio;
    PeripheralManagerService peripheralManagerService = new PeripheralManagerService();
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try{
            gpio = peripheralManagerService.openGpio(redLed);
        } catch (IOException ioe){
            Log.e(TAG, "Error on PeripheralIO API", ioe);
        }
        subscribeToFrames();
    }

    private void subscribeToFrames() {
        frameSubscription.unsubscribe();
        frameSubscription = FrameReceiverObservable.create(this, MainActivity.this.getResources().getString(R.string.quiet_profile)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(buf -> {
            System.out.println("This software is using: " + MainActivity.this.getResources().getString(R.string.quiet_profile));
            String receivedMessage = new String(buf, Charset.forName("UTF-8"));
            switch (receivedMessage) {
                case "toggleRedLed":
                    handler.post(toggleRed);
                    break;
            }
        });

    }

    private Runnable toggleRed = new Runnable() {
        @Override
        public void run() {
            if (gpio == null) {
                return;
            } try {
                    gpio.setValue(!gpio.getValue());
            } catch (IOException ioe) {
                Log.e(TAG, "Error on Peripheral IO API", ioe);
            }
        }
    };


    //Close and free up resource
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(gpio != null){
            try{
                gpio.close();
            } catch (IOException ioe){
                Log.e(TAG, "Error on Peripheral IO API", ioe);
            }
        }
    }
}
