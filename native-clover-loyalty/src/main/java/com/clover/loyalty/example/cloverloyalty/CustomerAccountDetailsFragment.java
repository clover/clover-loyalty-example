package com.clover.loyalty.example.cloverloyalty;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.clover.loyalty.example.cloverloyalty.adapters.CustomerAccountTransactionsAdapter;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts;

public class CustomerAccountDetailsFragment extends Fragment {

  CustomerAccounts.CustomerAccount customerAccount;

  public static CustomerAccountDetailsFragment newInstance(CustomerAccounts.CustomerAccount account) {
    Bundle bundle = new Bundle();
    bundle.putSerializable("CUSTOMER_ACCOUNT", account);
    CustomerAccountDetailsFragment fragment = new CustomerAccountDetailsFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.customerAccount = (CustomerAccounts.CustomerAccount)getArguments().get("CUSTOMER_ACCOUNT");
  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_customer_account_details, container, false);

    TextView idTextView = view.findViewById(R.id.customer_account_id);
    TextView nameTextView = view.findViewById(R.id.customer_account_name);
    TextView phoneTextView = view.findViewById(R.id.customer_account_phone);
    TextView pointsTextView = view.findViewById(R.id.customer_account_points);

    idTextView.setText(String.format("%d", customerAccount.id));
    nameTextView.setText(customerAccount.name);
    phoneTextView.setText(formatAsPhone(customerAccount.phone));
    pointsTextView.setText(String.format("%d", customerAccount.points));

    ListView txListView = view.findViewById(R.id.account_transactions_list);
    txListView.setAdapter(new CustomerAccountTransactionsAdapter(this.getActivity().getBaseContext(), customerAccount.accountTransactions));

    return view;
  }

  private String formatAsPhone(String number) {
    if(number.length() == 10) {
      return number.substring(0, 3) + "." + number.substring(3, 6) + "." + number.substring(6);
    }
    return number;
  }
}
