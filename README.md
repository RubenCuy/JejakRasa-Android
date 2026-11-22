# 🍲 JejakRasa — Kurator Kuliner Legendaris Nusantara

**JejakRasa** adalah aplikasi mobile Android berbasis **Java** yang berfungsi sebagai kurator destinasi kuliner legendaris dan lokal di berbagai daerah Nusantara. Aplikasi ini membantu pengguna menemukan tempat makan legendaris yang telah berdiri puluhan tahun, memfilter lokasi berdasarkan GPS maupun wilayah, melihat peta interaktif langsung di dalam aplikasi, berkonsultasi dengan asisten AI, serta mencatat pengalaman kunjungan kuliner ke akun pribadi.

---

## ✨ Fitur Utama

- **Autentikasi & Keamanan Akun Lengkap (Firebase Auth)**
  - **Registrasi Biodata:** Input nama depan, nama belakang, pemilihan tanggal lahir melalui dialog kalender (`DatePickerDialog`), email, dan kata sandi.
  - **Verifikasi Email Otomatis:** Mengirimkan tautan verifikasi resmi dari Firebase sebelum akun dapat digunakan untuk masuk.
  - **Pemulihan Kata Sandi (Reset Password):** Menggunakan **Material 3 Bottom Sheet** modern yang terhubung ke `sendPasswordResetEmail()` lengkap dengan validasi *inline* dan tombol pintasan untuk membuka aplikasi email.
- **Katalog 28 Kuliner Legendaris Real-Time**
  - Tersinkronisasi langsung dengan koleksi `culinary_spots` di **Cloud Firestore** (`addSnapshotListener`).
  - Memuat foto kuliner secara *online* melalui URL menggunakan pustaka **Glide** sehingga ukuran file APK tetap ringan.
- **Deteksi Lokasi GPS & Filter Wilayah Cerdas**
  - Deteksi kota otomatis menggunakan `LocationManager` dan `Geocoder`.
  - Panel geser (*Bottom Sheet*) untuk menyaring kuliner berdasarkan wilayah (**Banten & Tangerang, DKI Jakarta, Yogyakarta, Surakarta, Bandung, Surabaya, Medan, Makassar**) maupun pencarian kawasan kustom.
- **Peta Mini & Navigasi Google Maps Internal (`WebView`)**
  - Menampilkan peta Google Maps interaktif langsung di halaman detail maupun layar penuh (`WebMapActivity`) dengan kontrol *zoom* tanpa memaksa pengguna keluar dari aplikasi.
- **Kurator AI Interaktif (`AskAiActivity`)**
  - Asisten virtual yang membaca katalog kuliner langsung dari Firestore untuk memberikan rekomendasi berdasarkan kota, kategori, maupun kisaran harga (*budget*).
- **Check-In Ulasan, Favorit Per Akun, & Profil Pengguna**
  - Unggah foto bukti kunjungan menggunakan **Kamera** atau **Galeri** (`ActivityResultLauncher`) serta simpan rating dan ulasan ke riwayat pribadi.
  - Daftar tempat favorit (`saved_spots`) terisolasi berdasarkan `UID` masing-masing pengguna agar data antar-akun tidak tertukar.
  - Pembaruan nama dan foto profil langsung dari halaman Profil.

---

## 🏗️ Arsitektur & Struktur Halaman

Aplikasi dibangun menggunakan kombinasi **11 Activity**, **ViewPager2 + 4 Fragment**, dan **2 Material 3 Bottom Sheet**:

- **Alur Pembuka & Autentikasi:** `SplashActivity`, `LoginActivity`, `RegisterActivity`
- **Halaman Utama (Swipe Navigation via `ViewPager2`):**
  - `MainActivity` (Wadah utama dengan *Floating Bottom Navigation*)
  - `ExploreFragment` (Tab 1 — Jelajah Katalog, Filter Lokasi, & Tombol Tanya AI)
  - `SavedFragment` (Tab 2 — Spot Kuliner Tersimpan)
  - `HistoryFragment` (Tab 3 — Riwayat Check-In & Ulasan)
  - `ProfileFragment` (Tab 4 — Biodata, Statistik, Edit Profil, & Logout)
- **Halaman Fitur Lanjutan:** `DetailSpotActivity`, `WebMapActivity`, `CheckInReviewActivity`, `AskAiActivity`, serta `SavedSpotsActivity`, `ReviewHistoryActivity`, dan `ProfileActivity`.

---

## 🛠️ Teknologi & Pustaka yang Digunakan

| Komponen | Teknologi / Library |
| :--- | :--- |
| **Bahasa Pemrograman** | Java (Java 11) |
| **IDE & Build System** | Android Studio, Gradle Kotlin DSL (`minSdk 30`, `targetSdk 37`) |
| **UI & Navigasi** | Material Design 3, `ViewPager2`, `RecyclerView`, `BottomSheetDialog` |
| **Autentikasi & Database** | Firebase BoM `33.7.0` (`firebase-auth`, `firebase-firestore`) |
| **Image Loading** | Glide `4.16.0` |
| **Lokasi & Peta** | Android `LocationManager`, `Geocoder`, Android `WebView` |

---

## 🗄️ Struktur Database Cloud Firestore

- `culinary_spots` — Menyimpan 28 data destinasi kuliner legendaris (nama tempat, kategori, tahun berdiri, rating, kisaran harga, alamat, kisah sejarah, URL gambar, dan tautan Google Maps).
- `users/{uid}` — Menyimpan profil pengguna (`firstName`, `lastName`, `birthDate`, `email`, dan foto profil).
- `users/{uid}/saved_spots` — Sub-koleksi spot kuliner favorit milik masing-masing akun.
- `reviews` — Menyimpan riwayat *check-in*, rating bintang, ulasan, dan waktu kunjungan pengguna.

---

## 🚀 Cara Menjalankan Proyek
