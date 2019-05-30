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

package com.example.clover.loyalty.tender;

import android.content.Context;
import android.util.Log;
import com.clover.sdk.v3.customers.CustomerInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a very simplistic loyalty helper which store loyalty accounts in
 * a json file. It is used by the ExampleLoyaltyService and MerchantLoyaltyTenderActivity
 * to load, update and save customer accounts, as well as keep a transient association
 * between orders and customers
 *
 */
public class LoyaltyHelper {
  public static final String LOG_TAG = LoyaltyHelper.class.getSimpleName();

  public static Map<String, CustomerAccount> getAllAccounts(Context context) {
    String yourFilePath = context.getFilesDir() + "/" + "customers.json";
    File file = new File(yourFilePath);
    try {
      if (file.exists()) {
        FileReader fileReader = new FileReader(file);
        Type mapType = new TypeToken<HashMap<String, CustomerAccount>>() {
        }.getType();
        HashMap<String, CustomerAccount> customerAccounts = new Gson().fromJson(fileReader, mapType);
        if(customerAccounts == null){
          customerAccounts = new HashMap<>();
          saveAccounts(context, customerAccounts);
        }
        return customerAccounts;
      } else {
        file.createNewFile();
        return new HashMap<>();
      }

    } catch (Exception e) {
      Log.d(LOG_TAG, "Error parsing accounts file.", e);
    }
    return new HashMap<>();
  }

  public static boolean saveAccounts(Context context, Map<String, CustomerAccount> accounts) {
    String yourFilePath = context.getFilesDir() + "/" + "customers.json";

    String json = new Gson().toJson(accounts);

    File file = new File(yourFilePath);
    try {
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.write(json);
      fileWriter.close();

      return true;
    } catch (Exception e) {
      Log.d(LOG_TAG, "Error parsing accounts file.", e);
    }
    return false;
  }

  public static CustomerAccount getCustomerAccount(Context context, String id) {
    Map<String, CustomerAccount> stringCustomerAccountMap = getAllAccounts(context);
    CustomerAccount customerAccount = stringCustomerAccountMap.get(id);
    return customerAccount;
  }

  public static boolean saveAccount(Context context, CustomerAccount account) {
    Map<String, CustomerAccount> stringCustomerAccountMap = getAllAccounts(context);
    stringCustomerAccountMap.put(account.id, account);

    return saveAccounts(context, stringCustomerAccountMap);
  }

  public static CustomerAccount getCustomerAccountForOrderId(Context context, String orderId) {
    Map<String, CustomerAccount> accounts = getAllAccounts(context);
    for (CustomerAccount account : accounts.values()) {
      if(Arrays.asList(account.orders).contains(orderId)) {
        return account;
      }
    }
    return null;
  }

  public static boolean registerOrderForCustomer(Context context, String orderId, String customerAccountId) {
    CustomerAccount customerAccount = getCustomerAccount(context, customerAccountId);
    String[] orders = customerAccount.orders;
    List<String> modOrders = new ArrayList<>(Arrays.asList(orders));
    modOrders.add(orderId);
    customerAccount.orders = modOrders.toArray(new String[modOrders.size()]);

    return saveAccount(context, customerAccount);
  }

  public static CustomerInfo getCustomerInfo(CustomerAccount customerAccount) {
    CustomerInfo customerInfo = new CustomerInfo();
    customerInfo.setDisplayString(customerAccount.firstName);
    Map<String, String> extras = new HashMap<>();
    extras.put("POINTS", String.format("%d", customerAccount.points));
    customerInfo.setExtras(extras);
    customerInfo.setExternalId(customerAccount.id);
    customerInfo.setExternalSystemName("Example Loyalty Tender Rewards");

    return customerInfo;
  }
}
