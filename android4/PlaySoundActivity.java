package com.example.timepickertest1;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class PlaySoundActivity extends AppCompatActivity {

    private Button stopButton;
    private BTClientThread btClientThread;

    private static final long START_TIME = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    public static final String ESP32_NAME = "ESP32test";
    public static final UUID BT_UUID = UUID.fromString(
            "00001101-0000-1000-8000-00805f9b34fb");
    public static final int MESSAGE_BT = 0;
    public static final int MESSAGE_TEMP = 2;

    private BluetoothAdapter mAdapter;
    static final String TAG = "BTTEST1";

    private Handler mHandler = new Handler();

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
        setContentView(R.layout.activity_alarm);
        stopButton = findViewById(R.id.button);

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

        requestBtFeature();

        // bluetooth用の通信用スレッド呼び出しと実行
        btClientThread = new BTClientThread();
        btClientThread.start();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setContentView(R.layout.activity_play_sound);

//        startService(new Intent(this, PlaySoundService.class));

//        stop = (Button) findViewById(R.id.stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                stopService(new Intent(PlaySoundActivity.this, PlaySoundService.class));
                PreferenceUtil pref = new PreferenceUtil(PlaySoundActivity.this);
                pref.delete(MainActivity.ALARM_TIME);
                btClientThread.currentThread().interrupt();
                finish();
            }
        });
    }

    public static String getDate(String format){
        final DateFormat df = new SimpleDateFormat(format);
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
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
                            // String command = "C";

                            String time = getDate("ss");
                            /*if( Integer.parseInt(time) % 2 == 0 ){
                                command = "C";
                            }else{
                                command = "D";
                            }*/
                            Log.d(TAG, time);
                            String command = "C";
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