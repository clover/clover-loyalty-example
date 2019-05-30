/*
 * Copyright (C) 2018 Clover Network, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.clover.loyalty.tender.dataservice;

import android.accounts.Account;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import com.clover.loyalty.ILoyaltyDataService;
import com.clover.loyalty.LoyaltyConnector;
import com.clover.loyalty.LoyaltyDataTypes;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.clover.sdk.v3.scanner.BarcodeResult;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class QrBarcodeLoyaltyDataService extends Service implements ILoyaltyDataService {
  private static final String BARCODE_BROADCAST = "com.clover.BarcodeBroadcast";
  private static final String LOYALTY_DATA_TYPE = "QR_BARCODE";

  public static final String LOG_TAG = QrBarcodeLoyaltyDataService.class.getSimpleName();

  private Gson GSON = new Gson();

  public QrBarcodeLoyaltyDataService() {
    LoyaltyDataTypes.addListedType(getLoyaltyDataType());
  }

  private final BarcodeReceiver barcodeReceiver = new BarcodeReceiver();
  private boolean isRegistered = false;
  private LoyaltyConnector connector = null;

  @Override
  public void onCreate() {
    super.onCreate();
    initializeLoyaltyConnector();
  }

  private void registerBarcodeScanner() {
    if (!isRegistered) {
      isRegistered = true;
      registerReceiver(barcodeReceiver, new IntentFilter(BARCODE_BROADCAST));
      Log.d("registerBarcodeScanner", "");
    }
  }

  private void unregisterBarcodeScanner() {
    if (isRegistered) {
      isRegistered = false;
      unregisterReceiver(barcodeReceiver);
      Log.d(LOG_TAG, "unregisterBarcodeScanner");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public int onStartCommand (Intent intent,
      int flags,
      int startId) {
    Log.d(LOG_TAG, String.format("%s", intent));

    String runningFlag = intent == null ? ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING : intent.getStringExtra(ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT);

    // Not currently needed here, but added to show how the configuration is passed.
    String configuration = intent == null ? "" : intent.getStringExtra(EXTRA_LOYALTY_SERVICE_CONFIGURATION);
    // Not currently needed here, but added to show how additional makked pdata values are passed.
    Map<String, String> map = intent == null ? new HashMap<String, String>() : (Map<String, String>)intent.getSerializableExtra(ILoyaltyDataService.EXTRA_LOYALTY_SERVICE_DATA_VALUES);

    Log.d( LOG_TAG, String.format("configuration in start %s", configuration));

    if (ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_NOT_RUNNING.equals(runningFlag)) {
      stopScanner();
      stopSelf();
    } else if (ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING.equals(runningFlag)) {
      startScanner();
    }
    return super.onStartCommand(intent, flags, startId);
  }

  private void startScanner() {
    registerBarcodeScanner();
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void[] objects) {
        Log.d(LOG_TAG, "onBind starting scanner");
        try {
          Bundle extras = new Bundle();
          extras.putBoolean(Intents.EXTRA_LED_ON, false);
          connector.updateServiceState(getLoyaltyDataType(), LOYALTY_SERVICE_STATE_EVENT_RUNNING);
        } catch (Exception e) {
          Log.i(LOG_TAG, String.format("Error updating Barcode Loyalty Data Service state to %s", LOYALTY_SERVICE_STATE_EVENT_RUNNING), e);
        }
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(LOG_TAG, "Bound");
    return null;
  }

  public boolean onUnbind(Intent intent) {
    Log.d(LOG_TAG, "Unbound");
    return super.onUnbind(intent);
  }

  private void initializeLoyaltyConnector() {
    Context context = this;
    Account account = null;
    ServiceConnector.OnServiceConnectedListener client = new ServiceConnector.OnServiceConnectedListener() {
      @Override
      public void onServiceConnected(ServiceConnector<? extends IInterface> connector) {
        Log.d(LOG_TAG, "Connected!");
      }

      @Override
      public void onServiceDisconnected(ServiceConnector<? extends IInterface> connector) {
        Log.d(LOG_TAG, "Disconnected!");
      }
    };
    connector = new LoyaltyConnector(context, account, client) {
      @Override
      public void onBindingDied(ComponentName name) {
        Log.d(LOG_TAG, name + " onBindingDied");
      }
    };
    if (!connector.connect()) {
      Log.d(LOG_TAG, "Connect failed!!!!");
      connector = null;
    }
  }

  @Override
  public void onDestroy() {
    Log.d(LOG_TAG, "onDestroy");
    unregisterBarcodeScanner();
    stopScanner();
    connector.disconnect();
    super.onDestroy();
  }

  private void stopScanner() {
    unregisterBarcodeScanner();
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void[] objects) {
        //        mBarcodeScanner.executeStopScan(null);
        try {
          connector.updateServiceState(getLoyaltyDataType(), LOYALTY_SERVICE_STATE_EVENT_NOT_RUNNING);
        } catch (Exception e) {
          Log.i(LOG_TAG, String.format("Error updating Barcode Loyalty Data Service state to %s", LOYALTY_SERVICE_STATE_EVENT_NOT_RUNNING), e);
        }
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class BarcodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (BARCODE_BROADCAST.equals(action)) {
        final BarcodeResult barcodeResult = new BarcodeResult(intent);
        if (barcodeResult.getBarcode() != null) {
          final Barcode barcodeObj = new Barcode(barcodeResult.getBarcode(), barcodeResult.getType());
          final LoyaltyDataConfig config = generateAnnounceConfig();
          try {
            new AsyncTask<Void, Void, Void>() {
              @Override
              protected Void doInBackground(Void[] objects) {
                try {
                  connector.announceCustomerProvidedData(config, GSON.toJson(barcodeObj));
                  stopScanner();
                } catch (Exception e) {
                  Log.i(LOG_TAG, String.format("Error announcing Customer Provided Data (Barcode) %s", barcodeResult.getBarcode()), e);
                }
                return null;
              }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
          } catch (Exception e) {
            Log.i(LOG_TAG, String.format("Error announcing Barcode Loyalty Data"), e);
          }
        }
      }
    }
  }

  @SuppressWarnings("WeakerAccess")
  protected String getLoyaltyDataType() {
    return LOYALTY_DATA_TYPE;
  }

  @SuppressWarnings("WeakerAccess")
  protected LoyaltyDataConfig generateAnnounceConfig() {
    LoyaltyDataConfig config = new LoyaltyDataConfig();
    config.setType( getLoyaltyDataType() );
    return config;
  }

  /**
   * Used in serialization
   */
  class Barcode {
    final String barcodeValue;
    final String barcodeType;

    Barcode(String barcodeValue, String barcodeType) {
      this.barcodeValue = barcodeValue;
      this.barcodeType = barcodeType;
    }
  }
}
