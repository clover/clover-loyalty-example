package com.clover.loyalty.example.pos;

import com.clover.loyalty.LoyaltyDataTypes;
import com.clover.loyalty.example.pos.accounts.CustomerAccounts;
import com.clover.loyalty.example.pos.model.POSDiscount;
import com.clover.loyalty.example.pos.utils.SecurityUtils;
import com.clover.remote.client.CloverConnectorFactory;
import com.clover.remote.client.CloverDeviceConfiguration;
import com.clover.remote.client.DefaultCloverConnectorListener;
import com.clover.remote.client.ICloverConnector;
import com.clover.remote.client.ICloverConnectorListener;
import com.clover.remote.client.MerchantInfo;
import com.clover.remote.client.USBCloverDeviceConfiguration;
import com.clover.remote.client.WebSocketCloverDeviceConfiguration;
import com.clover.remote.client.messages.ConfirmPaymentRequest;
import com.clover.remote.client.messages.CustomActivityRequest;
import com.clover.remote.client.messages.CustomActivityResponse;
import com.clover.remote.client.messages.CustomerProvidedDataEvent;
import com.clover.remote.client.messages.DataProviderConfig;
import com.clover.remote.client.messages.MessageFromActivity;
import com.clover.remote.client.messages.MessageToActivity;
import com.clover.remote.client.messages.RegisterForCustomerProvidedDataRequest;
import com.clover.remote.client.messages.SaleRequest;
import com.clover.remote.client.messages.SaleResponse;
import com.clover.remote.client.messages.SetCustomerInfoRequest;
import com.clover.remote.client.messages.TipMode;
import com.clover.remote.order.DisplayLineItem;
import com.clover.remote.order.DisplayOrder;
import com.clover.sdk.v3.customers.Customer;
import com.clover.sdk.v3.customers.CustomerInfo;
import com.clover.sdk.v3.customers.PhoneNumber;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;

public class CloverLoyaltyPOSActivity extends Activity implements ClearCustomerIntf {

  static final String EXAMPLE_POS_SERVER_KEY = "clover_device_endpoint";
  static final String EXTRA_CLOVER_CONNECTOR_CONFIG = "EXTRA_CLOVER_CONNECTOR_CONFIG";
  static final String EXTRA_WS_ENDPOINT = "WS_ENDPOINT";
  static final String EXTRA_CLEAR_TOKEN = "CLEAR_TOKEN";
  static final String EXTRA_ENABLE_VAS = "ENABLE_VAS";
  static final String EXTRA_ENABLE_QUICK_PAY = "ENABLE_QUICK_PAY";
  static final String EXTRA_ENABLE_PHONE = "ENABLE_PHONE";
  static final String EXTRA_ENABLE_ID = "ENABLE_ID";
  static final String EXTRA_ENABLE_BARCODE = "ENABLE_BARCODE";
  private static final String EXTRA_USB_CONFIG = "USB";
  private static final String EXTRA_WEBSOCKET_CONFIG = "WS";
  static final String EXTRA_SERVER_IP = "SERVER_IP";
  static final String EXTRA_SERVER_PORT = "SERVER_PORT";
  private static final String PREF_HOST_URL = "HOST_URL";
  private static final String SHARED_PREFERENCES = "CloverLoyaltyExample";
  private static final String CUSTOM_ACTIVITY = "com.clover.remote.clover.loyalty.CloverLoyaltyCustomActivity";
  private static final String CUSTOM_TIP_ACTIVITY = "com.clover.remote.clover.loyalty.CloverExampleCustomTipActivity";

  private static final String POS_APP_NAME = "Clover Loyalty Example POS";
  private static final String POS_VERSION_NUMBER = "3.0.0";
  private static final String POS_SERIAL_NUMBER = "Aisle 3";
  private static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT";
  private static final String TAG = CloverLoyaltyPOSActivity.class.getSimpleName();
  private AlertDialog pairingCodeDialog;
  private String url;
  private SharedPreferences sharedPreferences;
  private ICloverConnector iCloverConnector;
  private final LoyaltyHelper loyaltyHelper = new LoyaltyHelper(this);

  private static CustomerInfo customerInfo;
  static String serverIp;
  static String serverPort;
  private DisplayOrder displayOrder;

