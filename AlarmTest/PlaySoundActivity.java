package com.example.timepickertest1;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class PlaySoundActivity extends AppCompatActivity {

    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        stopButton = findViewById(R.id.timeButton2);

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
                finish();
            }
        });
    }
}