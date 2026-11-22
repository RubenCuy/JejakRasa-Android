package com.example.jejakrasa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jejakrasa.R;
import com.example.jejakrasa.adapter.CulinarySpotAdapter;
import com.example.jejakrasa.model.CulinarySpot;
import com.example.jejakrasa.model.SavedSpotsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class SavedSpotsActivity extends AppCompatActivity {

    private RecyclerView rvSavedSpots;
    private TextView tvEmptySaved, tvUserEmail;
    private CulinarySpotAdapter adapter;
    private FirebaseAuth mAuth;
    private SwipeTabHelper swipeTabHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_saved_spots);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        swipeTabHelper = new SwipeTabHelper(this, SwipeTabHelper.TAB_SAVED);

        rvSavedSpots = findViewById(R.id.rvSavedSpots);
        tvEmptySaved = findViewById(R.id.tvEmptySaved);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        Button btnLogout = findViewById(R.id.btnLogout);
        BottomNavigationView bottomNavSaved = findViewById(R.id.bottomNavSaved);

        bottomNavSaved.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            tvUserEmail.setText("Akun: " + currentUser.getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Keluar Akun")
                    .setMessage("Yakin ingin keluar dari akun ini?")
                    .setPositiveButton("Keluar", (dialog, which) -> {
                        SavedSpotsManager.clearCache();
                        mAuth.signOut();
                        Toast.makeText(this, "Berhasil keluar akun", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SavedSpotsActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        rvSavedSpots.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CulinarySpotAdapter(SavedSpotsManager.getCachedSpots(), spot -> {
            Intent intent = new Intent(SavedSpotsActivity.this, DetailSpotActivity.class);
            intent.putExtra(DetailSpotActivity.EXTRA_SPOT, spot);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        rvSavedSpots.setAdapter(adapter);

        bottomNavSaved.setSelectedItemId(R.id.nav_saved);
        bottomNavSaved.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_SAVED, SwipeTabHelper.TAB_EXPLORE);
                return true;
            } else if (id == R.id.nav_history) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_SAVED, SwipeTabHelper.TAB_HISTORY);
                return true;
            } else if (id == R.id.nav_profile) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_SAVED, SwipeTabHelper.TAB_PROFILE);
                return true;
            }
            return id == R.id.nav_saved;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        swipeTabHelper.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SavedSpotsManager.loadUserSavedSpots(spots -> {
            adapter.updateData(spots);
            tvEmptySaved.setVisibility(spots.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}