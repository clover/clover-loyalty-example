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

package com.example.clover.loyalty.tender.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.tender.Tender;
import com.clover.sdk.v1.tender.TenderConnector;
import com.clover.sdk.v3.customers.CustomerInfo;
import com.clover.sdk.v3.loyalty.CustomerProvidedDataResponse;
import com.clover.sdk.v3.loyalty.CustomerProvidedDataResponseType;
import com.clover.sdk.v3.loyalty.ILoyaltyServiceProvider;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;
import com.clover.sdk.v3.payments.Payment;
import com.example.clover.loyalty.tender.CustomerAccount;
import com.example.clover.loyalty.tender.LoyaltyHelper;
import com.example.clover.loyalty.tender.R;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExampleLoyaltyService extends Service {

  public static final String LOYALTY_DATA_CONFIG_MSR = "MSR";
  public static final String LOYALTY_DATA_CONFIG_QRCODE_BARCODE = "QR_BARCODE";

  private IBinder binder = new ExampleLoyaltyServiceBinder();

  public static final String TAG = ExampleLoyaltyService.class.getSimpleName();


  @Nullable @Override public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    return super.onStartCommand(intent, flags, startId);
  }


  class ExampleLoyaltyServiceBinder extends ILoyaltyServiceProvider.Stub {

    public ExampleLoyaltyServiceBinder() {
      new AsyncTask<Void, Void, Void>() {
        @Override protected Void doInBackground(Void... voids) {
          createTenderType(getApplicationContext());
          registerOrderListener(getApplicationContext());

          return null;
        }
      }.execute();
    }

    @Override public CustomerProvidedDataResponse onCustomerProvidedData(String type, LoyaltyDataConfig loyaltyDataConfig, String data) throws RemoteException {
      String customerId = null;
      if (LOYALTY_DATA_CONFIG_MSR.equals(loyaltyDataConfig.getType())) {
        // get track 2 from data
        JsonObject jsonObject = new Gson().fromJson(data, JsonObject.class);
        customerId = jsonObject.get("track2").getAsString();
      } else if (LOYALTY_DATA_CONFIG_QRCODE_BARCODE.equals(loyaltyDataConfig.getType())) {
        //
        JsonObject jsonObject = new Gson().fromJson(data, JsonObject.class);
        JsonElement barcodeValue = jsonObject.get("barcodeValue");
        if (barcodeValue != null) {
          customerId = barcodeValue.getAsString();
        }
      }

      if (customerId != null) {
        Map<String, CustomerAccount> stringCustomerAccountMap = LoyaltyHelper.getAllAccounts(ExampleLoyaltyService.this);

        CustomerAccount customerAccount = stringCustomerAccountMap.get(customerId);
        if (customerAccount == null) {
          customerAccount = new CustomerAccount(customerId, "Customer " + stringCustomerAccountMap.values().size(), "", "", "", 0);
          LoyaltyHelper.saveAccount(ExampleLoyaltyService.this, customerAccount);
        }

        CustomerInfo customerInfo = LoyaltyHelper.getCustomerInfo(customerAccount);
        Intent intent = new Intent(Intents.ACTION_V1_CUSTOMER_IDENTIFIED);

        intent.putExtra(Intents.EXTRA_CUSTOMERINFO, customerInfo);
        sendBroadcast(intent);

      } else {
        // do nothing...probably got something we didn't expect
      }

      CustomerProvidedDataResponse cpdr = new CustomerProvidedDataResponse();
      cpdr.setResponseType(CustomerProvidedDataResponseType.ACCEPTED);
      return cpdr;
    }

    @Override public List<LoyaltyDataConfig> getLoyaltyDataConfigOfInterest() throws RemoteException {
      List<LoyaltyDataConfig> configs = new ArrayList<>();
      addMsr(configs);
      addBarcode(configs);
      return configs;
    }

    private void addBarcode(List<LoyaltyDataConfig> configs) {
      LoyaltyDataConfig config = new LoyaltyDataConfig();
      config.setType(LOYALTY_DATA_CONFIG_QRCODE_BARCODE);
      configs.add(config);
    }

    private void addMsr(List<LoyaltyDataConfig> configs) {
      LoyaltyDataConfig config = new LoyaltyDataConfig();
      config.setType(LOYALTY_DATA_CONFIG_MSR);
      configs.add(config);
    }

    private void registerOrderListener(final Context context) {
      new AsyncTask<Void, Void, Exception>() {
        @Override protected void onPreExecute() {
          super.onPreExecute();
          orderConnector = new OrderConnector(context, CloverAccount.getAccount(context), null);
          orderConnector.connect();
        }

        @Override protected Exception doInBackground(Void... voids) {
          orderConnector.addOnOrderChangedListener(mOnOrderUpdateListener);
          return null;
        }
      }.execute();

    }



    /*
     * This makes sure the custom tender is registered with Clover
     * @param context
     */
    private void createTenderType(final Context context) {
      new AsyncTask<Void, Void, Exception>() {

        private TenderConnector tenderConnector;

        @Override
        protected void onPreExecute() {
          super.onPreExecute();
          tenderConnector = new TenderConnector(context, CloverAccount.getAccount(context), null);
          if(tenderConnector.connect()) {
            Log.i(TAG, "got connected to TenderConnector");
          } else {
            Log.i(TAG, "did not get connected to TenderConnector");
          }
        }

        @Override
        protected Exception doInBackground(Void... params) {
          try {
            Tender tender = tenderConnector.checkAndCreateTender(context.getString(R.string.tender_name), context.getPackageName(), true, false); // initialization

            Log.i(TAG, String.format("Tender created: %s", tender.getLabel()));
          } catch (Exception exception) {
            Log.e(TAG, exception.getMessage(), exception.getCause());
            return exception;
          }
          return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
          tenderConnector.disconnect();
          tenderConnector = null;
        }
      }.execute();
    }

    private Executor executor = Executors.newSingleThreadExecutor();
    private OrderConnector orderConnector = null;

    /*
     * this listens for order changes, specifically when a payment gets added to an order. We look up in the
     * Loyalty helper to see what customer is associated to this order, then increment, or decrement based on
     * the payment tender type. i.e. credit or cash, credit the loyalty account, pay with points should decrement
     * the points.
     */
    private OrderConnector.OnOrderUpdateListener2 mOnOrderUpdateListener = new OrderConnector.OnOrderUpdateListener2() {
      @Override public void onOrderUpdated(final String orderId, boolean selfChange) {}
      @Override public void onOrderCreated(String orderId) {}
      @Override public void onOrderDeleted(String orderId) {}
      @Override public void onOrderDiscountAdded(String orderId, String discountId) {}
      @Override public void onOrderDiscountsDeleted(String orderId, List<String> discountIds) {}
      @Override public void onLineItemsAdded(String orderId, List<String> lineItemIds) {}
      @Override public void onLineItemsUpdated(String orderId, List<String> lineItemIds) {}
      @Override public void onLineItemsDeleted(String orderId, List<String> lineItemIds) {}
      @Override public void onLineItemModificationsAdded(String orderId, List<String> lineItemIds, List<String> modificationIds) {}
      @Override public void onLineItemDiscountsAdded(String orderId, List<String> lineItemIds, List<String> discountIds) {}
      @Override public void onLineItemExchanged(String orderId, String oldLineItemId, String newLineItemId) {}
      @Override public void onRefundProcessed(String orderId, String refundId) {}
      @Override public void onCreditProcessed(String orderId, String creditId) {}

      @Override
      public void onPaymentProcessed(final String orderId, final String paymentId) {
        executor.execute(new Runnable(){
          @Override public void run() {
            try {
              Order order = orderConnector.getOrder(orderId);
              // see if we have a customer associated with this order
              CustomerAccount customerAccount = LoyaltyHelper.getCustomerAccountForOrderId(ExampleLoyaltyService.this, orderId);
              if (customerAccount == null) {
                return;
              }
              //          List<Customer> customers = order.getCustomers(); // if the customer was a Clover customer, this could work, but the tender activity would have to set the customer on the order

              for (Payment payment : order.getPayments()) {
                if (payment.getId().equals(paymentId)) {
                  if("com.clover.tender.credit_card".equals(payment.getTender().getLabelKey())) {
                    customerAccount.points += payment.getAmount() / 10 / 100; //10% of dollars spent
                  } else if ("com.example.clover.loyalty.tender".equals(payment.getTender().getLabelKey())){
                    customerAccount.points -= Math.ceil(payment.getAmount() / 100.0);
                  } else {
                    Log.i(TAG, String.format("Tender labelKey: %s", payment.getTender().getLabelKey()));
                  }
                }
              }

              if(LoyaltyHelper.saveAccount(ExampleLoyaltyService.this, customerAccount)) {
                Log.i(TAG, "Points awarded to customer.");
              }

              // TODO: if order is closed, remove order <-> customer map
              //

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

    };




  }


}
