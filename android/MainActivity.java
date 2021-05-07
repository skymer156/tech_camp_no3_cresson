package com.example.bless_you;

import androidx.appcompat.app.AppCompatActivity;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Button;

import java.util.Locale;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private static final long START_TIME = 10000;

    private  TextView mTextViewCountDown;
    private  Button mButtonStartPause;
    private  Button getmButtonReset;
    private  TextView mTextViewToggle;
    private CompoundButton mToggleSwitch;

    private  CountDownTimer mCountDownTimer;
    private  boolean mTimerRunning;

    private long mTimerLeftInMillis = START_TIME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        getmButtonReset = findViewById(R.id.button_reset);
        mTextViewToggle = findViewById(R.id.text_switch_on_off);
        mToggleSwitch = findViewById(R.id.on_off_switch);
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

}