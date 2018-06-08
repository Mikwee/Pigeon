package com.mikwee.timebrowser.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mikwee.timebrowser.R;


public class Wakkamole {

    private InterstitialAd mInterstitialAd;
    private Context c;


    public Wakkamole(Context c) {
        this.c = c;
        createNewAd();
    }

    //Create new Ad
    private void createNewAd() {
        // Create the InterstitialAd and set the adUnitId (defined in values/strings.xml).
        mInterstitialAd = newInterstitialAd();
        //Load the Ad
        loadInterstitial();
    }

    //Initialize the InterstialAd
    private InterstitialAd newInterstitialAd() {
        //Create new instance
        InterstitialAd interstitialAd = new InterstitialAd(c);
        //Set my unique AdUnitId
        interstitialAd.setAdUnitId(c.getString(R.string.interstitial_ad_unit_id));
        //Set Ad listener
        interstitialAd.setAdListener(new AdListener() {
            @Override //When my ad is loaded
            public void onAdLoaded() {
                //Show Ad
                showInterstitial();
            }

            @Override //If Ad fails to load
            public void onAdFailedToLoad(int errorCode) {
                //Toast "come back to see magic
               // createNewAd();
            }

            @Override // When the ad is closed
            public void onAdClosed() {
            }
        });

        return interstitialAd;
    }

    //Load the Ad
    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    //Show the Ad if is loaded
    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and reload the ad.
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Toast.makeText(c, "Can't show magic, trying again", Toast.LENGTH_SHORT).show();
            //Init the ad again
            createNewAd();
        }
    }

}
