package com.clover.loyalty.example.cloverloyalty;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.clover.loyalty.LoyaltyDataTypes;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;
import com.clover.loyalty.example.cloverloyalty.providers.CustomerAccountsSQLiteOpenHelper;
import com.clover.sdk.v3.loyalty.CustomerProvidedDataResponse;
import com.clover.sdk.v3.loyalty.CustomerProvidedDataResponseType;
import com.clover.sdk.v3.loyalty.ILoyaltyServiceProvider;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.clover.sdk.v3.payments.VasDataTypeType;
import com.clover.sdk.v3.payments.VasProtocol;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloverLoyaltyService extends Service {

  private static final String LOG_TAG = "CloverLoyaltyService";
  private static final String ANNOUNCE_DATA_BROADCAST = "COM.CLOVER.LOYALTY.TEST.ANNOUNCE_DATA_BROADCAST";
  private static final String UUID_EXTRA = ANNOUNCE_DATA_BROADCAST + "UUID";
  private static final String CONFIG_EXTRA = ANNOUNCE_DATA_BROADCAST + "CONFIG";
  private static final String DATA_EXTRA = ANNOUNCE_DATA_BROADCAST + "DATA";

  Field phoneField;
  Field uuidField;
  Field idField;

  private static final Gson GSON;
  private CustomerAccountsSQLiteOpenHelper dbHelper;

  {
    try {
      phoneField = CustomerAccounts.CustomerAccount.class.getField("phone");
      uuidField = CustomerAccounts.CustomerAccount.class.getField("uuid");
      idField = CustomerAccounts.CustomerAccount.class.getField("id");
    } catch(Exception e) {

    }
  }

  static {
    GsonBuilder builder = new GsonBuilder();
    GSON = builder.create();
  }

  private IBinder binder = new CloverLoyaltyServiceBinder();


  public class CloverLoyaltyServiceBinder extends ILoyaltyServiceProvider.Stub {
    @Override
    public CustomerProvidedDataResponse onCustomerProvidedData(String uuid, LoyaltyDataConfig loyaltyDataConfig, String data) {
      CustomerProvidedDataResponse response = new CustomerProvidedDataResponse();
      response.setResponseType(CustomerProvidedDataResponseType.ACCEPTED);
      Log.d(LOG_TAG, String.format("For loyaltyDataConfig: uuid is %s, %s data is %s",
          uuid, loyaltyDataConfig.getJSONObject().toString(), data));

      // Transferring the data to a UI element or some other thing would occur here.
      // Since this a simple example, I am just broadcasting.
      //      Intent intent = new Intent(ANNOUNCE_DATA_BROADCAST);
      //      intent.putExtra(UUID_EXTRA, uuid);
      //      intent.putExtra(CONFIG_EXTRA, loyaltyDataConfig);
      //      intent.putExtra(DATA_EXTRA, data);
      //      CloverLoyaltyService.this.sendBroadcast(intent);

      if(LoyaltyDataTypes.PHONE_TYPE.equals(loyaltyDataConfig.getType())) {
        List<CustomerAccounts.CustomerAccount> customerAccounts = dbHelper.getCustomerAccountsByPhone(data);//getAccountWrapper().getItems(phoneField, data);
        if(customerAccounts.size() == 1) {
          // Found it!!!!
          // broadcast it?
          // start activity with selected customer?
          Intent intent = new Intent(CloverLoyaltyService.this.getApplicationContext(), CloverLoyaltyActivity.class);
          intent.putExtra("CUSTOMER", customerAccounts.get(0));
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          CloverLoyaltyService.this.startActivity(intent);
        } else {
          Intent intent = new Intent(CloverLoyaltyService.this.getApplicationContext(), CloverLoyaltyActivity.class);
          intent.putExtra("PHONE", data);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          CloverLoyaltyService.this.startActivity(intent);
        }

      } else if (LoyaltyDataTypes.VAS_TYPE.equals(loyaltyDataConfig.getType())) {
        Log.d(getClass().getSimpleName(), "onCustomerProvidedData: " + data);

        JsonObject jObj = new Gson().fromJson(data, JsonObject.class);
        String vasPayloadString = jObj.get("vasPayload").getAsString();
        JsonObject jsonObject = new Gson().fromJson(vasPayloadString, JsonObject.class);
        JsonObject payloadElements = jsonObject.getAsJsonObject("payloadElements");
        JsonArray elements = payloadElements.getAsJsonArray("elements");
        // in test, elements has 8
        String account = elements.get(0).getAsJsonObject().get("vasData").getAsString();

        List<CustomerAccounts.CustomerAccount> customerAccounts = dbHelper.getCustomerAccountsByUUID(account);//getAccountWrapper().getItems(uuidField, account);
        if(customerAccounts.size() == 1) {
          // Found it!!!!
          // broadcast it?
          // start activity with selected customer?
          Intent intent = new Intent(CloverLoyaltyService.this.getApplicationContext(), CloverLoyaltyActivity.class);
          intent.putExtra("CUSTOMER", customerAccounts.get(0));
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          CloverLoyaltyService.this.startActivity(intent);
        } else {
          Intent intent = new Intent(CloverLoyaltyService.this.getApplicationContext(), CloverLoyaltyActivity.class);
          intent.putExtra("PHONE", "");
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          CloverLoyaltyService.this.startActivity(intent);
        }
      } else if ("com.loyalty.AccountNumber".equals(loyaltyDataConfig.getType())) {
        String accountNumber = data;
        List<CustomerAccounts.CustomerAccount> customerAccounts = dbHelper.getCustomerAccountsById(accountNumber);//getAccountWrapper().getItems(idField, accountNumber);
        if(customerAccounts.size() == 1) {
          // Found it!!!!
          // broadcast it?
          // start activity with selected customer?
          Intent intent = new Intent(CloverLoyaltyService.this.getApplicationContext(), CloverLoyaltyActivity.class);
          intent.putExtra("CUSTOMER", customerAccounts.get(0));
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          CloverLoyaltyService.this.startActivity(intent);
        } else {
          Intent intent = new Intent(CloverLoyaltyService.this.getApplicationContext(), CloverLoyaltyActivity.class);
          intent.putExtra("PHONE", "");
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          CloverLoyaltyService.this.startActivity(intent);
        }
      }

      return response;
    }


    @Override
    public List<LoyaltyDataConfig> getLoyaltyDataConfigOfInterest() {
      List<LoyaltyDataConfig> loyaltyDataConfigs = new ArrayList<>();

      addInterestInCustomLoyaltyType(loyaltyDataConfigs);
      addInterestInPhoneLoyaltyType(loyaltyDataConfigs);
      addInterestInVasST(loyaltyDataConfigs);
      addInterestInVasPk(loyaltyDataConfigs);
      addInterestInBarcode(loyaltyDataConfigs);
      Log.d(LOG_TAG, String.format("For loyaltyDataConfigs: %s",
          loyaltyDataConfigs));
      return loyaltyDataConfigs;
    }

    /**
     * This adds interest in the pre-defined VAS loyalty type.  It has additional information in the
     * "configuration" that can be used by the provider of the information to filter the data collected.
     * <p>
     * The interpretation of the information is done by the provider of the data.  The filtering in this case is
     * within the announcement mechanism.
     *
     * @param loyaltyDataConfigs - the set of configurations to add to
     */
    private void addInterestInVasPk(List<LoyaltyDataConfig> loyaltyDataConfigs) {
      try {
        LoyaltyDataConfig loyaltyDataConfig = new LoyaltyDataConfig();
        loyaltyDataConfig.setType(LoyaltyDataTypes.VAS_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
        Map<String, String> configuration = new HashMap<>();
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROVIDER_PACKAGE, "com.clover.loyalty.CLE");
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROTOCOL_ID, VasProtocol.PK.toString());
        Map<String, String> protocolConfig = new HashMap<>();
        protocolConfig.put("pkPassTypeId", "pass.clover.customer");
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROTOCOL_CONFIG, GSON.toJson(protocolConfig));
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_URL, "av.clover.com");
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_TITLE, "Test AV Push Url");
        loyaltyDataConfig.setConfiguration(configuration);
        loyaltyDataConfigs.add(loyaltyDataConfig);
      } catch (Exception e) {
        Log.e(LOG_TAG, "", e);
      }
    }

    /**
     * This adds interest in the pre-defined VAS loyalty type.  It has additional information in the
     * "configuration" that can be used by the provider of the information to filter the data collected.
     * <p>
     * The interpretation of the information is done by the provider of the data.  The filtering in this case is
     * within the announcement mechanism.
     *
     * @param loyaltyDataConfigs - the set of configurations to add to
     */
    private void addInterestInVasST(List<LoyaltyDataConfig> loyaltyDataConfigs) {
      try {
        LoyaltyDataConfig loyaltyDataConfig = new LoyaltyDataConfig();
        loyaltyDataConfig.setType(LoyaltyDataTypes.VAS_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
        Map<String, String> configuration = new HashMap<>();
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROVIDER_PACKAGE, "com.clover.loyalty.CLE");
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROTOCOL_ID, VasProtocol.ST.toString());
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.SUPPORTED_SERVICES,
            GSON.toJson(Collections.singletonList(VasDataTypeType.ALL.toString())));
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_URL, "st.clover.com");
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_TITLE, "Test ST Push Url");
        loyaltyDataConfig.setConfiguration(configuration);
        loyaltyDataConfigs.add(loyaltyDataConfig);
      } catch (Exception e) {
        Log.e(LOG_TAG, "", e);
      }
    }

    /**
     * This adds interest in a custom loyalty type named "Test".
     * <p>
     * This illustrates how a loyalty configuration can be created to listen for any
     * custom type of data that is collected.
     *
     * @param loyaltyDataConfigs - the set of configurations to add to
     */
    private void addInterestInPhoneLoyaltyType(List<LoyaltyDataConfig> loyaltyDataConfigs) {
      try {
        // look at com.clover.loyalty.LoyaltyBinderImpl.filterPayload() to see how we tie announcement to consumption
        LoyaltyDataConfig loyaltyDataConfig = new LoyaltyDataConfig();
        loyaltyDataConfig.setType(LoyaltyDataTypes.PHONE_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
        Map<String, String> configuration = new HashMap<>();
        //        configuration.put("ConfigKey1", "ConfigVal1");
        loyaltyDataConfig.setConfiguration(configuration);
        loyaltyDataConfigs.add(loyaltyDataConfig);
      } catch (Exception e) {
        Log.e(LOG_TAG, "", e);
      }

    }

    private void addInterestInCustomLoyaltyType(List<LoyaltyDataConfig> loyaltyDataConfigs) {
      try {
        // look at com.clover.loyalty.LoyaltyBinderImpl.filterPayload() to see how we tie announcement to consumption
        LoyaltyDataConfig loyaltyDataConfig = new LoyaltyDataConfig();
        loyaltyDataConfig.setType("com.loyalty.AccountNumber"); // This is a string.  We want to avoid enums, but will have some types by convention
        Map<String, String> configuration = new HashMap<>();
        configuration.put("barcode", "true");
        configuration.put("qrcode", "true");
        configuration.put("ui", "true");
        loyaltyDataConfig.setConfiguration(configuration);
        loyaltyDataConfigs.add(loyaltyDataConfig);
      } catch (Exception e) {
        Log.e(LOG_TAG, "", e);
      }
    }

    private void addInterestInBarcode(List<LoyaltyDataConfig> loyaltyDataConfigs) {
      try {
        // look at com.clover.loyalty.LoyaltyBinderImpl.filterPayload() to see how we tie announcement to consumption
        LoyaltyDataConfig loyaltyDataConfig = new LoyaltyDataConfig();
        loyaltyDataConfig.setType("BARCODE"); // This is a string.  We want to avoid enums, but will have some types by convention
        Map<String, String> configuration = new HashMap<>();
        loyaltyDataConfig.setConfiguration(configuration);
        loyaltyDataConfigs.add(loyaltyDataConfig);
      } catch (Exception e) {
        Log.e(LOG_TAG, "", e);
      }
    }

    /*public CustomerAccountsAdapter.AccountWrapper getAccountWrapper() {
//      final ArrayList<CustomerAccounts.CustomerAccount> customerAccountList = new ArrayList<>();
      CustomerAccountsAdapter.AccountWrapper wrapper = new CustomerAccountsAdapter.AccountWrapper();
      List<CustomerAccounts.CustomerAccount> accounts = new ArrayList<>();
      accounts.add(new CustomerAccounts.CustomerAccount("0", "44XBAPX69H", "Name 0", "7195551212", 20, Collections.<CustomerAccounts.AccountTransaction>emptyList()));
      for (int i = 1; i <= 5; i++) {
        CustomerAccounts.CustomerAccount customer = CustomerAccounts.createDummyCustomer(i, null);
        accounts.add(customer);
      }
      wrapper.addAll(accounts, phoneField);
      wrapper.addAll(accounts, uuidField);
      wrapper.addAll(accounts, idField);
      return wrapper;
    }*/
  }

  @Override
  public void onCreate() {
    Log.d(LOG_TAG, "I'm alive!!!!");
    dbHelper = new CustomerAccountsSQLiteOpenHelper(this);
    super.onCreate();
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(LOG_TAG, "I'm bound!!!!");
    return binder;
  }
}

