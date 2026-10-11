package com.example.healthapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.healthapp.diet.DietFragment;
import com.example.healthapp.exercise.ExerciseFragment;
import com.example.healthapp.database.SeedData;
import com.example.healthapp.home.HomeFragment;
import com.example.healthapp.profile.ProfileFragment;
import com.example.healthapp.summary.SummaryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Hosts the five top-level fragments. Business logic belongs in each module package. */
public class MainActivity extends AppCompatActivity {
    private static final String STATE_SELECTED_ITEM = "selected_navigation_item";

    private BottomNavigationView bottomNavigation;
    private int selectedItemId = R.id.navigation_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fill the built-in exercise/food libraries once per install (async, no-op when done).
        SeedData.seedIfEmpty(this);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            selectedItemId = item.getItemId();
            return showSelectedPage(selectedItemId);
        });

        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
        } else {
            selectedItemId = savedInstanceState.getInt(
                    STATE_SELECTED_ITEM, R.id.navigation_home);
            bottomNavigation.setSelectedItemId(selectedItemId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_ITEM, selectedItemId);
        super.onSaveInstanceState(outState);
    }

    private boolean showSelectedPage(int itemId) {
        Fragment fragment;
        if (itemId == R.id.navigation_exercise) {
            fragment = new ExerciseFragment();
        } else if (itemId == R.id.navigation_diet) {
            fragment = new DietFragment();
        } else if (itemId == R.id.navigation_summary) {
            fragment = new SummaryFragment();
        } else if (itemId == R.id.navigation_profile) {
            fragment = new ProfileFragment();
        } else {
            fragment = new HomeFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        return true;
    }
}
