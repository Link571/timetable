package com.example.timetable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.timetable.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private boolean openedMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.blue_primary));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.background_light));

        if (isAnimationDisabled()) {
            binding.getRoot().postDelayed(this::openMain, 180);
        } else {
            prepareStartState();
            binding.getRoot().post(this::playIntro);
        }
    }

    private void prepareStartState() {
        binding.ivLogo.setScaleX(0.86f);
        binding.ivLogo.setScaleY(0.86f);
        binding.ivLogo.setTranslationY(dp(16));
        binding.tvAppName.setTranslationY(dp(10));
        binding.tvSubtitle.setTranslationY(dp(8));

        View[] cards = getCards();
        for (View card : cards) {
            card.setScaleX(0.72f);
            card.setTranslationY(dp(8));
        }
    }

    private void playIntro() {
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(
            ObjectAnimator.ofFloat(binding.ivLogo, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 0.86f, 1f),
            ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 0.86f, 1f),
            ObjectAnimator.ofFloat(binding.ivLogo, View.TRANSLATION_Y, dp(16), 0f)
        );
        logoSet.setDuration(520);
        logoSet.setInterpolator(new DecelerateInterpolator(1.8f));

        AnimatorSet textSet = new AnimatorSet();
        textSet.playTogether(
            ObjectAnimator.ofFloat(binding.tvAppName, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(binding.tvAppName, View.TRANSLATION_Y, dp(10), 0f),
            ObjectAnimator.ofFloat(binding.tvSubtitle, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(binding.tvSubtitle, View.TRANSLATION_Y, dp(8), 0f)
        );
        textSet.setStartDelay(220);
        textSet.setDuration(420);
        textSet.setInterpolator(new DecelerateInterpolator(1.6f));

        AnimatorSet cardSet = new AnimatorSet();
        AnimatorSet.Builder builder = null;
        View[] cards = getCards();
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            AnimatorSet cardAnim = new AnimatorSet();
            cardAnim.playTogether(
                ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(card, View.SCALE_X, 0.72f, 1f),
                ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, dp(8), 0f)
            );
            cardAnim.setStartDelay(360L + i * 55L);
            cardAnim.setDuration(260);
            cardAnim.setInterpolator(new DecelerateInterpolator(1.9f));
            if (builder == null) {
                builder = cardSet.play(cardAnim);
            } else {
                builder.with(cardAnim);
            }
        }

        AnimatorSet intro = new AnimatorSet();
        intro.playTogether(logoSet, textSet, cardSet);
        intro.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                binding.getRoot().postDelayed(SplashActivity.this::playExit, 420);
            }
        });
        intro.start();
    }

    private void playExit() {
        AnimatorSet exit = new AnimatorSet();
        exit.playTogether(
            ObjectAnimator.ofFloat(binding.splashRoot, View.ALPHA, 1f, 0f),
            ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 1f, 1.04f),
            ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 1f, 1.04f)
        );
        exit.setDuration(220);
        exit.setInterpolator(new DecelerateInterpolator(1.2f));
        exit.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                openMain();
            }
        });
        exit.start();
    }

    private View[] getCards() {
        return new View[] {
            binding.splashCard1,
            binding.splashCard2,
            binding.splashCard3,
            binding.splashCard4,
            binding.splashCard5
        };
    }

    private boolean isAnimationDisabled() {
        try {
            return Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE) == 0f;
        } catch (Settings.SettingNotFoundException ignored) {
            return false;
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openMain() {
        if (openedMain) return;
        openedMain = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
