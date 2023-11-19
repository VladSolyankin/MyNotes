package com.example.mynotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MyNotesSplashScreen extends AppCompatActivity {
    private static final int SPLASH_TIMEOUT = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notes_splash_screen);

        View splashImageView = findViewById(R.id.splashImageView);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale);

        splashImageView.startAnimation(rotateAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MyNotesSplashScreen.this, RegisterUserActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIMEOUT);
    }
}