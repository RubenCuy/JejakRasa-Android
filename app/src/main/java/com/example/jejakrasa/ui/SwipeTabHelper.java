package com.example.jejakrasa.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.example.jejakrasa.MainActivity;
import com.example.jejakrasa.R;

public class SwipeTabHelper {

    public static final int TAB_EXPLORE = 0;
    public static final int TAB_SAVED = 1;
    public static final int TAB_HISTORY = 2;
    public static final int TAB_PROFILE = 3;

    private final Activity activity;
    private final int currentTabIndex;
    private final GestureDetector gestureDetector;

    private float downX;
    private float downY;
    private boolean isHorizontalDrag = false;
    private boolean navigated = false;

    public SwipeTabHelper(Activity activity, int currentTabIndex) {
        this.activity = activity;
        this.currentTabIndex = currentTabIndex;

        this.gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 75;
            private static final int SWIPE_VELOCITY_THRESHOLD = 120;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || navigated) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY) * 1.3f
                        && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    navigated = true;
                    if (diffX < 0) {
                        navigateToTab(activity, currentTabIndex, currentTabIndex + 1);
                    } else {
                        navigateToTab(activity, currentTabIndex, currentTabIndex - 1);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        View rootView = activity.findViewById(R.id.main);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                isHorizontalDrag = false;
                navigated = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;

                if (!isHorizontalDrag && Math.abs(dx) > 35 && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                    isHorizontalDrag = true;
                }

                // Efek pantulan halus jika geser mentok di tab paling kiri (Jelajah) atau paling kanan (Profil)
                if (isHorizontalDrag && rootView != null) {
                    boolean atLeftEdge = (currentTabIndex == TAB_EXPLORE && dx > 0);
                    boolean atRightEdge = (currentTabIndex == TAB_PROFILE && dx < 0);
                    if (atLeftEdge || atRightEdge) {
                        rootView.setTranslationX(dx * 0.16f);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (rootView != null && rootView.getTranslationX() != 0f) {
                    rootView.animate()
                            .translationX(0f)
                            .setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.2f))
                            .start();
                }

                if (isHorizontalDrag && !navigated) {
                    float totalDx = event.getX() - downX;
                    if (Math.abs(totalDx) > 110) {
                        navigated = true;
                        if (totalDx < 0) {
                            navigateToTab(activity, currentTabIndex, currentTabIndex + 1);
                        } else {
                            navigateToTab(activity, currentTabIndex, currentTabIndex - 1);
                        }
                    }
                }
                isHorizontalDrag = false;
                break;
        }
    }

    public static void navigateToTab(Activity activity, int fromIndex, int targetIndex) {
        if (targetIndex < TAB_EXPLORE || targetIndex > TAB_PROFILE || targetIndex == fromIndex) {
            return;
        }

        // Warna latar celah pemisah (space) di antara dua halaman yang bergeser bersamaan
        activity.getWindow().setBackgroundDrawableResource(R.color.divider_warm);

        Class<?> targetClass;
        switch (targetIndex) {
            case TAB_SAVED:
                targetClass = SavedSpotsActivity.class;
                break;
            case TAB_HISTORY:
                targetClass = ReviewHistoryActivity.class;
                break;
            case TAB_PROFILE:
                targetClass = ProfileActivity.class;
                break;
            default:
                targetClass = MainActivity.class;
                break;
        }

        Intent intent = new Intent(activity, targetClass);
        if (targetIndex == TAB_EXPLORE) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        activity.startActivity(intent);

        if (targetIndex > fromIndex) {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }

        if (fromIndex != TAB_EXPLORE) {
            activity.finish();
        }
    }
}