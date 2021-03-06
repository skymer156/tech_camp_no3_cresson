package com.example.timepickertest1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.TimePickerDialog;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final String ALARM_TIME = "alarm_time";
    Calendar alarmCalendar = Calendar.getInstance();
    PreferenceUtil pref;

    private TextView textView1;
    private TextView textView2;
    private Button timeButtonView1;
    private Button timeButtonView2;
    private Switch alertSwitch;

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
    private Runnable alartsetting;

    private CountDownTimer mCountDownTimer;
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
                        // ???????????????MESSAGE BT?????????????????????????????????UI????????????
                    }
                    break;
                case MESSAGE_TEMP:
                    s = (String)msg.obj;
                    if(s != null){
                        // ???????????????MESSAGE TEMP?????????????????????????????????UI????????????
                    }
                    break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = new PreferenceUtil(this);

        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);

        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = bluetoothManager.getAdapter();
        // Bluetooth????????????????????????????????????????????????????????????
        if(mAdapter == null){
            // ????????????????????????????????????
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show();
        }else{
            // ?????????????????????????????????
            Toast.makeText(this, R.string.bluetooth_is_supported, Toast.LENGTH_SHORT).show();
        }
        
        setupViews();
        setListeners();

    }

    private void setupViews() {
        timeButtonView1 = findViewById(R.id.timeButton1);
        alertSwitch = findViewById(R.id.switch1);

        long alarmTime = pref.getLong(ALARM_TIME);
        if (alarmTime != 0) {
            DateFormat df = new SimpleDateFormat("HH:mm");
            Date date = new Date(alarmTime);
            timeButtonView1.setText(df.format(date));
            alertSwitch.setChecked(true);
        }
    }

    private void setListeners() {
        timeButtonView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar calendar = Calendar.getInstance();
                final int year = calendar.get(Calendar.YEAR);
                final int monthOfYear = calendar.get(Calendar.MONTH);
                final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                final int hour = calendar.get(Calendar.HOUR_OF_DAY);
                final int minute = calendar.get(Calendar.MINUTE);
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, final int y, final int m, final int d) {
                        TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                alarmCalendar.set(Calendar.YEAR, y);
                                alarmCalendar.set(Calendar.MONTH, m);
                                alarmCalendar.set(Calendar.DAY_OF_MONTH, d);
                                alarmCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                alarmCalendar.set(Calendar.MINUTE, minute);
                                alarmCalendar.set(Calendar.SECOND, 0);
                                DateFormat df = new SimpleDateFormat("HH:mm");
                                timeButtonView1.setText(df.format(alarmCalendar.getTime()));
                                register(alarmCalendar.getTimeInMillis());
                                alertSwitch.setChecked(true);

                            }
                        }, hour, minute, true);
                        timePickerDialog.show();
                    }
                }, year, monthOfYear, dayOfMonth);
                datePickerDialog.show();


            }
        });

        alertSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    register(alarmCalendar.getTimeInMillis());
                    Toast.makeText(MainActivity.this, "Set Time", Toast.LENGTH_LONG).show();
                } else {
                    unregister();
                }
            }
        });
    }

    // ??????
    private void register(long alarmTimeMillis) {
        Context context = getBaseContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(context,AlarmReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);
        PendingIntent pendingIntent = getPendingIntent();
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmTimeMillis, null), pendingIntent);
        // ??????
        pref.setLong(ALARM_TIME, alarmTimeMillis);
    }

    // ??????
    private void unregister() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent());
        pref.delete(ALARM_TIME);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setClass(this, AlarmReceiver.class);
        // ?????????????????????????????????????????????PendingIntent.getBroadcast??????????????????????????????
        // ???????????????????????????????????????FLAG_CANCEL_CURRENT????????????????????????????????????2????????????????????????????????????
        // ??????????????????????????????????????????
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}