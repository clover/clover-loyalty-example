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
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import com.clover.loyalty.ILoyaltyDataService;
import com.clover.loyalty.LoyaltyConnector;
import com.clover.loyalty.LoyaltyDataTypes;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MsrLoyaltyDataService extends Service implements ILoyaltyDataService {
  private static final String MSR_BROADCAST = "com.clover.intent.action.broadcast.CARD_SWIPED";
  private static final String LOYALTY_DATA_TYPE = "MSR";

  public static final String LOG_TAG = MsrLoyaltyDataService.class.getSimpleName();

  Executor executor = Executors.newSingleThreadExecutor();
  private Gson GSON = new Gson();

  //  private BarcodeScanner mBarcodeScanner;

  public MsrLoyaltyDataService() {
    LoyaltyDataTypes.addListedType(getLoyaltyDataType());
  }

  private final MsrReceiver msrBroadcastReceiver = new MsrReceiver();
  private boolean isRegistered = false;
  private LoyaltyConnector connector = null;

  @Override
  public void onCreate() {
    super.onCreate();
    initializeLoyaltyConnector();
  }

  private void registerBarcodeScanner() {

    if (!isRegistered) {
      registerReceiver(msrBroadcastReceiver, new IntentFilter("com.clover.intent.action.broadcast.CARD_SWIPED"));
      isRegistered = true;

      executor.execute(new Runnable(){
        @Override public void run() {
          try {
            connector.updateServiceState(getLoyaltyDataType(), ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING);
          } catch (RemoteException e) {
            e.printStackTrace();
          } catch (ClientException e) {
            e.printStackTrace();
          } catch (ServiceException e) {
            e.printStackTrace();
          } catch (BindingException e) {
            e.printStackTrace();
          }

        }
      });
    }
  }

  private void unregisterBarcodeScanner() {
    if (isRegistered) {
      isRegistered = false;
      unregisterReceiver(msrBroadcastReceiver);
      Log.d(LOG_TAG, "unregisterBarcodeScanner");
      executor.execute(new Runnable() {
        @Override public void run() {
          try {
            connector.updateServiceState(getLoyaltyDataType(), ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_NOT_RUNNING);
          } catch (RemoteException e) {
            e.printStackTrace();
          } catch (ClientException e) {
            e.printStackTrace();
          } catch (ServiceException e) {
            e.printStackTrace();
          } catch (BindingException e) {
            e.printStackTrace();
          }
        }
      });
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
        try {
          connector.updateServiceState(getLoyaltyDataType(), LOYALTY_SERVICE_STATE_EVENT_NOT_RUNNING);
        } catch (Exception e) {
          Log.i(LOG_TAG, String.format("Error updating Barcode Loyalty Data Service state to %s", LOYALTY_SERVICE_STATE_EVENT_NOT_RUNNING), e);
        }
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class MsrReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (MSR_BROADCAST.equals(action)) {

        String track1 = intent.getStringExtra("track1");
        String track2 = intent.getStringExtra("track2");
        String swipeTime = intent.getStringExtra("swipeTime");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("track1", track1);
        jsonObject.addProperty("track2", track2);
        jsonObject.addProperty("swipeTime", swipeTime);

        final LoyaltyDataConfig config = new LoyaltyDataConfig();
        final String payload = new Gson().toJson(jsonObject);
        config.setType("MSR");


        executor.execute(new Runnable() {
          @Override public void run() {
            try {
              connector.announceCustomerProvidedData(config, payload);
            } catch (RemoteException e) {
              e.printStackTrace();
            } catch (ClientException e) {
              e.printStackTrace();
            } catch (ServiceException e) {
              e.printStackTrace();
            } catch (BindingException e) {
              e.printStackTrace();
            }
          }
        });
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

}