  private boolean enableQuickpay, enableVas, enablePhone, enableId, enableBarcode;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_clover_loyalty_pos);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
    url = sharedPreferences.getString(PREF_HOST_URL, getString(R.string.lan_pay_address));

    String applicationId = POS_APP_NAME + POS_VERSION_NUMBER;
    CloverDeviceConfiguration config;

    String configType = getIntent().getStringExtra(EXTRA_CLOVER_CONNECTOR_CONFIG);
    if (EXTRA_USB_CONFIG.equals(configType)) {
      config = new USBCloverDeviceConfiguration(this, applicationId);
    } else if (EXTRA_WEBSOCKET_CONFIG.equals(configType)) {
      String authToken;

      URI uri = (URI) getIntent().getSerializableExtra(EXTRA_WS_ENDPOINT);

      Uri androidUri = Uri.parse(uri.toString());
      authToken = androidUri.getQueryParameter("authenticationToken");

      try {
        uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),uri.getPort(), uri.getPath(), null,uri.getFragment());
      } catch (Exception e) {
        Log.e(TAG, "Error extracting query information from uri.", e);
        setResult(RESULT_CANCELED);
        finish();
        return;
      }

      // NOTE:  At the moment, we are always loading our certs from resources.  Opened JIRA SEMI-2147 to
      // add capability to load from the network endpoints dynamically.  Will need to refactor this code
      // to pull network access off the main thread though...
      KeyStore trustStore = SecurityUtils.createTrustStore(true);

      if(authToken == null) {
        boolean clearToken = getIntent().getBooleanExtra(EXTRA_CLEAR_TOKEN, false);
        if (!clearToken) {
          authToken = sharedPreferences.getString("AUTH_TOKEN", null);
        }
      }
      config = new WebSocketCloverDeviceConfiguration(uri, applicationId, trustStore, POS_APP_NAME, POS_SERIAL_NUMBER, authToken) {
        @Override
        public int getMaxMessageCharacters() {
          return 0;
        }

        @Override
        public void onPairingCode(final String pairingCode) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              // If we previously created a dialog and the pairing failed, reuse
              // the dialog previously created so that we don't get a stack of dialogs
              if (pairingCodeDialog != null) {
                pairingCodeDialog.setMessage("Enter pairing code: " + pairingCode);
              } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(CloverLoyaltyPOSActivity.this);
                builder.setTitle("Pairing Code");
                builder.setMessage("Enter pairing code: " + pairingCode);
                pairingCodeDialog = builder.create();
              }
              pairingCodeDialog.show();
            }
          });
        }

        @Override
        public void onPairingSuccess(String authToken) {
          Preferences.userNodeForPackage(CloverLoyaltyPOSActivity.class).put("AUTH_TOKEN", authToken);
          sharedPreferences.edit().putString("AUTH_TOKEN", authToken).apply();
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (pairingCodeDialog != null && pairingCodeDialog.isShowing()) {
                pairingCodeDialog.dismiss();
                pairingCodeDialog = null;
              }
            }
          });
        }
      };
    } else {
      finish();
      return;
    }

    enableQuickpay = getIntent().getBooleanExtra(EXTRA_ENABLE_QUICK_PAY, true);
    enableVas = getIntent().getBooleanExtra(EXTRA_ENABLE_VAS, true);
    enablePhone = getIntent().getBooleanExtra(EXTRA_ENABLE_PHONE, true);
    enableId = getIntent().getBooleanExtra(EXTRA_ENABLE_ID, true);
    enableBarcode = getIntent().getBooleanExtra(EXTRA_ENABLE_BARCODE, true);

    serverIp = getIntent().getStringExtra(EXTRA_SERVER_IP);
    serverPort = getIntent().getStringExtra(EXTRA_SERVER_PORT);
    iCloverConnector = CloverConnectorFactory.createICloverConnector(config);
    ICloverConnectorListener cloverConnectorListener = new CloverConnectorListener(iCloverConnector);
    iCloverConnector.addCloverConnectorListener(cloverConnectorListener);
    iCloverConnector.initializeConnection();

    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

    MainFragment mainFragment = MainFragment.newInstance();
    fragmentTransaction.add(R.id.rootContainer, mainFragment, MAIN_FRAGMENT_TAG);
    fragmentTransaction.commit();
  }

  public void disconnectClicked(View view) {
    if (iCloverConnector != null) {
      iCloverConnector.dispose();
      iCloverConnector = null;

      log("Disconnected and Disposed");
      finish();
    }
  }

  private void updateUIOnDisconnect() {
    updateUI(false);
  }
  private void updateUIOnConnect() {
    updateUI(true);
  }

  private void updateUI(final boolean connected) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        // update any ui components as needed...
      }
    });

  }

  public void startCustomActivityClicked(View view) {
    startCustomActivity();
  }

  /**
   * start the custom activity, with no extra parameters
   */
  private void startCustomActivity() {
    if (iCloverConnector != null) {
      CustomActivityRequest car = new CustomActivityRequest(CUSTOM_ACTIVITY);
      // this allows other activities to start and will stop this activity.. i.e. doSale()
      // if nonBlocking is false, then you would have to send a "finish" type request to the custom
      // activity using sendMessageToActivity and have the activity finish itself before
      // processing a payment.
      car.setNonBlocking(true);

      iCloverConnector.startCustomActivity(car);
    }
  }

  /**
   * start the custom tip activity, with data for initialization.
   *
   */
  private void startCustomTipActivity() {
    CustomActivityRequest car = new CustomActivityRequest(CUSTOM_TIP_ACTIVITY);
    // this allows other activities to start and will stop this activity.. i.e. doSale()
    // if nonBlocking is false, then you would have to send a "finish" type request to the custom
    // activity using sendMessageToActivity and have the activity finish itself before
    // processing a payment.
    car.setNonBlocking(true);

    // Create a payload configuration for the activity.
    // The Activity must understand how to deserialize this payload to make use of it!
    //
    // The example creates a set of tip configuration objects for the activity.
    CustomTipConfigurationMessage customTipConfigurationMessage = buildCustomTipConfigurationMessage();
    car.setPayload(new Gson().toJson(customTipConfigurationMessage));

    iCloverConnector.startCustomActivity(car);
  }


  /**
   * Build a set of tip configurations for the custom tip activity.
   *
   * This example sets up percentage and static amount tip configurations.
   * The example uses this configuration, but can decide how to handle them
   * on its own.
   *
   * @return a set of configurations that are understood by the example tip screen.
   */
  private CustomTipConfigurationMessage buildCustomTipConfigurationMessage() {
    ArrayList<CustomTipConfiguration> tipConfigs = new ArrayList<>();
    tipConfigs.add(new CustomTipConfiguration(true, 15L));
    tipConfigs.add(new CustomTipConfiguration(true, 18L));
    tipConfigs.add(new CustomTipConfiguration(true, 20L));
    tipConfigs.add(new CustomTipConfiguration(true, 30L));

    // Add a couple static tip values (one can always hope for large tips!)
    tipConfigs.add(new CustomTipConfiguration(false, 10L));
    tipConfigs.add(new CustomTipConfiguration(false, 20L));
    return
        new CustomTipConfigurationMessage(getSaleAmount(), tipConfigs);
  }

  public void clearCustomerClicked(View view) {
    clearCustomer();
  }

  @Override
  public void clearCustomer() {
    if (customerInfo != null && customerInfo.getCustomer() != null) {
      LoyaltyServiceHelper.queryService("/customer/clear/" + customerInfo.getCustomer().getId());
    }
    customerInfo = null;
    iCloverConnector.setCustomerInfo(new SetCustomerInfoRequest());
  }

  public void saleWithTipClicked(View view) {
    startCustomTipActivity();
  }

  public void saleClicked(View view) {
    saleWithTip(null);
  }

  private void saleWithTip(Long tip) {
    SaleRequest saleRequest = new SaleRequest(getSaleAmount(), UUID.randomUUID().toString().replace("-", ""));
    //saleRequest.setSignatureEntryLocation(DataEntryLocation.NONE);
    saleRequest.setAutoAcceptSignature(true);
    saleRequest.setAutoAcceptPaymentConfirmations(true);
    saleRequest.setDisableReceiptSelection(true);
    // Set the tip if it is sent in
    if (null != tip) {
      saleRequest.setTipMode(TipMode.TIP_PROVIDED);
      saleRequest.setTipAmount(tip);
    }
    iCloverConnector.sale(saleRequest);
  }

  private long getSaleAmount() {
    EditText saleAmountView = findViewById(R.id.saleAmount);
    String saleAmountText = saleAmountView.getText().toString();
    return (int)(Float.valueOf(saleAmountText) * 100);
  }

  public void resetDeviceClicked(View view) {
    iCloverConnector.resetDevice();
  }

  public void sendOrderClicked(View view) {
    sendOrder();
  }

  /**
   * just tests using the sendMessageToActivity capability
   */
  private void sendOrder() {
    if (displayOrder == null) {
      // just create a test order to send over to verify the send message to activity
      displayOrder = new DisplayOrder();
      List<DisplayLineItem> displayLineItems = new ArrayList<>();
      DisplayLineItem dli1 = new DisplayLineItem();
      dli1.setName("Cheeseburger");
      dli1.setQuantity("1");
      dli1.setPrice("$7.99");
      displayLineItems.add(dli1);
      DisplayLineItem dli2 = new DisplayLineItem();
      dli2.setName("French Fries (Large)");
      dli2.setQuantity("1");
      dli2.setPrice("$2.19");
      displayLineItems.add(dli2);
      displayOrder.setLineItems(displayLineItems);
      displayOrder.setTotal("$10.18");
    }

    JsonObject jsonObject = new JsonObject();
    String displayOrderString = displayOrder.getJSONObject().toString();
    jsonObject.addProperty("displayOrder", displayOrderString);
    MessageToActivity msg = new MessageToActivity(CUSTOM_ACTIVITY, new Gson().toJson(jsonObject));
    iCloverConnector.sendMessageToActivity(msg);
  }

  private void log(final String msg) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("MAIN_FRAGMENT");
        if(fragment != null){
          View view = fragment.getView();
          if (view != null) {
            TextView textView = view.findViewById(R.id.outputText);
            if (textView != null) {
              textView.getEditableText().insert(0, msg + "\n\n");
            }
          }}
      }
    });
  }

  public void clearLog(View view) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("MAIN_FRAGMENT");
        if(fragment != null){
          View view = fragment.getView();
          if (view != null) {
            TextView textView = view.findViewById(R.id.outputText);
            if (textView != null) {
              textView.setText("");
            }
          }}
      }
    });
  }

  class CloverConnectorListener extends DefaultCloverConnectorListener {
    CloverConnectorListener(ICloverConnector cc) {
      super(cc);
    }

    @Override public void onDeviceDisconnected() {
      log("Disconnected");
      updateUIOnDisconnect();
    }

    @Override public void onDeviceConnected() {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(CloverLoyaltyPOSActivity.this, "Device Connected", Toast.LENGTH_LONG).show();
        }
      });
    }

    @Override public void onDeviceReady(MerchantInfo merchantInfo) {
      updateUIOnConnect();

      // now that we have a valid connection, let's register for customer provided data. e.g. phone number, vas, customer id, etc.
      List<DataProviderConfig> configs = new ArrayList<>();
      if (enableId) {
        loyaltyHelper.addInterestInAccountNumber(configs); // add config for account number (account id)
      }
      if (enableBarcode) {
        loyaltyHelper.addInterestInBarCode(configs); // add config for barcode scanner
      }
      if (enablePhone) {
        loyaltyHelper.addInterestInPhone(configs); // add config for phone number
      }
      if (enableQuickpay) {
        loyaltyHelper.addInterestInQuickPay(configs); // add config for phone number
      }
      if (enableVas) {
        loyaltyHelper.addInterestInVasPk(configs); // add config for Apple clover.pass
        loyaltyHelper.addInterestInVasST(configs); // add config for Google SmartTap
      }
      loyaltyHelper.addInterestInClear(configs); // add config for clearing a customer

      RegisterForCustomerProvidedDataRequest request = new RegisterForCustomerProvidedDataRequest();
      request.setConfigurations(configs);

      iCloverConnector.registerForCustomerProvidedData(request);
    }

    @Override public void onCustomActivityResponse(final CustomActivityResponse response) {
      log(response.getAction() + " finished");
    }

    @Override public void onMessageFromActivity(final MessageFromActivity message) {
      log(message.getAction() + " => " + message.getPayload());

      // If the message is from the example custom tip activity...
      if (isMessageFromActivity(message, CUSTOM_TIP_ACTIVITY)) {
        // In this implementation, any message from the custom tip activity is treated as
        // a tip confirmation and we immediately start the payment process.
        Gson gson = new Gson();
        CustomTipSelectedMessage value = gson.fromJson(message.getPayload(), CustomTipSelectedMessage.class);
        saleWithTip(value.tipAmount);
      }
    }

    private boolean isMessageFromActivity(MessageFromActivity message, String activityName) {
      return message.getAction() != null && message.getAction().startsWith(activityName);
    }

    /**
     * handle the custom provided data from the Mini. Since we register for multiple types,
     * we have to look at the config type to determine how to handle the data.
     * @param event the data from the customer
     */
    @Override public void onCustomerProvidedData(final CustomerProvidedDataEvent event) {
      log(event.getEventId() + " : " + event.getConfig().getType() + " : " + event.getData() );

      if(LoyaltyDataTypes.CLEAR_TYPE.equals(event.getConfig().getType())) {
        clearCustomer();
      } else {

        final List<CustomerAccounts.CustomerAccount> customerAccounts = loyaltyHelper.process(event, CloverLoyaltyPOSActivity.this);
        if (null != customerAccounts) {
          log("Found " + customerAccounts.size() + " accounts.");
          Log.d(TAG, String.format("customerAccounts %s", customerAccounts));
        }

        if (customerAccounts != null && customerAccounts.size() == 1) {
          final CustomerAccounts.CustomerAccount acct = customerAccounts.get(0);

          // Create a customer object and populate as desired
          Customer customer = new Customer();
          customer.setFirstName(acct.name);
          customer.setPhoneNumbers(new ArrayList<PhoneNumber>(){{
            PhoneNumber pn = new PhoneNumber();
            pn.setPhoneNumber(acct.phone);
            add(pn);
          }});
          customer.setId(String.valueOf(acct.id));
          // create a map of extras of "proprietary" data
          Map<String, String> extras = new HashMap<>();
          extras.put("POINTS", String.format("%d", acct.points));
          CustomerAccounts.Offer[] offers = new CustomerAccounts.Offer[]{new CustomerAccounts.Offer("O1", "5% Off", "5% off", 0, null, new POSDiscount()) };
          extras.put("OFFERS", new Gson().toJson(offers));

          // create a CustomerInfo object
          customerInfo = new CustomerInfo();
          customerInfo.setCustomer(customer);
          customerInfo.setExternalId(acct.uuid);

          customerInfo.setExtras(extras);
          customerInfo.setDisplayString("Welcome back " + acct.name);

          // create the request to send
          SetCustomerInfoRequest request = new SetCustomerInfoRequest();
          request.setCustomerInfo(customerInfo);

          cloverConnector.setCustomerInfo(request);

          log("Sent customer: " + acct.uuid);
        } else {
          log("Didn't send a customer");
        }
      }

    }

    @Override public void onSaleResponse(final SaleResponse response) {
      log("Sale was successful: " + response.isSuccess());

      if (customerInfo != null) {
        if (response.getPayment() != null) {
          // update our mock user account
          loyaltyHelper.addPointsToCustomer((int) Math.floor(response.getPayment().getAmount() / 100f), customerInfo.getExternalId());
          String updatedCustomer = LoyaltyServiceHelper.queryService("/customer/update/" + customerInfo.getCustomer().getId() + "/" + ((int) Math.floor(response.getPayment().getAmount() / 100f)));
          Map map = new Gson().fromJson(updatedCustomer, Map.class);
          String points = map.get("points").toString();

          Map extras = new HashMap();
          extras.put("POINTS", String.format("%d", Double.valueOf(points).intValue()));
          CustomerAccounts.Offer[] offers = new CustomerAccounts.Offer[]{new CustomerAccounts.Offer("O1", "5% Off", "5% off", 0, null, new POSDiscount())};
          extras.put("OFFERS", new Gson().toJson(offers));
          customerInfo.setExtras(extras);
          SetCustomerInfoRequest request = new SetCustomerInfoRequest();
          request.setCustomerInfo(customerInfo);
          cloverConnector.setCustomerInfo(request);
        } else {
          log("Sale was not successful. Will not send updated customer");
        }
      } else {
        log("no customer info in successful sale.");
      }
    }
    @Override public void onConfirmPaymentRequest(ConfirmPaymentRequest request) {
      // don't prompt for payment challenges, just accept them.
      cloverConnector.acceptPayment(request.getPayment());
    }

  }

  /*
  Note: these classes should be in a shared library with the example custom tip screen.
    We have not done this for simplicity.
   */
  public class CustomTipSelectedMessage {
    final Long tipAmount;

    public CustomTipSelectedMessage(Long tipAmount) {
      this.tipAmount = tipAmount;
    }
  }

  public class CustomTipConfigurationMessage {
    final Long amountToBaseTipOn;
    final ArrayList<CustomTipConfiguration> tipConfigs;

    CustomTipConfigurationMessage(Long amountToBaseTipOn, ArrayList<CustomTipConfiguration> tipConfigs) {
      this.amountToBaseTipOn = amountToBaseTipOn;
      this.tipConfigs = tipConfigs;
    }
  }

  public class CustomTipConfiguration {
    final boolean percentage;
    final Long value;

    CustomTipConfiguration(boolean isPercentage, Long value) {
      percentage = isPercentage;
      this.value = value;
    }
  }
}