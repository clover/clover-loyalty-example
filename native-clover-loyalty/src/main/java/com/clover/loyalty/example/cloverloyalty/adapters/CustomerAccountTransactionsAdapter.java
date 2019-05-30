package com.clover.loyalty.example.cloverloyalty.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.clover.loyalty.example.cloverloyalty.R;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;

import java.util.ArrayList;
import java.util.List;

public class CustomerAccountTransactionsAdapter extends BaseAdapter {

  private Context mContext;
  private LayoutInflater mInflater;

  List<CustomerAccounts.AccountTransaction> transactions = new ArrayList<>();

  public CustomerAccountTransactionsAdapter(Context context, List<CustomerAccounts.AccountTransaction> transactions) {
    mContext = context;
    mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.transactions = transactions;
  }

  @Override public int getCount() {
    return transactions.size();
  }

  @Override public Object getItem(int i) {
    return transactions.get(i);
  }

  @Override public long getItemId(int i) {
    return transactions.get(i).hashCode();
  }

  @Override public View getView(int i, View view, ViewGroup parent) {
    // Get view for row item
    View rowView = mInflater.inflate(R.layout.line_item_customer_account_transaction, parent, false);

    TextView orderIdTextView = rowView.findViewById(R.id.orderIdField);
    TextView dateTextView = rowView.findViewById(R.id.dateField);
    TextView pointsTextView = rowView.findViewById(R.id.pointsField);

    CustomerAccounts.AccountTransaction accountTransaction = (CustomerAccounts.AccountTransaction) getItem(i);
    orderIdTextView.setText(accountTransaction.orderId);
    dateTextView.setText(accountTransaction.date.toString());
    pointsTextView.setText(String.format("%d", accountTransaction.amount));

    if(accountTransaction.amount < 0) {
      pointsTextView.setTextColor(ContextCompat.getColor(mContext, R.color.debit_text));
    } else {
      pointsTextView.setTextColor(ContextCompat.getColor(mContext, R.color.credit_text));
    }

    return rowView;
  }
}
