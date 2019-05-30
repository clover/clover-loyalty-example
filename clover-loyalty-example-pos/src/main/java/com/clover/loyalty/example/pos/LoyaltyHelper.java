package com.clover.loyalty.example.pos;

import com.clover.loyalty.LoyaltyDataTypes;
import com.clover.loyalty.example.pos.accounts.CustomerAccounts;
import com.clover.loyalty.example.pos.accounts.CustomerAccountsSQLiteOpenHelper;
import com.clover.remote.client.messages.CustomerProvidedDataEvent;
import com.clover.remote.client.messages.DataProviderConfig;
import com.clover.sdk.v3.payments.VasDataTypeType;
import com.clover.sdk.v3.payments.VasProtocol;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.nfc.NdefRecord.TNF_EXTERNAL_TYPE;

class LoyaltyHelper {
  private static final String LOG_TAG = LoyaltyHelper.class.getSimpleName();
  private static final Gson GSON = new Gson();
  private final CustomerAccountsSQLiteOpenHelper dbHelper;
  LoyaltyHelper(Context context) {
    dbHelper = new CustomerAccountsSQLiteOpenHelper(context);
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
  LoyaltyHelper addInterestInVasPk(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType(LoyaltyDataTypes.VAS_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
      Map<String, String> configuration = new HashMap<>();
      configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROVIDER_PACKAGE, "com.clover.loyalty.example.CLE");
      configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROTOCOL_ID, VasProtocol.PK.toString());
      Map<String, String> protocolConfig = new HashMap<>();
      protocolConfig.put("pkPassTypeId", "pass.clover.customer");
      configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROTOCOL_CONFIG, GSON.toJson(protocolConfig));
      if (CloverLoyaltyPOSActivity.serverPort != null && CloverLoyaltyPOSActivity.serverIp != null) {
        //if user passes in VAS endpoint, then use it
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_URL, "http://"+ CloverLoyaltyPOSActivity.serverIp + ":" + CloverLoyaltyPOSActivity.serverPort +"/v1/getPassUrl");
      } else {
        configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_URL, "http://pk.clover.com/{customer.externalId}");
      }
      configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PUSH_TITLE, "Clover Burger Bucks");
      loyaltyDataConfig.setConfiguration(configuration);
      loyaltyDataConfigs.add(loyaltyDataConfig);
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    return this;
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
  LoyaltyHelper addInterestInVasST(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType(LoyaltyDataTypes.VAS_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
      Map<String, String> configuration = new HashMap<>();
      configuration.put(LoyaltyDataTypes.VAS_TYPE_KEYS.PROVIDER_PACKAGE, "com.clover.loyalty.example.CLE");
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
    return this;
  }

  /**
   * This adds interest in the pre-defined EMAIL loyalty type.
   * "configuration" that can be used by the provider of the information to filter the data collected.
   * <p>
   * The interpretation of the information is done by the provider of the data.
   *
   * @param loyaltyDataConfigs - the set of configurations to add to
   */
  LoyaltyHelper addInterestInPhone(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType(LoyaltyDataTypes.PHONE_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
      loyaltyDataConfigs.add(loyaltyDataConfig);
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    return this;
  }

  LoyaltyHelper addInterestInQuickPay(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType(LoyaltyDataTypes.QUICKPAY_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
      loyaltyDataConfigs.add(loyaltyDataConfig);
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    return this;
  }

  LoyaltyHelper addInterestInAccountNumber(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType("com.loyalty.AccountNumber"); // This is a string.  We want to avoid enums, but will have some types by convention
      loyaltyDataConfigs.add(loyaltyDataConfig);
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    return this;
  }

  LoyaltyHelper addInterestInBarCode(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType("BARCODE"); // This is a string.  We want to avoid enums, but will have some types by convention
      loyaltyDataConfigs.add(loyaltyDataConfig);
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    return this;
  }

  LoyaltyHelper addInterestInClear(List<DataProviderConfig> loyaltyDataConfigs) {
    try {
      DataProviderConfig loyaltyDataConfig = new DataProviderConfig();
      loyaltyDataConfig.setType(LoyaltyDataTypes.CLEAR_TYPE); // This is a string.  We want to avoid enums, but will have some types by convention
      loyaltyDataConfigs.add(loyaltyDataConfig);
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    return this;
  }

  List<CustomerAccounts.CustomerAccount> process(CustomerProvidedDataEvent event, ClearCustomerIntf clearCustomer) {
    List<CustomerAccounts.CustomerAccount> accounts = null;
    switch (event.getConfig().getType()) {
      case LoyaltyDataTypes.PHONE_TYPE:
      /*
      Standard Phone type
       */
        Log.d(LOG_TAG, "process: phone number: " + event.getData());
        accounts = dbHelper.getCustomerAccountsByPhone(event.getData());
        if (accounts == null || accounts.size() == 0) {
          accounts = createNewCustomerWithPhone(dbHelper, event);
        }
        break;
      case "com.loyalty.AccountNumber":
      /*
      A custom type
       */
        Log.d(LOG_TAG, "process: account number: " + event.getData());
        accounts = dbHelper.getCustomerAccountsById(event.getData());
        // We do not create by account number, nothing returned
        break;
      case LoyaltyDataTypes.EMAIL_TYPE:
      /*
      Standard Email type
       */
        Log.d(LOG_TAG, "process: email: " + event.getData());
        accounts = dbHelper.getCustomerAccountsByEmail(event.getData());
        if (accounts == null) {
          accounts = createNewCustomerWithEmail(dbHelper, event);
        }
        break;
      case "BARCODE":
      /*
      Custom Barcode type
       */
        Log.d(LOG_TAG, "process: barcode: " + event.getData());

        JsonObject jsonObject = new Gson().fromJson(event.getData(), JsonObject.class);
        String data = jsonObject.get("barcodeValue").getAsString();

        if (data.length() == 10) {
          // TODO: check for phone number regex or better match
          accounts = dbHelper.getCustomerAccountsByPhone(data);
        } else {
          if (data.matches(".+@.+\\..+")) {
            accounts = dbHelper.getCustomerAccountsByEmail(event.getData().trim());
          } else {
            try {
              int id = Integer.parseInt(data);
              accounts = dbHelper.getCustomerAccountsById(id + "");
            } catch (NumberFormatException nfe) {
              // what about phone number?
            }
          }
        }

        break;
      case LoyaltyDataTypes.VAS_TYPE:
      /*
        The standard VAS type
       */
        accounts = getCustomersFromVasEvent(event);
        if (accounts == null) {
          // Should not ever happen.  We should ALWAYS create a new customer, but something went wrong.
          Log.e(LOG_TAG, "Should not ever happen.  We should ALWAYS create a new customer, but something went wrong. ");
        }
        break;
      case LoyaltyDataTypes.QUICKPAY_TYPE:
        accounts = getCustomersFromQuickpayEvent(event, clearCustomer);
        if (accounts == null) {
          Log.e(LOG_TAG, "Should not ever happen.  We should ALWAYS create a new customer, but something went wrong. ");
        }
        break;
      case LoyaltyDataTypes.CLEAR_TYPE:
        Log.e(LOG_TAG, "Should not ever happen.  We should ALWAYS create a new customer, but something went wrong. ");
        break;
    }
    return accounts;
  }

  private List<CustomerAccounts.CustomerAccount> createNewCustomerWithEmail(CustomerAccountsSQLiteOpenHelper dbHelper, CustomerProvidedDataEvent event) {
    CustomerAccounts.CustomerAccount customerAccount = new CustomerAccounts.CustomerAccount(
        dbHelper.getCustomerAccountsCount(),
        UUID.randomUUID().toString().replace("-", ""),
        "Unknown Customer",
        null,
        0,
        event.getData(),
        null,
        new ArrayList<CustomerAccounts.AccountTransaction>(),
        new ArrayList<CustomerAccounts.CustomerCard>());
    dbHelper.addCustomerAccount(customerAccount);
    return Collections.singletonList(customerAccount);
  }

  private List<CustomerAccounts.CustomerAccount> createNewCustomerWithPhone(CustomerAccountsSQLiteOpenHelper dbHelper, CustomerProvidedDataEvent event) {
    CustomerAccounts.CustomerAccount customerAccount=null;
    if (CloverLoyaltyPOSActivity.serverIp != null && CloverLoyaltyPOSActivity.serverPort != null) {
      String customerJSON = LoyaltyServiceHelper.queryService("/customer/query/phone/"+event.getData());
      if (customerJSON != null) {
        customerAccount = new Gson().fromJson(customerJSON, CustomerAccounts.CustomerAccount.class);
      }
    } else {
      customerAccount = new CustomerAccounts.CustomerAccount(dbHelper.getCustomerAccountsCount(), UUID.randomUUID().toString().replace("-", ""), "Unknown Customer", event.getData(), 0, null, null, new ArrayList<CustomerAccounts.AccountTransaction>(),
          new ArrayList<CustomerAccounts.CustomerCard>());
      dbHelper.addCustomerAccount(customerAccount);
    }

    return Collections.singletonList(customerAccount);
  }

  private List<CustomerAccounts.CustomerAccount> getCustomersFromQuickpayEvent(CustomerProvidedDataEvent event, ClearCustomerIntf clearCustomer) {
    List<CustomerAccounts.CustomerAccount> customerAccounts = null;

    JsonObject jObj = GSON.fromJson(event.getData(), JsonObject.class);
    String dataMethodElem = jObj.get("dataMethod").getAsString();
    if ("QUICKPAY_DEFERRED_READ".equals(dataMethodElem)) {
      JsonElement dataElem = jObj.get("data");
      if (dataElem.isJsonObject()) {
        JsonObject dataObj = dataElem.getAsJsonObject();
        JsonElement qpDataXtraElem = dataObj.get("QUICKPAY_DATA_EXTRAS");
        if (qpDataXtraElem.isJsonObject()) {
          JsonObject qpDataXtraObj = qpDataXtraElem.getAsJsonObject();
          String cardType = qpDataXtraObj.get("cardType").getAsString();
          String last4 = qpDataXtraObj.get("last4").getAsString();

          customerAccounts = dbHelper.getCustomerAccountsByCard(cardType, last4);
          if (customerAccounts != null && customerAccounts.size() > 0) {
            return customerAccounts;
          }
          CustomerAccounts.CustomerAccount customerAccount = new CustomerAccounts.CustomerAccount(
              dbHelper.getCustomerAccountsCount(),
              UUID.randomUUID().toString().replace("-", ""),
              String.format("Customer from %s card %s", cardType, last4),
              null,
              0,
              null,
              null,
              new ArrayList<CustomerAccounts.AccountTransaction>(),
              Collections.singletonList(new CustomerAccounts.CustomerCard(cardType, last4)));
          dbHelper.addCustomerAccount(customerAccount);
        }
      }
    } else if ("QUICKPAY_DEFERRED_READ_CLEAR".equals(dataMethodElem)) {
      clearCustomer.clearCustomer();
    }
    return customerAccounts;
  }

  private List<CustomerAccounts.CustomerAccount> getCustomersFromVasEvent(CustomerProvidedDataEvent event) {

    List<CustomerAccounts.CustomerAccount> customerAccounts;

    JsonObject jObj = GSON.fromJson(event.getData(), JsonObject.class);
    String vasPayloadString = jObj.get("vasPayload").getAsString();
    JsonObject jsonObject = new Gson().fromJson(vasPayloadString, JsonObject.class);
    JsonObject payloadElements = jsonObject.getAsJsonObject("payloadElements");
    JsonArray elements = payloadElements.getAsJsonArray("elements");

    if (elements.size() > 0) {
      customerAccounts = new ArrayList<>(payloadElements.size());

      for (JsonElement element : elements) {
        JsonElement dataType = element.getAsJsonObject().get("dataType");
        if (dataType != null) {
          JsonElement dataTypeDataType = dataType.getAsJsonObject().get("dataType");
          if (dataTypeDataType != null) {
            VasDataTypeType dataTypeType = VasDataTypeType.valueOf(dataTypeDataType.getAsString());
            String vasData = element.getAsJsonObject().get("vasData").getAsString();// .payloadElement.getVasData();
            switch (dataTypeType) {
              case VAS_DATA:
                VasProtocol protocol = VasProtocol.valueOf(event.getConfig().getConfiguration().get(LoyaltyDataTypes.VAS_TYPE_KEYS.PROTOCOL_ID));
                getCustomerAccountForVas(dbHelper, customerAccounts, protocol, vasData);
                break;
              case LOYALTY:
                getCustomerAccountGoogleST(dbHelper, customerAccounts, vasData);
                break;
              case CUSTOMER:
                getCustomerAccountGoogleST(dbHelper, customerAccounts, vasData);
                break;
              case OFFER:
                getCustomerAccountGoogleST(dbHelper, customerAccounts, vasData);
                break;
              default:
                Log.d(LOG_TAG, String.format("VasDataTypeType %s vasData %s", dataTypeType, vasData));
            }
          }
        }
        if (customerAccounts.size() > 0) {
          break;
        }
      }
    } else {
      throw new RuntimeException(String.format("Got a VAS event with empty payload.  %s", jObj.toString()));
    }
    return customerAccounts;
  }

  private String byteArrayToString(byte[] byteArray) {
    return byteArrayToString(byteArray, 0, " ");
  }
  private String byteArrayToString(byte[] byteArray, int offset, String delim) {
    StringBuilder sb = new StringBuilder();
    for (int idx = offset; idx < byteArray.length; idx++) {
      byte b = byteArray[idx];
      sb.append(String.format("%02X%s", b, delim));
    }
    return sb.toString();
  }

  static private @NonNull
  byte[] decodeBase64(String s) {
    byte[] empty = new byte[] {};
    byte[] b = Base64.decode(s, Base64.NO_WRAP);
    if (b == null) {
      return empty;
    }
    if (b.length == 0) {
      return empty;
    }
    return b;
  }

  private void getCustomerAccountGoogleST(
      CustomerAccountsSQLiteOpenHelper dbHelper,
      List<CustomerAccounts.CustomerAccount> customerAccounts,
      String vasData) {
    // Have to figure out what we are looking for
    byte[] recordPayload = decodeBase64(vasData);
    Log.d(LOG_TAG, String.format("VAS Data Message: %s ", byteArrayToString(recordPayload)));
    getCustomerAccountFromNDEF(dbHelper, customerAccounts, recordPayload, "");
  }

  private void getCustomerAccountFromNDEF(
      CustomerAccountsSQLiteOpenHelper dbHelper,
      List<CustomerAccounts.CustomerAccount> customerAccounts,
      byte[] recordPayload,
      String indent) {
    Log.d(LOG_TAG, String.format("%s start *****************************************", indent));
    Log.d(LOG_TAG, String.format("%s recordPayload: %s ", indent, byteArrayToString(recordPayload)));

    try {
      NdefMessage ndefMessage = new NdefMessage(recordPayload);
      NdefRecord[] records = ndefMessage.getRecords();
      for (NdefRecord ndefRecord : records) {

        byte[] CustomerID = new byte[]{'c', 'i', 'd'};
        byte[] PreferredLanguageCode = new byte[]{'c', 'p', 'l'};
        byte[] UniqueTapID = new byte[]{'c', 'u', 't'};
        byte[] UniqueDeviceID = new byte[]{'c', 'u', 'd'};
        byte[] Text = new byte[]{'T'}; // "Text" is from the broader NDEF spec, not the Smart-Tap spec.

        // Top level records.
        byte[] Customer = new byte[]{'c', 'u', 's'};
        byte[] Loyalty = new byte[]{'l', 'y'};

        Log.d(LOG_TAG, String.format("%s      ndefRecord id: %s ", indent, byteArrayToString(ndefRecord.getId())));
        Log.d(LOG_TAG, String.format("%s    ndefRecord type: %s ", indent, byteArrayToString(ndefRecord.getType())));
        Log.d(LOG_TAG, String.format("%s ndefRecord payload: %s ", indent, byteArrayToString(ndefRecord.getPayload())));
        Log.d(LOG_TAG, String.format("%s     ndefRecord tnf: %s ", indent, ndefRecord.getTnf()));


        // Specs - https://nfc-forum.org/our-work/specifications-and-application-documents/specifications/nfc-forum-technical-specifications/
        // Requires a membership.
        //  Found a copy and have it here - https://confluence.dev.clover.com/download/attachments/9535649/NFCForum-TS-NDEF.pdf?api=v2
        //
        // -- Start Message --
        // D4 HEADER (tnf+flags): 0xd4 - 11010100
        //      ( MB:1, ME:1, CF:0, SR:1, IL:0, TNF:4 )
        //      11010100
        //        1     MB - message begin
        //        1     ME - Message End
        //        0     CF - Chunk Flag
        //        1     SR - Short Record
        //        0     IL - ID "Length" (boolean: is it there?)
        //        100   TNF - Type Name Format 4
        //
        // 03 TYPE LENGTH - 3 bytes.  This is the length of the "TYPE" below
        // 24 PAYLOAD LENGTH: 36 bytes.  This is the length of the payload.
        //
        // 63 75 73 TYPE: 0x637573	("cus")
        // -- Start Payload --
        //    -- Start Record --
        //    94 HEADER (tnf+flags): 0x94 10010100
        //        ( MB:1, ME:0, CF:0, SR:1, IL:0, TNF:4 )
        //        10010100
        //          1     MB - message begin
        //          0     ME - Message End
        //          0     CF - Chunk Flag
        //          1     SR - Short Record
        //          0     IL - ID "Length" (boolean: is it there?)
        //          100   TNF - Type Name Format 4
        //
        //    03 TYPE LENGTH - 3 bytes.  This is the length of the "TYPE" below
        //    0B PAYLOAD LENGTH: 11 bytes
        //    63 69 64 TYPE: ("cid")
        //    -- Start Payload --
        //    04 12 34 56 78 90 11 11 11 11 11 - Note that the value I expected here was 12345678901111111111.
        //      The first byte (0x04) obviously has some significance, but I am having trouble finding what it is.
        //    -- End Payload --
        //    -- End Record --
        //
        //    -- Start Record --
        //    19 HEADER (tnf+flags): 0x19 11001
        //        ( MB:0, ME:0, CF:0, SR:1, IL:1, TNF:1 )
        //        00011001
        //          0     MB - message begin
        //          0     ME - Message End
        //          0     CF - Chunk Flag
        //          1     SR - Short Record
        //          1     IL - ID "Length" (boolean: is it there?)
        //          001   TNF - Type Name Format 1
        //
        //    01 TYPE LENGTH - 1 byte.  This is the length of the "TYPE" below
        //    03 PAYLOAD LENGTH: 3 bytes
        //    03 ID LENGTH: 3 bytes.  Note the flag above that tells us that the id IS present on this record
        //    54 TYPE: 0x54	("T").  Note the flag above that tells us that the type is a 'Well-Known Record', the TYPE LENGTH tells us it is one byte. (urn:nfc:wkt:t)
        //    63 70 6C ID: 0x63706c ("cpl")
        //    -- Start Payload --
        //    00 65 6E - PAYLOAD is : 0x00656e " en"
        //        00 Status byte
        //        65 "e"
        //        6e "n"
        //    -- End Payload --
        //    -- End Record --
        //
        //    -- Start Record --
        //    54 HEADER (tnf+flags): 0x54 1010100
        //        ( MB:0, ME:1, CF:0, SR:1, IL:0, TNF:4 )
        //        01010100
        //          0     MB - message begin
        //          1     ME - Message End
        //          0     CF - Chunk Flag
        //          1     SR - Short Record
        //          0     IL - ID "Length" (boolean: is it there?)
        //          100   TNF - Type Name Format 4
        //
        //    03 TYPE LENGTH: 3
        //    02 PAYLOAD LENGTH 2 bytes
        //    63 75 74 TYPE: ("cut")
        //    -- Start Payload --
        //    04 7B
        //    -- End Payload --
        //    -- End Record --
        //
        // -- End Payload --
        // -- End Message --

        if (ndefRecord.getTnf() == TNF_EXTERNAL_TYPE) {
          // Indicates the type field contains an external type name.
          Log.d(LOG_TAG, String.format("%s ndefRecord type is external", indent));
        }

        Log.d(LOG_TAG, String.format("%s ndefRecord: %s", indent, ndefRecord.toString()));
        if (Arrays.equals(ndefRecord.getType(), CustomerID)) {
          // This is assuming the test app on an android phone
          String extId = byteArrayToString(ndefRecord.getPayload(), 1, "");
          Log.d(LOG_TAG, String.format("%s Record is a CustomerID: %s ", indent, extId));
          List<CustomerAccounts.CustomerAccount> customerAccountFound = dbHelper.getCustomerAccountsByExternalId(extId);
          if (customerAccountFound.size() <= 0) {
            CustomerAccounts.CustomerAccount newPhoneBasedAccount = new CustomerAccounts.CustomerAccount(
                dbHelper.getCustomerAccountsCount(),
                UUID.randomUUID().toString().replace("-", ""),
                "GoogleSmartTap Customer",
                "2222222222",
                0,
                null,
                null,
                new ArrayList<CustomerAccounts.AccountTransaction>(),
                new ArrayList<CustomerAccounts.CustomerCard>());
            dbHelper.addCustomerAccount(newPhoneBasedAccount);
            customerAccounts.add(newPhoneBasedAccount);
          } else {
            customerAccounts.addAll(customerAccountFound);
          }
        } else if (Arrays.equals(ndefRecord.getType(),PreferredLanguageCode)) {
          Log.d(LOG_TAG, String.format("%s Record is a PreferredLanguageCode: %s ", indent, new String(ndefRecord.getPayload())));
        } else if (Arrays.equals(ndefRecord.getType(),UniqueTapID)) {
          Log.d(LOG_TAG, String.format("%s Record is a UniqueTapID: %s ", indent, new String(ndefRecord.getPayload())));
        } else if (Arrays.equals(ndefRecord.getType(),UniqueDeviceID)) {
          Log.d(LOG_TAG, String.format("%s Record is a UniqueDeviceID: %s ", indent, new String(ndefRecord.getPayload())));
        } else if (Arrays.equals(ndefRecord.getType(),Text)) {
          Log.d(LOG_TAG, String.format("%s Record is a Text: %s ", indent, new String(ndefRecord.getPayload())));
        } else if (Arrays.equals(ndefRecord.getType(),Customer)) {
          Log.d(LOG_TAG, String.format("%s Record is a Customer ", indent ));
          getCustomerAccountFromNDEF(dbHelper, customerAccounts, ndefRecord.getPayload(), indent + "\t");
        } else if (Arrays.equals(ndefRecord.getType(),Loyalty)) {
          Log.d(LOG_TAG, String.format("%s Record is a Loyalty ", indent));
          getCustomerAccountFromNDEF(dbHelper, customerAccounts, ndefRecord.getPayload(), indent + "\t");
        } else {
          Log.d(LOG_TAG, String.format("%s ndefRecord type is unknown: %s ", indent, new String(ndefRecord.getType())));
        }
      }

    } catch (FormatException e) {
      Log.e(LOG_TAG, "Error getting NDEF Message", e);
    }
    Log.d(LOG_TAG, String.format("%s end *****************************************", indent));
  }

  private void getCustomerAccountForVas(
      CustomerAccountsSQLiteOpenHelper dbHelper,
      List<CustomerAccounts.CustomerAccount> customerAccounts,
      VasProtocol protocol, String vasData) {
    switch (protocol) {
      case PK: {
        Log.d(LOG_TAG, String.format("process: VAS PK: %s", vasData));
        List<CustomerAccounts.CustomerAccount> customerAccountFound = new ArrayList<>();
        if (CloverLoyaltyPOSActivity.serverPort != null && CloverLoyaltyPOSActivity.serverIp != null) {
          String customerJSON = LoyaltyServiceHelper.queryService("/customer/query/vas/"+vasData);
          if (customerJSON != null) {
            CustomerAccounts.CustomerAccount customerAccount = new Gson().fromJson(customerJSON, CustomerAccounts.CustomerAccount.class);
            customerAccountFound.add(customerAccount);
          }
        } else {
          customerAccountFound = dbHelper.getCustomerAccountsByUUID(vasData);
        }
        if (customerAccountFound.size() <= 0) {
          CustomerAccounts.CustomerAccount newPhoneBasedAccount = new CustomerAccounts.CustomerAccount(dbHelper.getCustomerAccountsCount(), vasData, "iPhone Customer", "2222222222", 0, null, null, new ArrayList<CustomerAccounts.AccountTransaction>(),
              new ArrayList<CustomerAccounts.CustomerCard>());
          dbHelper.addCustomerAccount(newPhoneBasedAccount);
          customerAccounts.add(newPhoneBasedAccount);
        } else {
          customerAccounts.addAll(customerAccountFound);
        }
        break;
      }
      case ST: {
        Log.d(LOG_TAG, String.format("process: VAS ST: %s", vasData));
        List<CustomerAccounts.CustomerAccount> customerAccountFound = dbHelper.getCustomerAccountsByUUID(vasData);
        if (customerAccountFound.size() <= 0) {
          customerAccounts.add(new CustomerAccounts.CustomerAccount(dbHelper.getCustomerAccountsCount(), vasData, "Unknown Google Customer", "?", 0, null, null, new ArrayList<CustomerAccounts.AccountTransaction>(),
              new ArrayList<CustomerAccounts.CustomerCard>()));
        } else {
          customerAccounts.addAll(customerAccountFound);
        }
        break;
      }
      default:
        // nothing
    }
  }

  void addPointsToCustomer(int points, String customerUuid) {
    List<CustomerAccounts.CustomerAccount> accounts = dbHelper.getCustomerAccountsByUUID(customerUuid);
    if (accounts.size() == 1) {
      CustomerAccounts.CustomerAccount acct = accounts.get(0);
      acct.points += points;
      dbHelper.updateCustomerAccount(acct);
    }
  }
}
