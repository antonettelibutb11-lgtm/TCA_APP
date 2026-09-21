package com.example.tca_app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.cardview.widget.CardView;

public class CurvedBottomNavigationView extends RelativeLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int position);
    }

    private OnTabSelectedListener tabSelectedListener;
    private int currentPosition = 0;

    private LinearLayout layoutTabsBar;
    private CardView activePillIndicator;

    private final ImageView[] tabIcons = new ImageView[4];
    private final int[] iconDrawables = new int[]{
            R.drawable.nav_home,
            R.drawable.nav_calendar,
            R.drawable.nav_dashboard,
            R.drawable.nav_profile
    };

    public CurvedBottomNavigationView(Context context) {
        super(context);
        init(context);
    }

    public CurvedBottomNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CurvedBottomNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Resolve background color based on current theme
        setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.bg_main));

        setClipChildren(false);
        setClipToPadding(false);

        // 1. Sliding Soft Purple Pill Highlight Background Indicator (Z-index: 0, Elevation: 0)
        activePillIndicator = new CardView(context);
        int pillWidth = dpToPx(56);
        int pillHeight = dpToPx(42);
        LayoutParams pillParams = new LayoutParams(pillWidth, pillHeight);
        pillParams.addRule(CENTER_VERTICAL);
        activePillIndicator.setLayoutParams(pillParams);
        activePillIndicator.setRadius(dpToPx(21));
        
        // Resolve a subtle pill color for both modes
        activePillIndicator.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.chip_bg));
        
        activePillIndicator.setCardElevation(0f);
        activePillIndicator.setMaxCardElevation(0f);
        activePillIndicator.setUseCompatPadding(false);
        addView(activePillIndicator);

        // 2. Main Row for 4 Tabs (Z-index: 10, Elevation: 4dp to stay strictly on top)
        layoutTabsBar = new LinearLayout(context);
        layoutTabsBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layoutTabsBar.setOrientation(LinearLayout.HORIZONTAL);
        layoutTabsBar.setClipChildren(false);
        layoutTabsBar.setClipToPadding(false);
        layoutTabsBar.setElevation(dpToPx(4));
        addView(layoutTabsBar);

        // Build 4 Tabs
        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout tabLayout = new LinearLayout(context);
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
            tabLayout.setLayoutParams(tabParams);
            tabLayout.setGravity(Gravity.CENTER);
            tabLayout.setClipChildren(false);
            tabLayout.setClipToPadding(false);

            ImageView iconView = new ImageView(context);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
            iconView.setLayoutParams(iconParams);
            iconView.setImageResource(iconDrawables[i]);
            iconView.setElevation(dpToPx(6));

            tabIcons[i] = iconView;
            tabLayout.addView(iconView);

            tabLayout.setOnClickListener(v -> selectTab(index, true));
            layoutTabsBar.addView(tabLayout);
        }

        // Setup Initial Selection for Tab 0
        post(() -> selectTab(0, false));
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.tabSelectedListener = listener;
    }

    public void setTabVisibility(int position, boolean visible) {
        if (layoutTabsBar != null && position >= 0 && position < layoutTabsBar.getChildCount()) {
            View tabChild = layoutTabsBar.getChildAt(position);
            if (tabChild != null) {
                tabChild.setVisibility(visible ? VISIBLE : GONE);
                post(() -> {
                    if (activePillIndicator != null) {
                        activePillIndicator.setX(calculateTargetX(currentPosition));
                    }
                });
            }
        }
    }

    public void selectTab(int position, boolean animate) {
        if (position < 0 || position >= 4) return;

        currentPosition = position;

        // 1. Update Icons Styling & Floating Elevation Animation
        for (int i = 0; i < 4; i++) {
            ImageView icon = tabIcons[i];
            icon.bringToFront(); // Ensure icon is ALWAYS rendered in front of background indicator!
            if (i == position) {
                icon.clearColorFilter(); // Full vibrant original purple & gold color!
                if (animate) {
                    icon.animate()
                            .translationY(-dpToPx(5))
                            .scaleX(1.15f)
                            .scaleY(1.15f)
                            .setDuration(240)
                            .setInterpolator(new OvershootInterpolator(1.2f))
                            .start();
                } else {
                    icon.setTranslationY(-dpToPx(5));
                    icon.setScaleX(1.15f);
                    icon.setScaleY(1.15f);
                }
            } else {
                // Dim unselected icons to match the theme's secondary text color
                int tintColor = androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_secondary);
                icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
                if (animate) {
                    icon.animate()
                            .translationY(0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start();
                } else {
                    icon.setTranslationY(0f);
                    icon.setScaleX(1.0f);
                    icon.setScaleY(1.0f);
                }
            }
        }

        // 2. Smooth Liquid Sliding Animation for the Pill Indicator
        float targetX = calculateTargetX(position);

        if (animate) {
            float startX = activePillIndicator.getX();
            ValueAnimator animator = ValueAnimator.ofFloat(startX, targetX);
            animator.setDuration(260);
            animator.setInterpolator(new OvershootInterpolator(1.08f));
            animator.addUpdateListener(animation -> {
                float val = (float) animation.getAnimatedValue();
                activePillIndicator.setX(val);
            });
            animator.start();
        } else {
            activePillIndicator.setX(targetX);
        }

        if (tabSelectedListener != null) {
            tabSelectedListener.onTabSelected(position);
        }
    }

    private float calculateTargetX(int position) {
        if (layoutTabsBar != null && position >= 0 && position < layoutTabsBar.getChildCount()) {
            View tabChild = layoutTabsBar.getChildAt(position);
            if (tabChild != null && tabChild.getVisibility() != GONE && tabChild.getWidth() > 0) {
                float childLeft = tabChild.getLeft();
                float childWidth = tabChild.getWidth();
                float pillWidth = activePillIndicator != null && activePillIndicator.getWidth() > 0 
                        ? activePillIndicator.getWidth() : dpToPx(56);
                return childLeft + ((childWidth - pillWidth) / 2.0f);
            }
        }

        // Dynamic fallback based on visible tab count and active tab index
        int visibleCount = 0;
        int targetVisibleIndex = 0;
        if (layoutTabsBar != null) {
            for (int i = 0; i < layoutTabsBar.getChildCount(); i++) {
                if (layoutTabsBar.getChildAt(i).getVisibility() != GONE) {
                    if (i == position) {
                        targetVisibleIndex = visibleCount;
                    }
                    visibleCount++;
                }
            }
        }
        if (visibleCount == 0) visibleCount = 4;

        int width = getWidth();
        if (width == 0) return 0;

        float tabWidth = (float) width / (float) visibleCount;
        float pillWidth = dpToPx(56);
        return (tabWidth * targetVisibleIndex) + ((tabWidth - pillWidth) / 2.0f);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (activePillIndicator != null) {
            activePillIndicator.setX(calculateTargetX(currentPosition));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(() -> {
            if (activePillIndicator != null) {
                activePillIndicator.setX(calculateTargetX(currentPosition));
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
