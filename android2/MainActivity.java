package com.example.bless_you;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final long START_TIME = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    public static final String ESP32_NAME = "ESP32test";
    public static final UUID BT_UUID = UUID.fromString(
            "00001101-0000-1000-8000-00805f9b34fb");
    public static final int MESSAGE_BT = 0;
    public static final int MESSAGE_TEMP = 2;

    private BluetoothAdapter mAdapter;
    static final String TAG = "BTTEST1";
    private BTClientThread btClientThread;

    private  TextView mTextViewCountDown;
    private  Button mButtonStartPause;
    private  Button getmButtonReset;
    private  TextView mTextViewToggle;
    private CompoundButton mToggleSwitch;

    private  CountDownTimer mCountDownTimer;
    private  boolean mTimerRunning;

    private long mTimerLeftInMillis = START_TIME;

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            String s;
            switch (msg.what){
                case MESSAGE_BT:
                    s = (String)msg.obj;
                    if(s != null){
                        // スレッドでMESSAGE BTに登録した内容はここでUIと繋げる
                    }
                    break;
                case MESSAGE_TEMP:
                    s = (String)msg.obj;
                    if(s != null){
                        // スレッドでMESSAGE TEMPに登録した内容はここでUIと繋げる
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        getmButtonReset = findViewById(R.id.button_reset);
        mTextViewToggle = findViewById(R.id.text_switch_on_off);
        mToggleSwitch = findViewById(R.id.on_off_switch);

        // Android端末がbluetoothを使用できるかの確認処理
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = bluetoothManager.getAdapter();
        // Bluetoothサポートの是非についてユーザーに知らせる
        if(mAdapter == null){
            // サポートされていない場合
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show();
        }else{
            // サポートされている場合
            Toast.makeText(this, R.string.bluetooth_is_supported, Toast.LENGTH_SHORT).show();
        }

        mToggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                if(isChecked){ // if switch on
                    mTextViewToggle.setText("Toggle On");
                }else { // if switch off
                    mTextViewToggle.setText("Toggle Off");
                }
            }
        });


        mButtonStartPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if( mTimerRunning ){
                    pauseTimer();
                }else{
                    startTimer();
                }
            }
        });

        getmButtonReset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                resetTimer();
            }
        });

        updateCountDownText();

    }

    @Override
    protected void onResume(){
        super.onResume();
        requestBtFeature();

        // bluetooth用の通信用スレッド呼び出しと実行
        btClientThread = new BTClientThread();
        btClientThread.start();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(btClientThread != null){
            btClientThread.interrupt();
            btClientThread = null;
        }
    }

    private void startTimer(){
        mCountDownTimer = new CountDownTimer(mTimerLeftInMillis, 1000){
            @Override
            public void onTick(long millisUntilFinished){
                mTimerLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish(){
                mTimerRunning = false;
                mButtonStartPause.setText("スタート");
                getmButtonReset.setVisibility(View.INVISIBLE);

            }
        }.start();

        mTimerRunning = true;
        mButtonStartPause.setText("一時停止");
        getmButtonReset.setVisibility(View.INVISIBLE);
    }

    private void pauseTimer(){
        mCountDownTimer.cancel();
        mTimerRunning = false;
        mButtonStartPause.setText("スタート");
        getmButtonReset.setVisibility(View.VISIBLE);
    }

    private void resetTimer(){
        mTimerLeftInMillis = START_TIME;
        updateCountDownText();
        mButtonStartPause.setVisibility(View.VISIBLE);
        getmButtonReset.setVisibility(View.INVISIBLE);
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimerLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimerLeftInMillis / 1000) % 60;
        String timerLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        mTextViewCountDown.setText(timerLeftFormatted);
    }

    private void requestBtFeature(){
        if( mAdapter.isEnabled()){
            return;
        }
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult( enableBtIntent, REQUEST_ENABLE_BT );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch ( requestCode ){
            case REQUEST_ENABLE_BT:
                if(Activity.RESULT_CANCELED == resultCode){
                    Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getDate(String format){
        final DateFormat df = new SimpleDateFormat(format);
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    public class BTClientThread extends Thread{

        InputStream inputStream;
        OutputStream outputStream;
        BluetoothSocket bluetoothSocket;

        public void run(){
            byte[] incomingBuff = new byte[64];

            BluetoothDevice bluetoothDevice = null;
            Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
            for( BluetoothDevice device : devices ){
                if(device.getName().equals(ESP32_NAME)){
                    bluetoothDevice = device;
                    break;
                }
            }

            if( bluetoothDevice == null){
                Log.d(TAG, "No device found.");
                return;
            }

            try{
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(
                        BT_UUID);
                while(true){
                    if(Thread.interrupted()){
                        break;
                    }
                    try {
                        bluetoothSocket.connect();

                        handler.obtainMessage( MESSAGE_BT, "CONNECTED" + bluetoothDevice.getName()).sendToTarget();

                        inputStream = bluetoothSocket.getInputStream();
                        outputStream = bluetoothSocket.getOutputStream();

                        while(true){

                            if(Thread.interrupted()){
                                break;
                            }

                            // send command
                            String command = "C";
                            String time = getDate("ss");
                            /*if( Integer.parseInt(time) % 2 == 0 ){
                                command = "C";
                            }else{
                                command = "D";
                            }*/
                            Log.d(TAG, time);
                            outputStream.write(command.getBytes());

                            // read response
                            int incomingBytes = inputStream.read(incomingBuff);
                            byte[] buff = new byte[incomingBytes];
                            System.arraycopy( incomingBuff, 0, buff, 0, incomingBytes );
                            String s = new String(buff, StandardCharsets.UTF_8);

                            // show result to UI
                            handler.obtainMessage(MESSAGE_TEMP, s).sendToTarget();

                            // 500ミリ秒ごとに送信を繰り返す
                            Thread.sleep(500);
                        }
                    }catch(IOException e){
                        // 接続が切れたら直ぐにIOExceptionをconnectが吐き出す
                        Log.d(TAG, e.getMessage());
                    }

                    handler.obtainMessage(MESSAGE_BT, "DISCONNECTED").sendToTarget();

                    // Re-try after 3 sec
                    Thread.sleep(1000 * 3);
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
            if(bluetoothSocket != null){
                try {
                    bluetoothSocket.close();
                }catch(IOException e){}
                bluetoothSocket = null;
            }
            handler.obtainMessage(MESSAGE_BT, "DISCONNECTED - Exit BTClient Thread").sendToTarget();
        }
    }

}