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

package com.example.clover.loyalty.tender.activity;

import android.accounts.Account;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IInterface;
import android.support.annotation.Nullable;
import android.util.Log;
import com.clover.loyalty.ILoyaltyDataService;
import com.clover.loyalty.LoyaltyConnector;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v3.customers.CustomerInfo;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.clover.sdk.v3.payments.VasPushMode;
import com.clover.sdk.v3.payments.VasSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class BaseLoyaltyActivity extends Activity {
  public static final String LOG_TAG = BaseLoyaltyActivity.class.getSimpleName();
  private LoyaltyConnector connector;
  Executor executor = Executors.newSingleThreadExecutor();

  private Map<String, BroadcastReceiver> loyaltyServiceStateChangeReceiver = new HashMap<>();


  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeLoyaltyConnector();
  }

  private void initializeLoyaltyConnector() {
    if(connector == null) {
      Context context = this;

      Account account = null;
      ServiceConnector.OnServiceConnectedListener client = new ServiceConnector.OnServiceConnectedListener() {

        @Override
        public void onServiceConnected(ServiceConnector<? extends IInterface> serviceConnector) {
          Log.d(LOG_TAG, "Connected!");

          executor.execute(new Runnable(){
            @Override public void run() {
              try {
                final List<LoyaltyDataConfig> desiredDataConfig = connector.getDesiredDataConfig();
                runOnUiThread(new Runnable() {
                  @Override public void run() {
                    onLoyaltyDataLoaded(desiredDataConfig);
                  }
                });
              } catch (Exception e) {
                Log.e(LOG_TAG, "Error getting desired configs", e);
              }
            }
          });
        }

        @Override
        public void onServiceDisconnected(ServiceConnector<? extends IInterface> serviceConnector) {
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
  }

  BroadcastReceiver customerReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      if (Intents.ACTION_V1_CUSTOMER_IDENTIFIED.equals(intent.getAction())) {
        CustomerInfo customerInfo = intent.getParcelableExtra(Intents.EXTRA_CUSTOMERINFO);
        onCustomerSelected(customerInfo);
      }
    }
  };

  @Override protected void onStart() {
    super.onStart();
    registerReceiver(customerReceiver, new IntentFilter(Intents.ACTION_V1_CUSTOMER_IDENTIFIED));
  }

  @Override protected void onStop() {
    super.onStop();
    unregisterReceiver(customerReceiver);
  }

  /**
   * to be overridden by subclasses wanting to know when the loyalty config data is loaded
   * @param loyaltyDataConfigList
   */
  public abstract void onLoyaltyDataLoaded(List<LoyaltyDataConfig> loyaltyDataConfigList);

  /**
   *
   */
  public void onCustomerSelected(CustomerInfo customerInfo) {
    // no-op
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    for (BroadcastReceiver receiver : loyaltyServiceStateChangeReceiver.values()) {
      unregisterReceiver(receiver);
    }
    loyaltyServiceStateChangeReceiver.clear();
    if (connector != null) {
      connector.disconnect();
    }
  }

  /**
   * use to start a loyalty data service. e.g. VAS
   * @param type
   */
  public void start(final String type) {
    final Runnable runLater = new Runnable() {
      @Override public void run() {
        try {
          Log.d(LOG_TAG, String.format("Calling connector.start(%s)", type));

          String key = ILoyaltyDataService.Util.getServiceStateEventAction(type);
          if (loyaltyServiceStateChangeReceiver.get(key) != null) {
            BroadcastReceiver statusReceiver = new BroadcastReceiver() {
              @Override public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra(ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT);
                onLoyaltyServiceStateChanged(type, state);
              }
            };

            registerReceiver(statusReceiver, new IntentFilter(key));
          }

          String config = null;

          if ("VAS".equals(type)) {
            VasSettings vs = new VasSettings();
            vs.setPushMode(VasPushMode.PUSH_AND_GET);
            config = vs.getJSONObject().toString();
          }

          // For now, we can specify the set of dataExtras.  Maybe later we let others add to this?
          Map<String, String> dataExtras = addToLoyaltyServiceExtras(new HashMap<String, String>());
          connector.startLoyaltyService(type, dataExtras, config);
        } catch (Exception e) {
          Log.e(LOG_TAG, String.format("Error when starting service of type %s", type), e);
        }

      }
    };
    callConnector(runLater);
  }

  /**
   * Add information to the map of extra information in the VasSettings
   *
   * @param map - the possibly null map of extras
   * @return the non null map of extra data
   *
   * see com.clover.remote.terminal.kiosk.RemoteTerminalKioskActivity#addToVasExtras(java.util.Map, com.clover.sdk.v3.customers.CustomerInfo)
   */
  private Map<String, String> addToLoyaltyServiceExtras(Map<String, String> map) {
    // TODO: add CustomerInfo so we can get things like update URL for Apple PK and Google ST
    return ILoyaltyDataService.Util.addToLoyaltyServiceExtras(map, null, null);
  }

  /**
   * used to stop a loyalty data service e.g. VAS
   * @param type
   */
  public void stop(final String type) {
    final Runnable runLater = new Runnable() {
      @Override public void run() {
        try {
          Log.d(LOG_TAG, String.format("Calling connector.stop(%s)", type));
          connector.stopLoyaltyService(type);
          // when should/can we unregister the state receiver? After STOP is received?
        } catch (Exception e) {
          Log.e(LOG_TAG, "Ow!", e);
        }

      }
    };
    callConnector(runLater);
  }

  /**
   * Can be called by the Data service to indicate its status. e.g. VAS is/is not running
   * @param configType
   * @param state
   */
  public void onLoyaltyServiceStateChanged(String configType, String state) {
    Log.w(LOG_TAG, String.format("onLoyaltyServiceStateChanged: Service %s state updated to: %s", configType, state));
  }

  /**
   * Used by custom activities to announce loyalty data, collected by the custom activity
   * and put it in the loyalty platform.
   * @param loyaltyDataConfig
   * @param data
   */
  public void announceCustomerProvidedData(final LoyaltyDataConfig loyaltyDataConfig, final String data) {
    new AsyncTask<Void, Void, Void>() {
      @Override protected Void doInBackground(Void... voids) {
        final Runnable runLater = new Runnable() {
          @Override public void run() {
            //        LoyaltyDataConfig loyaltyDataConfig = new LoyaltyDataConfig();
            //        loyaltyDataConfig.setType(type); // This is a string.  We want to avoid enums, but will have some types by convention
            try {
              Log.d(LOG_TAG, "Calling connector.announceCustomerProvidedData");
              connector.announceCustomerProvidedData(loyaltyDataConfig, data);
            } catch (Exception e) {
              Log.e(LOG_TAG, "Ow!", e);
            }

          }
        };
        callConnector(runLater);
        return null;
      }
    }.execute();
  }

  private void callConnector(final Runnable runLater) {
    if (connector == null) {
      Context context = this;

      Account account = null;
      ServiceConnector.OnServiceConnectedListener client = new ServiceConnector.OnServiceConnectedListener() {
        Runnable delayedRun = runLater;

        @Override
        public void onServiceConnected(ServiceConnector<? extends IInterface> connector) {
          Runnable tempDelayedRun = delayedRun;
          if (tempDelayedRun != null) {
            delayedRun = null;
            // We could thread it, but we are already in a thread...?
            Log.d(LOG_TAG, "Calling delayedRun.run!");
            executor.execute(tempDelayedRun);
          }
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
    } else {
      // LoyaltyConnector is already initialized, just call the runnable.
      executor.execute(runLater);
    }
  }
}
