package com.clover.loyalty.example.cloverloyalty.providers;

import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;
import com.google.gson.Gson;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountWrapper implements Serializable {
    ArrayList<CustomerAccounts.CustomerAccount> customerAccounts = new ArrayList<>();
    Map<String, Map<Character, AccountWrapper>> map = new HashMap<>();

//    Map<Character, AccountWrapper> map = new HashMap<>();

    public List<CustomerAccounts.CustomerAccount> getItems(Field field, String filter) {
      if(filter == null || filter.length() == 0) {
        return customerAccounts;
      } else {
        if(map.get(field.getName()) == null) {
          return Collections.emptyList();
        }
        AccountWrapper accountWrapper = map.get(field.getName()).get(filter.charAt(0));

        if(accountWrapper == null) {
          return Collections.emptyList();
        } else {
          return accountWrapper.getItems(field, filter.substring(1));
        }
      }
    }

    public void add(CustomerAccounts.CustomerAccount account, Field field, String key) {
      if(!customerAccounts.contains(account)) {
        customerAccounts.add(account);
      }
      if (key.length() > 0) {
        if(map.get(field.getName()) == null) {
          map.put(field.getName(), new HashMap<Character, AccountWrapper>());
        }
        if( map.get(field.getName()).get(key.charAt(0)) == null ) {
          map.get(field.getName()).put(key.charAt(0), new AccountWrapper());
        }
        map.get(field.getName()).get(key.charAt(0)).add(account, field, key.substring(1));
      }
    }

    public void addAll(List<CustomerAccounts.CustomerAccount> accounts, Field field) {
      for (CustomerAccounts.CustomerAccount account : accounts) {
        try {
          String key = field.get(account).toString();
          for (int i = 0; i < key.length(); i++) {
            add(account, field, key.substring(i));
          }
        } catch(Exception e) {
          //
        }
      }
    }

    public void dump(int indent) {
      System.out.println(new Gson().toJson(this));
    }
  }