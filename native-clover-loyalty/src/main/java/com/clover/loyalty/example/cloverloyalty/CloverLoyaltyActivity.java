package com.clover.loyalty.example.cloverloyalty;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;
import com.clover.loyalty.example.cloverloyalty.providers.CustomerAccountsSQLiteOpenHelper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CloverLoyaltyActivity extends Activity implements CustomerListFragment.OnCustomerListFragmentListener, NumberPadFragment.NumberPadFragmentListener {

  public void loadCFPActivities(View view) {

  }

  private enum BUTTON {
    ONE {
      @Override String process(String currentNumber) {
        return currentNumber + "1";
      }
    },
    TWO {
      @Override String process(String currentNumber) {
        return currentNumber + "2";
      }
    },
    THREE {
      @Override String process(String currentNumber) {
        return currentNumber + "3";
      }
    },
    FOUR {
      @Override String process(String currentNumber) {
        return currentNumber + "4";
      }
    },
    FIVE {
      @Override String process(String currentNumber) {
        return currentNumber + "5";
      }
    },
    SIX {
      @Override String process(String currentNumber) {
        return currentNumber + "6";
      }
    },
    SEVEN {
      @Override String process(String currentNumber) {
        return currentNumber + "7";
      }
    },
    EIGHT {
      @Override String process(String currentNumber) {
        return currentNumber + "8";
      }
    },
    NINE {
      @Override String process(String currentNumber) {
        return currentNumber + "9";
      }
    },
    ZERO {
      @Override String process(String currentNumber) {
        return currentNumber + "0";
      }
    },
    BACK {
      @Override String process(String currentNumber) {
        return currentNumber.substring(0, Math.max(0, currentNumber.length()-1));
      }
    },
    CLEAR {
      @Override String process(String currentNumber) {
        return "";
      }
    };

    String process(String currentNumber) {
      return currentNumber;
    }
  }

  private static String createPhone(int index) {
    int start = 5;
    String phone = "";
    for(int i=0;i<10; i++) {
      start =  (start + index*index + 1) % 10;
      phone += start;

    }
    return phone;
    //    long number = (int) Math.abs(2 + Math.floor(Math.random() * 6));
    //    for(int i=0;i<9; i++) {
    //      number *= 10 + Math.abs((int)Math.floor(Math.random() * 10));
    //    }
    //    return String.format("%d", number).substring(0,10);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {

    this.requestWindowFeature(Window.FEATURE_NO_TITLE);



    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_clover_loyalty);

    /**
     * START BOOTSTRAP FOR NEW APP W/ NO DB
     */
    CustomerAccountsSQLiteOpenHelper dbHelper = new CustomerAccountsSQLiteOpenHelper(this);
    int count = dbHelper.getCustomerAccountsCount();
    /**
     * END BOOTSTRAP
     */

    CustomerAccounts.CustomerAccount account = (CustomerAccounts.CustomerAccount) getIntent().getSerializableExtra("CUSTOMER");
    final String phone = getIntent().getStringExtra("PHONE");

    if(account == null) {
      List<CustomerAccounts.CustomerAccount> customerAccountsByPhone = dbHelper.getCustomerAccountsByPhone(phone);

      if(customerAccountsByPhone.size() == 0){
        account = dbHelper.createCustomerAccount(new CustomerAccounts.CustomerAccount(-1, UUID.randomUUID().toString().replace("-", ""), "Name", phone, 0, null, Collections.<CustomerAccounts.AccountTransaction>emptyList()));
      } else if (customerAccountsByPhone.size() == 1) {
        account = customerAccountsByPhone.get(0);
      } else {
        // do nothing...
      }
    }

    Intent svc = new Intent(getApplicationContext(), CloverLoyaltyService.class);
    bindService(svc, new ServiceConnection() {
      @Override public void onServiceConnected(ComponentName name, IBinder service) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, CustomerListFragment.newInstance(4, phone), "CUSTOMER_LIST_FRAGMENT");
        fragmentTransaction.commit();
      }

      @Override public void onServiceDisconnected(ComponentName name) {

      }
    }, Context.BIND_AUTO_CREATE);


    if(account != null) {
      onAccountSelected(account);
    }



  }

  @Override public void onAccountSelected(CustomerAccounts.CustomerAccount acct) {

    Fragment customerListFragment = this.getFragmentManager().findFragmentByTag("CUSTOMER_LIST_FRAGMENT");

    FragmentTransaction fragmentTransaction = this.getFragmentManager().beginTransaction();
    if(customerListFragment != null) {
      fragmentTransaction.remove(customerListFragment);
    }
    fragmentTransaction.add(R.id.fragment_container, CustomerAccountDetailsFragment.newInstance(acct), "CUSTOMER_ACCOUNT_DETAILS_FRAGMENT");
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
    this.getFragmentManager().executePendingTransactions();
  }

  public void announceCustomer(View view) {
    // TODO: announce
    finish();
  }

  public void buttonClicked(String tag) {
    CustomerListFragment customerListFragment = (CustomerListFragment) getFragmentManager().findFragmentByTag("CUSTOMER_LIST_FRAGMENT");
    customerListFragment.buttonClicked(tag);
  }

  @Override public void onNumberButton(String tag) {
    buttonClicked(tag);
  }
}
