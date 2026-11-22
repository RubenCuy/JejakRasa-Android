package com.example.jejakrasa.model;

import com.example.jejakrasa.R;
import java.io.Serializable;

public class CulinarySpot implements Serializable {
    private String id;
    private String namaTempat;
    private String kategori;
    private int tahunBerdiri;
    private double rating;
    private String kisaranHarga;
    private String alamatSingkat;
    private String kisahLegendaris;
    private int imageResId;
    private String imageUrl;      // Link Direct Image dari Cloud Firestore
    private String urlGoogleMaps; // Link Google Maps dari Cloud Firestore

    // Konstruktor kosong wajib untuk membaca data otomatis dari Firebase Firestore
    public CulinarySpot() {
        this.imageResId = R.drawable.bg_placeholder_food;
    }

    public CulinarySpot(String id, String namaTempat, String kategori, int tahunBerdiri,
                        double rating, String kisaranHarga, String alamatSingkat,
                        String kisahLegendaris, String imageUrl, String urlGoogleMaps) {
        this.id = id;
        this.namaTempat = namaTempat;
        this.kategori = kategori;
        this.tahunBerdiri = tahunBerdiri;
        this.rating = rating;
        this.kisaranHarga = kisaranHarga;
        this.alamatSingkat = alamatSingkat;
        this.kisahLegendaris = kisahLegendaris;
        this.imageResId = R.drawable.bg_placeholder_food;
        this.imageUrl = imageUrl;
        this.urlGoogleMaps = urlGoogleMaps;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNamaTempat() { return namaTempat; }
    public void setNamaTempat(String namaTempat) { this.namaTempat = namaTempat; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public int getTahunBerdiri() { return tahunBerdiri; }
    public void setTahunBerdiri(int tahunBerdiri) { this.tahunBerdiri = tahunBerdiri; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getKisaranHarga() { return kisaranHarga; }
    public void setKisaranHarga(String kisaranHarga) { this.kisaranHarga = kisaranHarga; }

    public String getAlamatSingkat() { return alamatSingkat; }
    public void setAlamatSingkat(String alamatSingkat) { this.alamatSingkat = alamatSingkat; }

    public String getKisahLegendaris() { return kisahLegendaris; }
    public void setKisahLegendaris(String kisahLegendaris) { this.kisahLegendaris = kisahLegendaris; }

    public int getImageResId() { return imageResId == 0 ? R.drawable.bg_placeholder_food : imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getUrlGoogleMaps() { return urlGoogleMaps; }
    public void setUrlGoogleMaps(String urlGoogleMaps) { this.urlGoogleMaps = urlGoogleMaps; }
}