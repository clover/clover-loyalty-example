package com.clover.loyalty.example.cloverloyalty.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.clover.loyalty.example.cloverloyalty.R;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;
import com.clover.loyalty.example.cloverloyalty.providers.AccountWrapper;
import com.clover.loyalty.example.cloverloyalty.providers.CustomerAccountsSQLiteOpenHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CustomerAccountsAdapter extends BaseAdapter {
  private static Field uuidField;
  private final String TAG = getClass().getSimpleName();

  //  List<CustomerAccounts.CustomerAccount> accountList = new ArrayList<>();

  private Context mContext;
  private LayoutInflater mInflater;
  private String mFilter = "";

  CustomerAccountsSQLiteOpenHelper dbHelper;
  private static Field phoneField;

  public CustomerAccountsAdapter(Context context, CustomerAccountsSQLiteOpenHelper dbHelper) {
    mContext = context;
//    dbHelper.addAll(wrapper.getItems(""));
    this.dbHelper = dbHelper;
//    accountList = items;
    mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    try {
      phoneField = CustomerAccounts.CustomerAccount.class.getField("phone");
    } catch (Exception iae) {
      Log.e(TAG, "CustomerAccountsAdapter: Couldn't get phone field", iae);
    }
  }

  @Override public int getCount() {
    return dbHelper.getCustomerAccountsCountByPhone(mFilter);
  }

  @Override public Object getItem(int i) {
    return dbHelper.getCustomerAccountsByPhone(mFilter).get(i);// getItems(phoneField, mFilter).get(i);
  }

  @Override public long getItemId(int i) {
    return dbHelper.getCustomerAccountsByPhone(mFilter).get(i).id;//  getItems(phoneField, mFilter).get(i).hashCode();
  }

  @Override public View getView(int i, View view, ViewGroup parent) {
    // Get view for row item
    View rowView = mInflater.inflate(R.layout.line_item_customer_account, parent, false);

    TextView idTextView = rowView.findViewById(R.id.idField);
    TextView nameTextView = rowView.findViewById(R.id.nameField);
    TextView phoneTextView = rowView.findViewById(R.id.phoneField);
    TextView pointsTextView = rowView.findViewById(R.id.pointsField);

    CustomerAccounts.CustomerAccount customerAccount = (CustomerAccounts.CustomerAccount) getItem(i);
    idTextView.setText(String.format("%d", customerAccount.id));
    nameTextView.setText(customerAccount.name);
    phoneTextView.setText(customerAccount.phone);
    pointsTextView.setText(String.format("%d", customerAccount.points));

    return rowView;
  }

  public void applyFilter(final String filter) {
    if(filter == null) {
      mFilter = "";
    } else {
      mFilter = filter;
    }
  }

  public String getFilter() {
    return mFilter;
  }



  public static void main(String[] args) {

    AccountWrapper accountWrapper = new AccountWrapper();
//    CustomerAccounts.CustomerAccount blake = new CustomerAccounts.CustomerAccount("1", "AB", "Blake", "1234", 0, Collections.EMPTY_LIST);
//    CustomerAccounts.CustomerAccount camille = new CustomerAccounts.CustomerAccount("2", "BC", "Camille", "1246", 0, Collections.EMPTY_LIST);
//    CustomerAccounts.CustomerAccount jen = new CustomerAccounts.CustomerAccount("3", "CD", "Jen", "3456", 0, Collections.EMPTY_LIST);
    List<CustomerAccounts.CustomerAccount> accounts = new ArrayList<>();
//    accounts.add(blake);
//    accounts.add(camille);
//    accounts.add(jen);

    try {
      phoneField = CustomerAccounts.CustomerAccount.class.getField("phone");
      uuidField = CustomerAccounts.CustomerAccount.class.getField("uuid");

      accountWrapper.addAll(accounts, phoneField);
      accountWrapper.addAll(accounts, uuidField);
    } catch (Exception e) {

    }

    accountWrapper.dump(0);

    System.out.println("1 => " + accountWrapper.getItems(phoneField,"1"));
    System.out.println("12 => " + accountWrapper.getItems(phoneField,"12"));
    System.out.println("234 => " + accountWrapper.getItems(phoneField,"234"));
    System.out.println("6 => " + accountWrapper.getItems(phoneField,"6"));

    System.out.println("A => " + accountWrapper.getItems(uuidField,"A"));
    System.out.println("B => " + accountWrapper.getItems(uuidField,"B"));
    System.out.println("CD => " + accountWrapper.getItems(uuidField,"CD"));
  }
}
