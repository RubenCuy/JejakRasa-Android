package com.example.jejakrasa;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.jejakrasa.ui.ExploreFragment;
import com.example.jejakrasa.ui.HistoryFragment;
import com.example.jejakrasa.ui.ProfileFragment;
import com.example.jejakrasa.ui.SavedFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPagerMain;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPagerMain = findViewById(R.id.viewPagerMain);
        bottomNav = findViewById(R.id.bottomNav);

        // Cegah BottomNavigationView menambah padding bawah ekstra di dalam kartu mengambang
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> insets);

        // Hubungkan 4 halaman ke ViewPager2
        viewPagerMain.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 1:
                        return new SavedFragment();
                    case 2:
                        return new HistoryFragment();
                    case 3:
                        return new ProfileFragment();
                    default:
                        return new ExploreFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 4;
            }
        });

        // Celah (space) 36px di tengah saat menggeser dua halaman bersamaan
        viewPagerMain.setPageTransformer(new MarginPageTransformer(36));

        // Simpan semua 4 tab di memori agar geser bolak-balik super mulus tanpa kedip
        viewPagerMain.setOffscreenPageLimit(3);

        // Saat halaman digeser dengan jari -> ikon menu mengambang ikut berpindah otomatis
        viewPagerMain.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_saved);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_history);
                        break;
                    case 3:
                        bottomNav.setSelectedItemId(R.id.nav_profile);
                        break;
                }
            }
        });

        // Saat ikon menu mengambang diketuk -> halaman bergeser mulus ke tab tujuan
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                viewPagerMain.setCurrentItem(0, true);
                return true;
            } else if (id == R.id.nav_saved) {
                viewPagerMain.setCurrentItem(1, true);
                return true;
            } else if (id == R.id.nav_history) {
                viewPagerMain.setCurrentItem(2, true);
                return true;
            } else if (id == R.id.nav_profile) {
                viewPagerMain.setCurrentItem(3, true);
                return true;
            }
            return false;
        });
    }
}