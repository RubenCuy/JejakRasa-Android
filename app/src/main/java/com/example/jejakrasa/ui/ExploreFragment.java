package com.example.jejakrasa.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jejakrasa.R;
import com.example.jejakrasa.adapter.CulinarySpotAdapter;
import com.example.jejakrasa.model.CulinarySpot;
import com.example.jejakrasa.model.SavedSpotsManager;
import com.example.jejakrasa.model.UserLocationManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExploreFragment extends Fragment {

    private RecyclerView rvCulinarySpots;
    private CulinarySpotAdapter adapter;
    private EditText etSearch;
    private TextView tvWelcomeSubtitle, tvCurrentLocation;
    private ChipGroup chipGroupCategory;

    private FirebaseFirestore db;
    private LocationManager locationManager;
    private LocationListener singleLocationListener;

    private final List<CulinarySpot> allSpots = new ArrayList<>();
    private String selectedCategory = "Semua";
    private String searchQuery = "";

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    deteksiLokasiGpsSekarang();
                } else {
                    Toast.makeText(requireContext(), "Izin lokasi ditolak. Kamu tetap bisa memilih kota secara manual.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        rvCulinarySpots = view.findViewById(R.id.rvCulinarySpots);
        etSearch = view.findViewById(R.id.etSearch);
        tvWelcomeSubtitle = view.findViewById(R.id.tvWelcomeSubtitle);
        tvCurrentLocation = view.findViewById(R.id.tvCurrentLocation);
        LinearLayout layoutLocationBar = view.findViewById(R.id.layoutLocationBar);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        ExtendedFloatingActionButton fabAskAi = view.findViewById(R.id.fabAskAi);

        perbaruiTampilanLokasi();

        layoutLocationBar.setOnClickListener(v -> tampilkanBottomSheetLokasi());

        fabAskAi.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AskAiActivity.class));
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        rvCulinarySpots.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CulinarySpotAdapter(allSpots, spot -> {
            Intent intent = new Intent(requireContext(), DetailSpotActivity.class);
            intent.putExtra(DetailSpotActivity.EXTRA_SPOT, spot);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        rvCulinarySpots.setAdapter(adapter);

        muatSapaanUser();
        muatDataDariFirestore();
        SavedSpotsManager.loadUserSavedSpots(null);
        setupSearchAndFilter();
    }

    @Override
    public void onResume() {
        super.onResume();
        muatSapaanUser();
        perbaruiTampilanLokasi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationManager != null && singleLocationListener != null) {
            locationManager.removeUpdates(singleLocationListener);
        }
    }

    private void perbaruiTampilanLokasi() {
        if (tvCurrentLocation != null) {
            tvCurrentLocation.setText("Lokasi: " + UserLocationManager.getDisplayLabel());
        }
    }

    private void tampilkanBottomSheetLokasi() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_location, null);
        bottomSheetDialog.setContentView(sheetView);

        bottomSheetDialog.setOnShowListener(dialog -> {
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        TextView tvSheetActiveStatus = sheetView.findViewById(R.id.tvSheetActiveStatus);
        TextView tvResetLocation = sheetView.findViewById(R.id.tvResetLocation);
        MaterialCardView cardUseGps = sheetView.findViewById(R.id.cardUseGps);
        EditText etCustomCity = sheetView.findViewById(R.id.etCustomCity);
        MaterialButton btnApplyCustomCity = sheetView.findViewById(R.id.btnApplyCustomCity);

        tvSheetActiveStatus.setText("Aktif: " + UserLocationManager.getDisplayLabel());
        String currentFilter = UserLocationManager.getCityFilter();

        // 1. Banten & Tangerang
        bindLocationChip(sheetView, R.id.chipBanten, "Banten", "Provinsi Banten", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipTangerang, "Tangerang", "Tangerang", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipKotaTangerang, "Kota Tangerang", "Kota Tangerang", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipKabTangerang, "Kabupaten Tangerang", "Kabupaten Tangerang", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipTangcity, "Tangcity", "Tangcity & Sekitarnya", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipTangsel, "Tangerang Selatan", "Tangerang Selatan", currentFilter, bottomSheetDialog);

        // 2. Wilayah DKI Jakarta
        bindLocationChip(sheetView, R.id.chipJktAll, "Jakarta", "Semua Jakarta", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipJktBarat, "Jakarta Barat", "Jakarta Barat", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipJktSelatan, "Jakarta Selatan", "Jakarta Selatan", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipJktUtara, "Jakarta Utara", "Jakarta Utara", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipJktTimur, "Jakarta Timur", "Jakarta Timur", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipJktPusat, "Jakarta Pusat", "Jakarta Pusat", currentFilter, bottomSheetDialog);

        // 3. Kota Nusantara Lainnya
        bindLocationChip(sheetView, R.id.chipCityAll, "", "Semua Kota", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipCityJogja, "Yogyakarta", "Yogyakarta", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipCitySolo, "Surakarta", "Surakarta", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipCityBandung, "Bandung", "Bandung", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipCitySurabaya, "Surabaya", "Surabaya", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipCityMedan, "Medan", "Medan", currentFilter, bottomSheetDialog);
        bindLocationChip(sheetView, R.id.chipCityMakassar, "Makassar", "Makassar", currentFilter, bottomSheetDialog);

        // Tombol GPS
        cardUseGps.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            cekIzinLaluDeteksiGps();
        });

        // Tombol Reset
        tvResetLocation.setOnClickListener(v -> {
            UserLocationManager.clearLocation();
            perbaruiTampilanLokasi();
            filterSpots();
            bottomSheetDialog.dismiss();
            Toast.makeText(requireContext(), "Menampilkan kuliner dari semua kota", Toast.LENGTH_SHORT).show();
        });

        // Tombol Kota / Kawasan Kustom
        btnApplyCustomCity.setOnClickListener(v -> {
            String customCity = etCustomCity.getText().toString().trim();
            if (customCity.isEmpty()) {
                etCustomCity.setError("Masukkan nama kota atau kawasan");
                return;
            }
            UserLocationManager.setManualCity(customCity, customCity + " (Filter Aktif)");
            perbaruiTampilanLokasi();
            filterSpots();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void bindLocationChip(View sheetView, int chipId, String filterValue, String displayLabel,
                                  String currentFilter, BottomSheetDialog dialog) {
        Chip chip = sheetView.findViewById(chipId);
        if (chip == null) return;

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        chip.setChipBackgroundColor(new ColorStateList(states, new int[]{
                Color.parseColor("#FFEDD5"),
                Color.parseColor("#FFFFFF")
        }));
        chip.setTextColor(new ColorStateList(states, new int[]{
                Color.parseColor("#9A3412"),
                Color.parseColor("#1F2937")
        }));
        chip.setChipStrokeColor(new ColorStateList(states, new int[]{
                Color.parseColor("#9A3412"),
                Color.parseColor("#E5E7EB")
        }));
        chip.setCheckedIconTint(ColorStateList.valueOf(Color.parseColor("#9A3412")));

        chip.setChecked(currentFilter.equalsIgnoreCase(filterValue));
        chip.setOnClickListener(v -> {
            if (filterValue.isEmpty()) {
                UserLocationManager.clearLocation();
            } else {
                UserLocationManager.setManualCity(filterValue, displayLabel + " (Filter Aktif)");
            }
            perbaruiTampilanLokasi();
            filterSpots();
            dialog.dismiss();
        });
    }

    private void cekIzinLaluDeteksiGps() {
        boolean fineOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineOk || coarseOk) {
            deteksiLokasiGpsSekarang();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void deteksiLokasiGpsSekarang() {
        if (locationManager == null) return;
        tvCurrentLocation.setText("Mendeteksi koordinat GPS...");

        Location lastKnown = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (lastKnown == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception ignored) {}

        if (lastKnown != null) {
            prosesKoordinatGps(lastKnown);
            return;
        }

        singleLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!isAdded()) return;
                locationManager.removeUpdates(this);
                prosesKoordinatGps(location);
            }
        };

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, singleLocationListener, Looper.getMainLooper());
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, singleLocationListener, Looper.getMainLooper());
            } else {
                tvCurrentLocation.setText("GPS belum aktif (Pilih kota manual)");
                Toast.makeText(requireContext(), "Aktifkan GPS di pengaturan HP/Emulator atau pilih kota manual", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            perbaruiTampilanLokasi();
        }
    }

    private void prosesKoordinatGps(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String namaKota = "";
        String adminArea = "";
        String negara = "";

        try {
            Geocoder geocoder = new Geocoder(requireContext(), new Locale("id", "ID"));
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                negara = addr.getCountryCode() != null ? addr.getCountryCode() : "";
                adminArea = addr.getAdminArea() != null ? addr.getAdminArea() : "";
                if (addr.getSubAdminArea() != null) {
                    namaKota = addr.getSubAdminArea();
                } else if (addr.getLocality() != null) {
                    namaKota = addr.getLocality();
                } else {
                    namaKota = adminArea;
                }
            }
        } catch (Exception ignored) {}

        String kataKunciKota = namaKota.replace("Kota ", "").replace("Kabupaten ", "").trim();
        String lowerGeo = (namaKota + " " + adminArea).toLowerCase();

        if (lowerGeo.contains("tangerang selatan")) {
            kataKunciKota = "Tangerang Selatan";
        } else if (namaKota.toLowerCase().contains("kota tangerang")) {
            kataKunciKota = "Kota Tangerang";
        } else if (namaKota.toLowerCase().contains("kabupaten tangerang")) {
            kataKunciKota = "Kabupaten Tangerang";
        } else if (lowerGeo.contains("tangerang")) {
            kataKunciKota = "Tangerang";
        } else if (lowerGeo.contains("jakarta barat")) {
            kataKunciKota = "Jakarta Barat";
        } else if (lowerGeo.contains("jakarta selatan")) {
            kataKunciKota = "Jakarta Selatan";
        } else if (lowerGeo.contains("jakarta utara")) {
            kataKunciKota = "Jakarta Utara";
        } else if (lowerGeo.contains("jakarta timur")) {
            kataKunciKota = "Jakarta Timur";
        } else if (lowerGeo.contains("jakarta pusat")) {
            kataKunciKota = "Jakarta Pusat";
        } else if (lowerGeo.contains("jakarta")) {
            kataKunciKota = "Jakarta";
        }

        if ("ID".equalsIgnoreCase(negara) && !kataKunciKota.isEmpty()) {
            UserLocationManager.setGpsLocation(lat, lng, kataKunciKota, namaKota + " (GPS Aktif)");
            Toast.makeText(requireContext(), "Lokasi terdeteksi: " + namaKota, Toast.LENGTH_SHORT).show();
        } else {
            String labelKoordinat = String.format(Locale.US, "GPS (%.3f, %.3f) - Semua Kota", lat, lng);
            UserLocationManager.setGpsLocation(lat, lng, "", labelKoordinat);
            Toast.makeText(requireContext(), "Koordinat GPS tersimpan untuk rute peta", Toast.LENGTH_SHORT).show();
        }

        perbaruiTampilanLokasi();
        filterSpots();
    }

    private void muatSapaanUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || tvWelcomeSubtitle == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String first = doc.getString("firstName");
                    if (first != null && !first.isEmpty()) {
                        tvWelcomeSubtitle.setText("Halo, " + first + "! Mau berburu rasa di mana hari ini?");
                    }
                });
    }

    private void muatDataDariFirestore() {
        db.collection("culinary_spots")
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        Toast.makeText(requireContext(), "Gagal memuat katalog: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Otomatis unggah 28 data kuliner jika database masih kosong atau belum lengkap
                    if (snapshots == null || snapshots.size() < 28) {
                        isiDataAwalKeFirestore();
                        return;
                    }

                    allSpots.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        CulinarySpot spot = doc.toObject(CulinarySpot.class);
                        if (spot != null) {
                            spot.setId(doc.getId());
                            allSpots.add(spot);
                        }
                    }
                    filterSpots();
                });
    }

    private void isiDataAwalKeFirestore() {
        List<CulinarySpot> initialSpots = new ArrayList<>();

        // === 1. BANTEN & TANGERANG (6 SPOT) ===
        initialSpots.add(new CulinarySpot("1", "Laksa Benteng Ny. Ai", "Makanan Berat", 1978, 4.9,
                "Rp 22.000 - Rp 35.000", "Pasar Lama, Kota Tangerang, Banten",
                "Kuliner peranakan legendaris khas Tangerang dengan kuah santan rempah kuning yang kental, mi beras kenyal, serta aroma daun kucai segar yang autentik.",
                "https://images.unsplash.com/photo-1555126634-323283e090fa?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Laksa+Tangerang+Pasar+Lama"));

        initialSpots.add(new CulinarySpot("2", "Es Bun Tin Pasar Lama", "Minuman & Kopi", 1980, 4.8,
                "Rp 15.000 - Rp 25.000", "Kawasan Tangcity & Pasar Lama, Kota Tangerang",
                "Es serut legendaris Tangerang dengan sirup racikan sendiri, kacang merah lembut, dan potongan alpukat segar yang wajib dicoba saat cuaca terik.",
                "https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Es+Bun+Tin+Pasar+Lama+Tangerang"));

        initialSpots.add(new CulinarySpot("3", "Bakmi Pasar Lama Aon", "Jajanan Legendaris", 1972, 4.8,
                "Rp 25.000 - Rp 40.000", "Jl. Kisamaun, Pasar Lama, Kota Tangerang",
                "Bakmi karet racikan tradisional di pusat kuliner Pasar Lama Tangerang yang disajikan dengan pangsit rebus lembut dan kuah kaldu gurih.",
                "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Bakmi+Pasar+Lama+Tangerang"));

        initialSpots.add(new CulinarySpot("4", "Sop Ikan Batam Gading Serpong", "Makanan Berat", 1995, 4.8,
                "Rp 40.000 - Rp 65.000", "Gading Serpong, Kabupaten Tangerang, Banten",
                "Hidangan kuah bening segar dengan irisan ikan tenggiri pilihan, sawi asin, dan bawang goreng yang menjadi favorit keluarga di kawasan Gading Serpong.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Sop+Ikan+Batam+Gading+Serpong"));

        initialSpots.add(new CulinarySpot("5", "Sate Ayam H. Ishak BSD", "Makanan Berat", 1988, 4.8,
                "Rp 30.000 - Rp 50.000", "BSD City, Tangerang Selatan, Banten",
                "Sate ayam legendaris dengan potongan daging tebal dan bumbu kacang sangrai halus yang meresap sempurna hingga ke tusukan terakhir.",
                "https://images.unsplash.com/photo-1529563021893-cc83c992d75d?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Sate+Ayam+H+Ishak+BSD+Tangerang+Selatan"));

        initialSpots.add(new CulinarySpot("6", "Rabeg Khas Serang H. Naswi", "Makanan Berat", 1975, 4.9,
                "Rp 35.000 - Rp 55.000", "Cimuncang, Kota Serang, Banten",
                "Masakan daging kambing berempah warisan Kesultanan Banten dengan perpaduan rasa manis, gurih, dan hangat dari lada, kayu manis, serta pala.",
                "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Rabeg+H+Naswi+Serang+Banten"));

        // === 2. DKI JAKARTA: PUSAT, BARAT, SELATAN, UTARA, TIMUR (10 SPOT) ===
        initialSpots.add(new CulinarySpot("7", "Soto Betawi H. Ma'ruf", "Makanan Berat", 1943, 4.9,
                "Rp 35.000 - Rp 55.000", "Gondangdia, Jakarta Pusat",
                "Berdiri sejak masa pendudukan Jepang tahun 1943, soto betawi ini mempertahankan racikan kuah santan dan susu dengan daging sapi empuk yang dimasak menggunakan tungku tradisional.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Soto+Betawi+H+Maruf+Gondangdia"));

        initialSpots.add(new CulinarySpot("8", "Bakmi Gang Kelinci", "Makanan Berat", 1957, 4.8,
                "Rp 30.000 - Rp 48.000", "Pasar Baru, Jakarta Pusat",
                "Bermula dari gerobak sederhana di gang kecil kawasan Pasar Baru sejak 1957, mie kenyal buatan sendiri dengan topping ayam kampung rebus dan pangsit gorengnya menjadi ikon kuliner ibu kota.",
                "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Bakmi+Gang+Kelinci+Pasar+Baru"));

        initialSpots.add(new CulinarySpot("9", "Kopi Es Tak Kie", "Minuman & Kopi", 1927, 4.9,
                "Rp 20.000 - Rp 30.000", "Gang Gloria Glodok, Jakarta Barat",
                "Kedai kopi legendaris lintas tiga generasi sejak 1927 yang menyajikan kopi tubruk dan es kopi susu klasik dari campuran biji robusta dan arabika Lampung tanpa pemanis buatan.",
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Kopi+Es+Tak+Kie+Glodok"));

        initialSpots.add(new CulinarySpot("10", "Rujak Shanghai Encim", "Jajanan Legendaris", 1950, 4.8,
                "Rp 25.000 - Rp 40.000", "Pancoran Glodok, Jakarta Barat",
                "Perpaduan unik cumi dan ubur-ubur rebus dengan kangkung serta lobak yang disiram saus merah kental asam manis khas kawasan Pecinan Glodok.",
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Rujak+Shanghai+Encim+Glodok"));

        initialSpots.add(new CulinarySpot("11", "Gultik Blok M Plaza", "Makanan Berat", 1985, 4.8,
                "Rp 15.000 - Rp 25.000", "Melawai, Kebayoran Baru, Jakarta Selatan",
                "Gulai sapi kuah santan gurih yang disajikan dalam porsi mungil khas kawasan Melawai, menjadi destinasi kuliner malam ikonik Jakarta Selatan sejak era 1980-an.",
                "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Gultik+Blok+M+Jakarta+Selatan"));

        initialSpots.add(new CulinarySpot("12", "Bubur Ayam Barito", "Jajanan Legendaris", 1992, 4.8,
                "Rp 20.000 - Rp 32.000", "Jl. Gandaria Tengah, Jakarta Selatan",
                "Bubur ayam kental khas Jakarta Selatan yang terkenal dengan topping melimpah, cakwe renyah, suwiran ayam kampung, dan stik keju gurih di atasnya.",
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Bubur+Ayam+Barito+Jakarta+Selatan"));

        initialSpots.add(new CulinarySpot("13", "Seafood 212 Wiro Sableng", "Makanan Berat", 1988, 4.8,
                "Rp 60.000 - Rp 120.000", "Kelapa Gading, Jakarta Utara",
                "Pelopor hidangan boga bahari legendaris di kawasan Kelapa Gading dengan kepiting saus Padang dan ikan bakar bumbu khas yang selalu ramai pengunjung.",
                "https://images.unsplash.com/photo-1555126634-323283e090fa?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Seafood+212+Wiro+Sableng+Kelapa+Gading"));

        initialSpots.add(new CulinarySpot("14", "Kopi Kwang Koan", "Minuman & Kopi", 1965, 4.8,
                "Rp 18.000 - Rp 28.000", "Pademangan, Jakarta Utara",
                "Kedai kopi klasik bergaya kopitiam di Jakarta Utara dengan aroma kopi sangrai pekat dan roti bakar srikaya buatan sendiri.",
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Kopi+Kwang+Koan+Jakarta+Utara"));

        initialSpots.add(new CulinarySpot("15", "Soto Jakarta Bang Madun", "Makanan Berat", 1979, 4.8,
                "Rp 30.000 - Rp 45.000", "Rawamangun, Jakarta Timur",
                "Soto daging sapi dan paru goreng dengan kuah susu gurih yang menjadi kebanggaan warga Rawamangun dan Jakarta Timur sejak puluhan tahun lalu.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Soto+Jakarta+Rawamangun+Jakarta+Timur"));

        initialSpots.add(new CulinarySpot("16", "Pancong Restumibu Jatinegara", "Jajanan Legendaris", 1982, 4.7,
                "Rp 12.000 - Rp 22.000", "Jatinegara, Jakarta Timur",
                "Kue pancong klasik berbahan kelapa parut dan tepung beras yang dipanggang setengah matang hingga lumer dan gurih di setiap gigitan.",
                "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Kue+Pancong+Jatinegara+Jakarta+Timur"));

        // === 3. KOTA NUSANTARA LAINNYA (12 SPOT) ===
        initialSpots.add(new CulinarySpot("17", "Gudeg Yu Djum Wijilan", "Makanan Berat", 1951, 4.8,
                "Rp 25.000 - Rp 50.000", "Jl. Wijilan, Yogyakarta",
                "Dimasak menggunakan kayu bakar nangka selama lebih dari 12 jam untuk menghasilkan cita rasa manis gurih khas Yogyakarta yang meresap sempurna hingga ke serat nangka muda.",
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Gudeg+Yu+Djum+Wijilan+Yogyakarta"));

        initialSpots.add(new CulinarySpot("18", "Kopi Joss Lik Man", "Minuman & Kopi", 1968, 4.8,
                "Rp 10.000 - Rp 20.000", "Stasiun Tugu, Yogyakarta",
                "Pencetus pertama kopi arang panas di dekat Stasiun Tugu Yogyakarta yang memberikan sensasi karamel unik dan aroma asap kayu alami.",
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Kopi+Joss+Lik+Man+Yogyakarta"));

        initialSpots.add(new CulinarySpot("19", "Serabi Notosuman Ny. Lidia", "Jajanan Legendaris", 1923, 4.9,
                "Rp 28.000 - Rp 40.000", "Jl. Moh. Yamin, Surakarta",
                "Jajanan klasik asal Solo yang dibuat dari tepung beras dan santan kental di atas wajan tanah liat kecil, menghasilkan pinggiran renyah dengan bagian tengah yang lembut dan harum pandan.",
                "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Serabi+Notosuman+Solo"));

        initialSpots.add(new CulinarySpot("20", "Selat Vien's Pusat", "Makanan Berat", 1985, 4.8,
                "Rp 22.000 - Rp 38.000", "Banjarsari, Surakarta",
                "Hidangan bistik bergaya Eropa-Jawa khas keraton Surakarta dengan kuah semur segar, galantin daging lembut, kentang goreng, dan saus mustar.",
                "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Selat+Viens+Surakarta"));

        initialSpots.add(new CulinarySpot("21", "Mie Kocok Mang Dadeng", "Makanan Berat", 1958, 4.9,
                "Rp 32.000 - Rp 48.000", "Jl. Banteng, Bandung",
                "Mie kocok legendaris Kota Kembang dengan kaldu kaki sapi kental yang dimasak dengan 27 macam rempah pilihan sejak tahun 1958.",
                "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Mie+Kocok+Mang+Dadeng+Bandung"));

        initialSpots.add(new CulinarySpot("22", "Warung Kopi Purnama", "Minuman & Kopi", 1930, 4.9,
                "Rp 20.000 - Rp 38.000", "Jl. Alkateri, Bandung",
                "Kedai kopi tertua di Bandung yang berdiri sejak zaman kolonial 1930, terkenal dengan kopi susu klasik dan roti selai srikaya legendarisnya.",
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Warung+Kopi+Purnama+Bandung"));

        initialSpots.add(new CulinarySpot("23", "Rawon Setan Embong Malang", "Makanan Berat", 1951, 4.8,
                "Rp 38.000 - Rp 55.000", "Genteng, Surabaya",
                "Rawon khas Jawa Timur dengan kuah kluwek hitam pekat, potongan daging sapi empuk berukuran besar, dan sambal terasi pedas menggugah selera.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Rawon+Setan+Embong+Malang+Surabaya"));

        initialSpots.add(new CulinarySpot("24", "Zangrandi Ice Cream", "Jajanan Legendaris", 1930, 4.8,
                "Rp 30.000 - Rp 55.000", "Jl. Yos Sudarso, Surabaya",
                "Kedai es krim klasik bergaya art deco di pusat Kota Surabaya yang mempertahankan resep es krim buatan rumah tanpa bahan pengawet sejak 1930.",
                "https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Zangrandi+Ice+Cream+Surabaya"));

        initialSpots.add(new CulinarySpot("25", "Soto Kesawan Medan", "Makanan Berat", 1955, 4.9,
                "Rp 30.000 - Rp 45.000", "Jl. Ahmad Yani, Medan",
                "Soto khas Medan dengan kuah santan kuning harum rempah dan isian udang galah segar serta daging sapi pilihan di kawasan bersejarah Kesawan.",
                "https://images.unsplash.com/photo-1555126634-323283e090fa?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Soto+Kesawan+Medan"));

        initialSpots.add(new CulinarySpot("26", "Kedai Kopi Apek", "Minuman & Kopi", 1922, 4.9,
                "Rp 15.000 - Rp 28.000", "Kesawan, Medan",
                "Salah satu kedai kopi tertua di Sumatra yang berdiri sejak 1922, menyeduh kopi Sidikalang asli menggunakan teko tradisional di atas tungku arang.",
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Kedai+Kopi+Apek+Medan"));

        initialSpots.add(new CulinarySpot("27", "Coto Nusantara Makassar", "Makanan Berat", 1975, 4.9,
                "Rp 25.000 - Rp 40.000", "Jl. Nusantara, Makassar",
                "Ikon kuliner Sulawesi Selatan dengan kuah kaldu daging sapi kental berbumbu kacang sangrai dan rempah khas, disajikan bersama ketupat hangat.",
                "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Coto+Nusantara+Makassar"));

        initialSpots.add(new CulinarySpot("28", "Es Pisang Ijo Muda Mudi", "Jajanan Legendaris", 1980, 4.8,
                "Rp 18.000 - Rp 28.000", "Ujung Pandang, Makassar",
                "Pisang raja matang berbalut adonan tepung pandan hijau lembut yang disajikan dengan bubur sumsum gurih, sirup merah khas Makassar, dan es serut.",
                "https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=800&q=80",
                "https://www.google.com/maps/search/?api=1&query=Es+Pisang+Ijo+Muda+Mudi+Makassar"));

        for (CulinarySpot spot : initialSpots) {
            db.collection("culinary_spots").document(spot.getId()).set(spot);
        }
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                filterSpots();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipHeavyMeal) {
                selectedCategory = "Makanan Berat";
            } else if (id == R.id.chipSnack) {
                selectedCategory = "Jajanan Legendaris";
            } else if (id == R.id.chipDrink) {
                selectedCategory = "Minuman & Kopi";
            } else {
                selectedCategory = "Semua";
            }
            filterSpots();
        });
    }

    private void filterSpots() {
        String cityFilter = UserLocationManager.getCityFilter().toLowerCase().trim();
        List<CulinarySpot> filtered = new ArrayList<>();

        for (CulinarySpot spot : allSpots) {
            boolean matchCategory = selectedCategory.equals("Semua")
                    || (spot.getKategori() != null && spot.getKategori().equalsIgnoreCase(selectedCategory));

            boolean matchCity = matchesLocationFilter(spot, cityFilter);

            boolean matchQuery = searchQuery.isEmpty()
                    || (spot.getNamaTempat() != null && spot.getNamaTempat().toLowerCase().contains(searchQuery))
                    || (spot.getAlamatSingkat() != null && spot.getAlamatSingkat().toLowerCase().contains(searchQuery));

            if (matchCategory && matchCity && matchQuery) {
                filtered.add(spot);
            }
        }
        adapter.updateData(filtered);
    }

    private boolean matchesLocationFilter(CulinarySpot spot, String filter) {
        if (filter.isEmpty()) return true;
        String alamat = spot.getAlamatSingkat() != null ? spot.getAlamatSingkat().toLowerCase() : "";

        switch (filter) {
            case "banten":
                return alamat.contains("banten") || alamat.contains("tangerang")
                        || alamat.contains("tangcity") || alamat.contains("bsd")
                        || alamat.contains("serang") || alamat.contains("cilegon");
            case "tangerang":
                return alamat.contains("tangerang") || alamat.contains("tangcity")
                        || alamat.contains("bsd") || alamat.contains("serpong");
            case "kota tangerang":
                return alamat.contains("kota tangerang") || alamat.contains("tangcity")
                        || alamat.contains("pasar lama") || alamat.contains("cikokol");
            case "kabupaten tangerang":
                return alamat.contains("kabupaten tangerang") || alamat.contains("kab. tangerang")
                        || alamat.contains("gading serpong") || alamat.contains("cikupa")
                        || alamat.contains("tigaraksa");
            case "tangcity":
                return alamat.contains("tangcity") || alamat.contains("cikokol")
                        || alamat.contains("kota tangerang");
            case "tangerang selatan":
                return alamat.contains("tangerang selatan") || alamat.contains("tangsel")
                        || alamat.contains("bsd") || alamat.contains("bintaro");
            default:
                return alamat.contains(filter);
        }
    }
}