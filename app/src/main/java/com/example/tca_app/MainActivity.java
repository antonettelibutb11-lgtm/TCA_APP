package com.example.tca_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private boolean isAdmin = false;
    private View topBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        CurvedBottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // Initially hide Analytics tab — will be shown only after access check
        if (bottomNavigation != null) {
            bottomNavigation.setTabVisibility(2, false);
        }

        // ANALYTICS TAB VISIBILITY RULES:
        //   Admin                  → VISIBLE
        //   Approved Campus Member (isMember = true, accepted by admin) → VISIBLE
        //   Normal Student         → HIDDEN
        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdminUser, role) -> {
            isAdmin = isAdminUser;
            boolean canSeeAnalytics = isAdminUser || isApprovedMember;
            if (bottomNavigation != null) {
                bottomNavigation.setTabVisibility(2, canSeeAnalytics);
            }
        });

        // Load Default Fragment (Home Feed)
        loadFragment(new HomeFeedFragment(), getString(R.string.app_name));

        if (bottomNavigation != null) {
            bottomNavigation.setOnTabSelectedListener(position -> {
                switch (position) {
                    case 0: // Home
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        loadFragment(new HomeFeedFragment(), getString(R.string.app_name));
                        break;
                    case 1: // Campus Event
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        loadFragment(new EventCalendarFragment(), getString(R.string.title_campus_events));
                        break;
                    case 2: // Analytics / Dashboard — Admin & Approved Members only
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        // Double-check access even if tab is somehow tapped
                        AuthUtils.checkCurrentUserAccess((isApprovedMember, isAdminUser, role) -> {
                            if (isAdminUser || isApprovedMember) {
                                loadFragment(new AdminDashboardFragment(), getString(R.string.title_analytics_dashboard));
                            } else {
                                // Unauthorized student — redirect back to Home
                                loadFragment(new HomeFeedFragment(), getString(R.string.app_name));
                                android.widget.Toast.makeText(this,
                                        "Analytics is only available to admin and approved campus members.",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case 3: // Profile
                        if (topBar != null) topBar.setVisibility(View.GONE);
                        loadFragment(new ProfileFragment(), getString(R.string.app_name));
                        break;
                }
            });
        }
    }

    private void loadFragment(Fragment fragment, String title) {
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(title);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    public void navigateToTab(int position) {
        CurvedBottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.selectTab(position, true);
        }
    }
}