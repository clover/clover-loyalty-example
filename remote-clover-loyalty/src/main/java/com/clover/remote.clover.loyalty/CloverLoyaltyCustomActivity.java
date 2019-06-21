package com.clover.remote.clover.loyalty;

import com.clover.cfp.activity.CFPConstants;
import com.clover.cfp.activity.CloverLoyaltyCFPActivity;
import com.clover.connector.sdk.v3.CardEntryMethods;
import com.clover.loyalty.ILoyaltyDataService;
import com.clover.loyalty.LoyaltyDataTypes;
import com.clover.remote.order.DisplayOrder;
import com.clover.sdk.v3.base.Reference;
import com.clover.sdk.v3.customers.Address;
import com.clover.sdk.v3.customers.Card;
import com.clover.sdk.v3.customers.Customer;
import com.clover.sdk.v3.customers.CustomerInfo;
import com.clover.sdk.v3.customers.CustomerMetadata;
import com.clover.sdk.v3.customers.EmailAddress;
import com.clover.sdk.v3.customers.PhoneNumber;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.clover.sdk.v3.payments.VasDataType;
import com.clover.sdk.v3.payments.VasDataTypeType;
import com.clover.sdk.v3.payments.VasMode;
import com.clover.sdk.v3.payments.VasPushMode;
import com.clover.sdk.v3.payments.VasSettings;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloverLoyaltyCustomActivity extends CloverLoyaltyCFPActivity implements NumberPadFragment.NumberPadFragmentListener {

  private String TAG = getClass().getSimpleName();
  private ListView displayOrderList;

  private TextView phoneNumberField;

  private List<LoyaltyDataConfig> loyaltyDataConfigList = new ArrayList<>();
  private LoyaltyDataConfig emailConfig = null;
  private LoyaltyDataConfig phoneConfig = null;
  private LoyaltyDataConfig accountNumberConfig = null;
  private LoyaltyDataConfig vasConfig = null;
  private LoyaltyDataConfig quickPayConfig = null;
  private LoyaltyDataConfig barCodeConfig = null;
  private LoyaltyDataConfig clearConfig = null;
  private CustomerInfo customerInfo;
  private DisplayOrder displayOrder;
  private String number = "";
  private Gson gson = new GsonBuilder().serializeNulls().create();
  private boolean shouldStop = true;
  Button forceStopButton;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_custom_loyalty);

    displayOrderList = findViewById(R.id.display_order_list);
    phoneNumberField = findViewById(R.id.entered_phone_number);

    // should also be able to get these from onLoyaltyDataLoaded call back
    customerInfo = getIntent().getParcelableExtra(CFPConstants.CUSTOMER_INFO_EXTRA);
    displayOrder = getIntent().getParcelableExtra(CFPConstants.DISPLAY_ORDER_EXTRA);

    updateDisplayOrder();

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    findViewById(R.id.account_number_field).setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          if (null != imm) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
          } else {
            Log.e(TAG, "getSystemService(Context.INPUT_METHOD_SERVICE) returned null! Cannot hideSoftInputFromWindow");
          }
        }
      }
    });

    forceStopButton = findViewById(R.id.forceStopButton);
    forceStopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (shouldStop) {
          shouldStop = false;
          stop(LoyaltyDataTypes.QUICKPAY_TYPE, true);
        } else {
          shouldStop = true;
          startQuickPay();
        }

      }
    });

    TextView notYouTextView = findViewById(R.id.not_you_text_view);
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    ssb.append(notYouTextView.getText());
    ssb.setSpan(new URLSpan("#"), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    notYouTextView.setText(ssb, TextView.BufferType.SPANNABLE);

    if (getApplicationContext().getResources().getBoolean(R.bool.isFlex)) {
      Button showPhone = findViewById(R.id.choose_phone_number_button);
      showPhone.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          findViewById(R.id.phone_number_component).setVisibility(View.VISIBLE);
          findViewById(R.id.no_customer_panel).setVisibility(View.GONE);
          findViewById(R.id.vas_enabled).setVisibility(View.INVISIBLE);
          findViewById(R.id.quick_pay_enabled).setVisibility(View.INVISIBLE);
          findViewById(R.id.order_panel).setVisibility(View.GONE);
        }
      });
    }

    findViewById(R.id.not_you_text_view).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        clearCustomer(v);
      }
    });

    Log.d(TAG, "CloverLoyaltyCustomActivity: it loaded!");
  }

  @Override protected void onResume() {
    super.onResume();
    if (quickPayConfig != null || vasConfig != null) {
      findViewById(R.id.tap_enabled).setVisibility(View.VISIBLE);
      if (quickPayConfig != null) {
        findViewById(R.id.quick_pay_enabled).setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... voids) {
            startQuickPay();
            return null;
          }
        }.execute();
      } else if (vasConfig != null) {
        findViewById(R.id.vas_enabled).setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... voids) {
            startVas();
            return null;
          }
        }.execute();
      }
      if (barCodeConfig != null) {
        findViewById(R.id.barcode_enabled).setVisibility(View.VISIBLE);
      }
    }
  }

  @Override protected void onPause() {
    super.onPause();
    if (quickPayConfig != null || vasConfig != null) {
      findViewById(R.id.tap_enabled).setVisibility(View.GONE);
      if (vasConfig != null) {
        findViewById(R.id.vas_enabled).setVisibility(View.INVISIBLE);
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... voids) {
            stop(LoyaltyDataTypes.VAS_TYPE);
            return null;
          }
        }.execute();
      }
      if (quickPayConfig != null) {
        findViewById(R.id.quick_pay_enabled).setVisibility(View.INVISIBLE);
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... voids) {
            stop(LoyaltyDataTypes.QUICKPAY_TYPE);
            return null;
          }
        }.execute();
      }
    }
    if (barCodeConfig != null) {
      findViewById(R.id.barcode_enabled).setVisibility(View.GONE);
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {
          stop("BARCODE");
          return null;
        }
      }.execute();
    }
  }

    /*
     *  This gets called out of super.onCreate() after configs are retrieved from clover-loyalty service.
     *  We hold on to the data configs we are aware of, so we can update the ui and show/hide
     *  the appropriate components. Phone, acct number and barcode are "collected" by this activity,
     *  so we will need those configs to announce that we collected customer identifying information
     *
     */
  @Override public void onLoyaltyDataLoaded(List<LoyaltyDataConfig> loyaltyDataConfigList, CustomerInfo customerInfo, DisplayOrder displayOrder) {
    Log.d(TAG, "onLoyaltyDataLoaded " + loyaltyDataConfigList);
    this.loyaltyDataConfigList = loyaltyDataConfigList;
    emailConfig = null;
    phoneConfig = null;
    accountNumberConfig = null;
    vasConfig = null;
    barCodeConfig = null;
    clearConfig = null;

    for (LoyaltyDataConfig config : loyaltyDataConfigList) {
      Log.d(TAG, "onLoyaltyDataLoaded: type: " + config.getType());
      if (LoyaltyDataTypes.EMAIL_TYPE.equals(config.getType())) {
        emailConfig = config;
      } else if (LoyaltyDataTypes.PHONE_TYPE.equals(config.getType())) {
        phoneConfig = config;
      } else if ("com.loyalty.AccountNumber".equals(config.getType())) {
        accountNumberConfig = config;
      } else if (LoyaltyDataTypes.VAS_TYPE.equals(config.getType())) {
        vasConfig = config;
        startVas();
      } else if (LoyaltyDataTypes.QUICKPAY_TYPE.equals(config.getType())) {
        Log.d(TAG, "Starting: " + LoyaltyDataTypes.QUICKPAY_TYPE);
        forceStopButton.setVisibility(View.VISIBLE);
        quickPayConfig = config;
        startQuickPay();

      } else if ("BARCODE".equals(config.getType())) {//com.clover.loyalty.barcode.BarCodeLoyaltyDataService#LOYALTY_DATA_TYPE
        barCodeConfig = config;
      } else if (LoyaltyDataTypes.CLEAR_TYPE.equals(config.getType())) {
        clearConfig = config;
      }
    }
    Log.d(TAG, "loyaltyDataConfigList: " + loyaltyDataConfigList);

    sendRegistrationConfigs();
    updateCustomerPanel(customerInfo);
    updateDisplayOrder();
  }

  private void startQuickPay() {
    // Build the quickpay configuration
    Map<String, String> configuration = new HashMap<>();

    // Accept all card entry methods.  For quickpay this is tap/chip insert/swipe
    configuration.put(LoyaltyDataTypes.QUICKPAY_TYPE_KEYS.QUICKPAY_CARD_ENTRY_METHODS, String.valueOf(CardEntryMethods.ALL));

    // Build the set of application specific values.  This is a additional configuration for payments.
    Map<String, String> applicationSpecificValues = new HashMap<>();
    applicationSpecificValues.put("ExampleAppKey1", "ExampleAppValue1");
    configuration.put(LoyaltyDataTypes.QUICKPAY_TYPE_KEYS.QUICKPAY_APP_SPECIFIC_MAP, gson.toJson(applicationSpecificValues));

    // Set up/create the embedded VAS settings.
    // Note that if we are running vas and QuickPay, then this configuration will take precedence if it is
    // started first.
    // TODO: We probably need to avoid using the Vas* classes directly here.
    VasSettings vs = new VasSettings();
    // VAS_OR_PAYMENT will stop and return vas data if available otherwise proceed to pay.
    // VAS_AND_PAYMENT will try for both in one step and not stop if vas is found
    vs.setVasMode(VasMode.VAS_AND_PAYMENT);
    // vs.setVasMode(VasMode.VAS_OR_PAYMENT);
    vs.setPushMode(VasPushMode.PUSH_AND_GET);
    List<VasDataType> serviceTypes = new ArrayList<>();
    VasDataType serviceType = new VasDataType();
    serviceType.setDataType(VasDataTypeType.ALL);
    // Potential service types are ALL, LOYALTY, OFFER, GIFT_CARD, PRIVATE_LABEL_CARD, CUSTOMER, VAS_DATA;
    serviceTypes.add(serviceType);
    vs.setServiceTypes(serviceTypes);
    String embeddedVasConfig = vs.getJSONObject().toString();
    // Add the VAS configuration to the quickPay configuration so we can start both simultaneously.
    // In a perfect world, we would be able to start either one first, and not affect the other, but
    // that is not the case here.
    // the secure board does not elegantly allow for a service to be added, not to mention adding a
    // configuration with a VasMode.VAS_ONLY while the quickPay service is running makes little sense.
    configuration.put(LoyaltyDataTypes.QUICKPAY_TYPE_KEYS.QUICKPAY_VAS_CONFIG, embeddedVasConfig);

    start(LoyaltyDataTypes.QUICKPAY_TYPE, null, gson.toJson(configuration));
  }

  private void startVas() {
    // Build the VAS configuration.
    // TODO: We probably need to avoid using the Vas* classes directly here.
    Map<String, String> configuration = new HashMap<>();
    // Set up/create the embedded VAS settings
    VasSettings vs = new VasSettings();
    // Only do VAS, no payments
    vs.setVasMode(VasMode.VAS_ONLY);
    vs.setPushMode(VasPushMode.PUSH_AND_GET);
    List<VasDataType> serviceTypes = new ArrayList<>();
    VasDataType serviceType = new VasDataType();
    serviceType.setDataType(VasDataTypeType.ALL);
    serviceTypes.add(serviceType);
    vs.setServiceTypes(serviceTypes);
    // String vasSettings = gson.toJson(vs.getJSONObject().toString());
    // Results in W/String  "{\"serviceTypes\":{\"elements\":[{\"dataType\":\"ALL\"}]},\"vasMode\":\"VAS_AND_PAYMENT\",\"pushMode\":\"PUSH_AND_GET\"}"
    // If we try to deserialize this in any normal fashion (settings = gson.fromJson(configuration, VasSettings.class)),
    // we get
    // W/String  (28763): com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected BEGIN_OBJECT
    // but was STRING at line 1 column 2 path $
    //
    // so we are forced to deserialize like this:
    // settings = new VasSettings(configuration);
    String vasSettings = vs.getJSONObject().toString();

    start(LoyaltyDataTypes.VAS_TYPE, null, vasSettings);
  }

  @Override
  public void onLoyaltyServiceStateChanged(String configType, String state) {
    Log.w(TAG, "onLoyaltyServiceStateChanged: Getting state: " + state + " for config type: " + configType, null);

    // listening for changes to services so we can update the ui appropriately, like hiding/showing the tap nfc image
    if (LoyaltyDataTypes.VAS_TYPE.equals(configType)) {
      // Get the visibility state for the one item.
      int visiblity = ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING.equals(state) ? View.VISIBLE : View.INVISIBLE;
      // We might change the visibility of the view that contains the two items.
      View tapEnabled = findViewById(R.id.tap_enabled);
      // If the QuickPAy item is visible, then leave the container visible, otherwise set it to the same as this item
      tapEnabled.setVisibility(findViewById(R.id.quick_pay_enabled).getVisibility() == View.VISIBLE ? View.VISIBLE : visiblity);
      findViewById(R.id.vas_enabled).setVisibility(visiblity);
    } else if (LoyaltyDataTypes.QUICKPAY_TYPE.equals(configType)) {
      // Get the visibility state for the one item.
      int visiblity = ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING.equals(state) ? View.VISIBLE : View.INVISIBLE;
      // We might change the visibility of the view that contains the two items.
      View tapEnabled = findViewById(R.id.tap_enabled);
      // If the VAS item is visible, then leave the container visible, otherwise set it to the same as this item
      tapEnabled.setVisibility(findViewById(R.id.vas_enabled).getVisibility() == View.VISIBLE ? View.VISIBLE : visiblity);
      findViewById(R.id.quick_pay_enabled).setVisibility(visiblity);
    } else if ("BARCODE".equals(configType)) {
      findViewById(R.id.barcode_enabled).setVisibility(ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING.equals(state) ? View.VISIBLE : View.GONE);
    } else {
      Log.w(TAG, String.format("onLoyaltyServiceStateChanged: state: %s unknown for config type: %s", state, configType));
    }
  }

  @Override
  public void onSessionDataChanged(String key, Object data) {
    // get update that the customer has been sent from the POS
    if (CFPConstants.CUSTOMER_INFO_EXTRA.equals(key)) {
      Log.d(TAG, String.format("onSessionDataChanged: Got a customer key %s, data %s: ", key, data));
      customerInfo = (CustomerInfo) data;
      sendCustomerInfo(customerInfo);
      Log.d("SendCustomerInfo: ", "from onSessionDataChanged");
      // We do not want to stop VAS here, because it will stop quickPay as well.
      // stop(LoyaltyDataTypes.VAS_TYPE);
      updateCustomerPanel(customerInfo);
    }
  }

  @Override
  public void onSessionEvent(String type, String data) {

  }

  @Override
  protected void onMessage(String s) {
    // this is a custom message that will finish the activity if a "finish" message is sent
    // by the pos
    if ("finish".equals(s)) {
      setResultAndFinish(RESULT_OK, null);
    } else {
      try {
        JsonObject jsonObject = gson.fromJson(s, JsonObject.class);
        if (jsonObject.has("displayOrder")) {
          // the displayOrder message demonstrates updating an order on the custom activity
          if (jsonObject.get("displayOrder") != null && !"null".equals(jsonObject.get("displayOrder").getAsString())) {
            displayOrder = new DisplayOrder(jsonObject.get("displayOrder").getAsString());
            updateDisplayOrder();
          } else {
            displayOrder = null;
            updateDisplayOrder();
          }
        } else if (jsonObject.has("command")) {
          // the command object is used for testing the loyalty API by loopback
          if (jsonObject.get("command") != null) {
            automateReply(jsonObject.get("command").getAsString(), jsonObject);
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "onMessage: Error parsing display order: " + s, e);
      }
    }
  }

  private Map.Entry<Boolean, String> parseCustomerInfo(final CustomerInfo customerInfo) {
    boolean error = false;
    try {
      String displayString = customerInfo.getDisplayString();
      String externalId = customerInfo.getExternalId();
      String externalSystemName = customerInfo.getExternalSystemName();
      Map<String, String> extras = customerInfo.getExtras();

      JsonObject customerJson = new JsonObject();
      if (customerInfo.getCustomer() != null) {
        Customer cust = customerInfo.getCustomer();
        String id = cust.getId();
        Reference merchant = cust.getMerchant();
        String firstName = cust.getFirstName();
        String lastName = cust.getLastName();
        Boolean marketingAllowed = cust.getMarketingAllowed();
        Long customerSince = cust.getCustomerSince();
        List<Reference> orders = cust.getOrders();
        List<Address> addresses = cust.getAddresses();
        List<EmailAddress> emailAddress = cust.getEmailAddresses();
        List<PhoneNumber> phoneNumbers = cust.getPhoneNumbers();
        List<Card> cards = cust.getCards();
        CustomerMetadata customerMetadata = cust.getMetadata();

        customerJson.addProperty("id", id);
        customerJson.addProperty("firstName", firstName);
        customerJson.addProperty("lastName", lastName);
        customerJson.addProperty("customerSince", customerSince);
      }

      JsonObject customerInfoJson = new JsonObject();
      customerInfoJson.addProperty("displayString", displayString);
      customerInfoJson.addProperty("externalId", externalId);
      customerInfoJson.addProperty("externalSystemName", externalSystemName);
      customerInfoJson.addProperty("extras", gson.toJson(extras));
      customerInfoJson.add("customer", customerJson);

      return new java.util.AbstractMap.SimpleEntry<>(error, gson.toJson(customerInfoJson));
    } catch (Exception e) {
      Log.d(TAG, "parseCustomerInfo: Exception: " + e.getMessage(), e);
      error = true;
      return new java.util.AbstractMap.SimpleEntry<>(error, e.getMessage());
    }
  }

  /* *************************************
  /* actions taken by a customer to provide data. e.g. phone number
  /* *************************************/

  public void sendPhoneNumber(View view) {
    String pn = number;
    // announcing we captured a phone number
    announceCustomerProvidedData(phoneConfig, pn);
  }

  public void sendAccountNumber(View view) {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    if (null != imm) {
      imm.hideSoftInputFromWindow(findViewById(R.id.account_number_field).getWindowToken(), 0);
    } else {
      Log.e(TAG, "getSystemService(Context.INPUT_METHOD_SERVICE) returned null! Cannot hideSoftInputFromWindow");
    }
    if (accountNumberConfig == null) {
      Log.e(getClass().getSimpleName(), "sendAccountNumber: AccountNumber Config not loaded", null);
      return;
    }
    final String accountNumber = ((EditText) findViewById(R.id.account_number_field)).getText().toString();
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        announceCustomerProvidedData(accountNumberConfig, accountNumber);
        return null;
      }
    }.execute();

  }

  public void clearCustomer(View view) {
    announceCustomerProvidedData(clearConfig, "");
    if (getApplicationContext().getResources().getBoolean(R.bool.isFlex)) {
      findViewById(R.id.phone_number_component).setVisibility(View.GONE);
      findViewById(R.id.order_panel).setVisibility(View.VISIBLE);
    }

  }

  @Override
  public void onNumberButton(String tag) {
    try {
      BUTTON button = BUTTON.valueOf(tag);
      number = button.process(number);

      if (number.length() < 11) {
        phoneNumberField.setText((number.length() == 0) ? "000.000.0000" : formatNumber(number));
      }
    } catch (IllegalArgumentException iae) {
      Log.e(getClass().getSimpleName(), "Couldn't parse value: " + tag, iae);
    }
  }

  public void startBarCode(View view) {
    start("BARCODE", null, null);
  }

  private void updateCustomerPanel(final CustomerInfo customerInfo) {
    boolean hasCustomerInfo = customerInfo != null;
    boolean hasCustomer = hasCustomerInfo && customerInfo.getCustomer() != null;
    boolean hasExtras = hasCustomerInfo && customerInfo.getExtras() != null;
    boolean hasOrder = displayOrder != null;
    // update the ui based on whether we have a customer identified or not.
    // this could also be handled by a separate custom activity, but in this example
    // a single activity collects info, and display the customer info
    findViewById(R.id.no_customer_panel).setVisibility(hasCustomerInfo ? View.GONE : View.VISIBLE);
    findViewById(R.id.customer_panel).setVisibility(hasCustomerInfo ? View.VISIBLE : View.GONE);
    if (!getApplicationContext().getResources().getBoolean(R.bool.isFlex)) {
      findViewById(R.id.phone_number_component).setVisibility((!hasCustomerInfo && phoneConfig != null) ? View.VISIBLE : View.GONE);
    }
    findViewById(R.id.account_number_component).setVisibility((!hasCustomerInfo && accountNumberConfig != null) ? View.VISIBLE : View.GONE);
    findViewById(R.id.vas_enabled).setVisibility((!hasCustomerInfo && vasConfig != null) ? View.VISIBLE : View.GONE);
    findViewById(R.id.barcode_enabled).setVisibility((!hasCustomerInfo && barCodeConfig != null) ? View.VISIBLE : View.GONE);

    if (hasCustomerInfo && hasCustomer) {
      ((TextView) findViewById(R.id.welcome_message)).setText(customerInfo.getCustomer().getFirstName());
    } else {
      ((TextView) findViewById(R.id.welcome_message)).setText("");
    }

    if (hasOrder) {
      ((TextView) findViewById(R.id.order_total_label)).setText(displayOrder.getTotal());
    } else {
      ((TextView) findViewById(R.id.order_total_label)).setText(R.string.zero_amt);
    }

    ((LinearLayout) findViewById(R.id.offers_panel)).removeAllViews();

    ((TextView) findViewById(R.id.customer_account_points)).setText("");
    if (hasCustomer && hasExtras) {
      // building the right panel if we have a customer
      String points = customerInfo.getExtras().get("POINTS");
      ((TextView) findViewById(R.id.customer_account_points)).setText(points);

      // offers are a custom payload between the integrator and the custom activity
      String offers = customerInfo.getExtras().get("OFFERS");
      JsonArray obj = gson.fromJson(offers, JsonArray.class);

      for (int i = 0; i < obj.size(); i++) {
        Button btn = new Button(new ContextThemeWrapper(this, R.style.discountButton), null, 0);
        JsonObject jsonObject = (JsonObject) obj.get(i);
        String val = jsonObject.get("label").getAsString();
        final String id = jsonObject.get("id").getAsString();
        btn.setText(val);


        btn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            JsonObject sendObject = new JsonObject();
            sendObject.addProperty("customerUUID", customerInfo.getExternalId());
            sendObject.addProperty("offerId", id);
            try {
              sendMessage(gson.toJson(sendObject));
            } catch (Exception e) {
              e.printStackTrace();
            }

            findViewById(R.id.offers_panel).setVisibility(View.GONE);
          }
        });

        findViewById(R.id.offers_panel).setVisibility(View.VISIBLE);
        ((LinearLayout) findViewById(R.id.offers_panel)).addView(btn);
      }
    } else {
      findViewById(R.id.no_customer_panel).setVisibility(View.VISIBLE);
      findViewById(R.id.customer_panel).setVisibility(View.GONE);
    }

    if (!hasCustomerInfo && vasConfig != null) {
      startVas();
    }
  }

  private void updateDisplayOrder() {
    ((TextView) findViewById(R.id.order_total_label)).setText(displayOrder == null ? "" : displayOrder.getTotal());
    displayOrderList.setAdapter(new DisplayOrderAdapter(getBaseContext(), displayOrder));

  }

  /* *************************************
  /* utility methods
  /* *************************************/

  private String formatNumber(String number) {
    if (number.length() == 10) {
      return number.substring(0, 3) + "." + number.substring(3, 6) + "." + number.substring(6);
    } else if (number.length() == 7) {
      return number.substring(0, 3) + "." + number.substring(3);
    } else {
      return number;
    }
  }

  private enum BUTTON {
    ONE {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "1";
        } else {
          return currentNumber;
        }
      }
    },
    TWO {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "2";
        } else {
          return currentNumber;
        }
      }
    },
    THREE {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "3";
        } else {
          return currentNumber;
        }
      }
    },
    FOUR {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "4";
        } else {
          return currentNumber;
        }
      }
    },
    FIVE {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "5";
        } else {
          return currentNumber;
        }
      }
    },
    SIX {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "6";
        } else {
          return currentNumber;
        }
      }
    },
    SEVEN {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "7";
        } else {
          return currentNumber;
        }
      }
    },
    EIGHT {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "8";
        } else {
          return currentNumber;
        }
      }
    },
    NINE {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "9";
        } else {
          return currentNumber;
        }
      }
    },
    ZERO {
      @Override
      String process(String currentNumber) {
        if (currentNumber.length() < 10) {
          return currentNumber + "0";
        } else {
          return currentNumber;
        }
      }
    },
    BACK {
      @Override
      String process(String currentNumber) {
        return currentNumber.substring(0, Math.max(0, currentNumber.length() - 1));
      }
    },
    CLEAR {
      @Override
      String process(String currentNumber) {
        return "";
      }
    };

    String process(String currentNumber) {
      return currentNumber;
    }
  }



  /* *************************************
  /* test methods
  /*
  /* these methods are used to provide loopback functionality of data objects to facilitate
  /* testing of the parsing of these objects.
  /*
  /* *************************************/

  private void automateReply(String command, JsonObject jsonObject){
    switch (command){
      case "SendCustomerProvidedData":
        sendCustomerProvidedData(jsonObject);
        break;
      case "SendCustomerInfo":
        sendCustomerInfo(customerInfo);
        break;
    }
  }

  private void sendCustomerProvidedData(JsonObject object){
    if(object.get("config") != null) {
      JsonObject configObject = object.get("config").getAsJsonObject();
      LoyaltyDataConfig config = new LoyaltyDataConfig(gson.toJson(configObject));
      announceCustomerProvidedData(config, object.get("data") != null ? object.get("data").getAsString() : null);
    }
  }

  private void sendRegistrationConfigs() {
    JsonObject SendRegistrationConfigs = new JsonObject();
    JsonArray configObject = new JsonArray();

    SendRegistrationConfigs.addProperty("type", "LoyaltyRegistrationConfigs" );
    for (LoyaltyDataConfig dataConfig : loyaltyDataConfigList) {
      JsonObject loyaltyConfig = new JsonObject();
      loyaltyConfig.addProperty("type", dataConfig.getType());
      if(dataConfig.getType().equals(LoyaltyDataTypes.VAS_TYPE)){
        loyaltyConfig.addProperty("configs", gson.toJson(dataConfig.getConfiguration()));
      }
      configObject.add(loyaltyConfig);
    }
    SendRegistrationConfigs.add("configs", configObject);
    try {
      sendMessage(gson.toJson(SendRegistrationConfigs));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendCustomerInfo(final CustomerInfo customerInfo) {
    JsonObject SendCustomerInfo = new JsonObject();
    SendCustomerInfo.addProperty("type" , "CustomerInfo");
    if (customerInfo != null) {
      Map.Entry<Boolean, String> entry = parseCustomerInfo(customerInfo);
      Boolean error = entry.getKey();
      String customerInfoString = entry.getValue();
      SendCustomerInfo.addProperty("error", error);
      SendCustomerInfo.addProperty("customerInfo", customerInfoString);
      Log.d(TAG, "sendCustomerInfo: " + customerInfoString);
      if (customerInfo.getCustomer() != null) {
        SendCustomerInfo.addProperty("firstName", customerInfo.getCustomer().getFirstName());
      }
    } else {
      Log.d(TAG, "sendCustomerInfo: sending null customerInfo.");
      SendCustomerInfo.add("customerInfo", null);
    }
    try {
      sendMessage(gson.toJson(SendCustomerInfo));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
