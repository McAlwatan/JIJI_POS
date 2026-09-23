package com.example.jijipos;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Small reusable motion helpers to give the UI a modern, tactile feel.
 * Keeps animation logic out of the individual fragments/activities.
 */
public final class UiAnim {

    private UiAnim() {}

    /**
     * Adds a subtle press-scale effect to any clickable view: it dips on
     * touch-down and springs back on release, without swallowing the click.
     */
    public static void addPressScale(final View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    scaleTo(v, 0.95f, 90);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scaleTo(v, 1f, 180);
                    break;
                default:
                    break;
            }
            return false; // let the normal click handling still run
        });
    }

    /**
     * Springs a view in from a slightly scaled, transparent state. Used for
     * hero elements like the central action button.
     */
    public static void popIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.6f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.6f, 1f));
        set.setDuration(420);
        set.setInterpolator(new OvershootInterpolator(1.4f));
        set.start();
    }

    private static void scaleTo(View view, float scale, long duration) {
        view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(duration)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }
}
