package com.clover.loyalty.example.cloverloyalty;

import android.app.Application;
import android.content.Context;
//import android.support.multidex.MultiDex;

public class CloverLoyaltyApplication extends Application {
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    android.support.multidex.MultiDex.install(this);
  }
}
