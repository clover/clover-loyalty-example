package com.clover.loyalty.example.cloverloyalty;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.clover.loyalty.example.cloverloyalty.accounts.CustomerAccounts.CustomerAccount;
import com.clover.loyalty.example.cloverloyalty.adapters.CustomerAccountsAdapter;
import com.clover.loyalty.example.cloverloyalty.providers.CustomerAccountsSQLiteOpenHelper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnCustomerListFragmentListener}
 * interface.
 */
public class CustomerListFragment extends Fragment {

  public static final String TAG = CustomerListFragment.class.getSimpleName();

  private OnCustomerListFragmentListener mListener;

  ListView mListView;
  private CustomerAccountsAdapter adapter;
  TextView filterTextView;
//  private CustomerAccountsAdapter.AccountWrapper accountWrapper;
  Field phoneField;
  private CustomerAccountsSQLiteOpenHelper dbHelper;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public CustomerListFragment() {
    try {
      phoneField = CustomerAccount.class.getField("phone");
    } catch(Exception e) {
      Log.e(getClass().getSimpleName(), "CustomerListFragment: Can't find phone field", e);
    }
  }

  // TODO: Customize parameter initialization
  @SuppressWarnings("unused") public static CustomerListFragment newInstance(int columnCount/*, CustomerAccountsAdapter.AccountWrapper customerAccounts*/, String phone) {

    CustomerListFragment fragment = new CustomerListFragment();
    Bundle args = new Bundle();
//    args.putSerializable("CUSTOMERS", customerAccounts);
    args.putString("PHONE", phone);
    fragment.setArguments(args);
    return fragment;
  }


  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    dbHelper = new CustomerAccountsSQLiteOpenHelper(this.getActivity());
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_customer_accounts, container, false);

    filterTextView = view.findViewById(R.id.filterTextView);

    mListView = (ListView) view.findViewById(R.id.customer_account_list);
    // 1
//    accountWrapper = (CustomerAccountsAdapter.AccountWrapper)getArguments().getSerializable("CUSTOMERS");
    // 2
    List<CustomerAccount> listItems = dbHelper.getCustomerAccounts();//.getItems(phoneField, "");
    // 3
    /*for(int i = 0; i < recipeList.size(); i++){
      CustomerAccount customerAccount = recipeList.get(i);
      listItems.add(customerAccount);
    }*/

    // 4
    adapter = new CustomerAccountsAdapter(this.getActivity().getBaseContext(), dbHelper);
    String initialPhone = getArguments().getString("PHONE");
    Log.d(TAG, "onCreateView: Initial Phone: " + initialPhone);
    adapter.applyFilter(initialPhone);
    setFilter(adapter.getFilter());
//    ArrayAdapter<CustomerAccount> adapter = new ArrayAdapter(this.getActivity().getBaseContext(), R.layout.fragment_item_list, listItems);
    mListView.setAdapter(adapter);

    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//        CustomerAccount customerAccount = dbHelper.getCustomerAccounts().get(i);//accountWrapper.getItems(phoneField, "").get(i);
        CustomerAccount customerAccount = (CustomerAccount) adapter.getItem(i);
        getListener().onAccountSelected(customerAccount);
      }
    });


    return view;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnCustomerListFragmentListener) {
      mListener = (OnCustomerListFragmentListener) context;
    } else {
      throw new RuntimeException(context.toString() + " must implement OnCustomerListFragmentListener");
    }
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnCustomerListFragmentListener) {
      mListener = (OnCustomerListFragmentListener) activity;
    } else {
      throw new RuntimeException(activity.toString() + " must implement OnCustomerListFragmentListener");
    }
  }

  @Override public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  private OnCustomerListFragmentListener getListener() {
    if(mListener == null) {
      if (getActivity() instanceof OnCustomerListFragmentListener) {
        mListener = (OnCustomerListFragmentListener) getActivity();
      } else {
        throw new RuntimeException(getActivity().toString() + " must implement OnCustomerListFragmentListener");
      }
    }
    return mListener;
  }
  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p/>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnCustomerListFragmentListener {
    void onAccountSelected(CustomerAccount acct);
  }

  private void setFilter(String filter) {
    Log.d(TAG, "setFilter: " + filter);
    adapter.applyFilter(filter);
//    setFilter(adapter.getFilter());
    mListView.setAdapter(adapter);
    filterTextView.setText(adapter.getFilter() == null ? "" : formatPhone(filter));
  }

  private String formatPhone(String filter) {
    if(filter.length() != 7 || filter.length() != 10) {
      return filter;
    } if(filter.length() == 7) {
      return filter.substring(0, 3) + "." + filter.substring(3);
    } else {
      return filter.substring(0, 3) + "." + filter.substring(3, 6) + "." + filter.substring(7);
    }
  }

  private enum BUTTON {
    ONE {
      @Override String process(String currentFilter) {
        return currentFilter + "1";
      }
    },
    TWO {
      @Override String process(String currentFilter) {
        return currentFilter + "2";
      }
    },
    THREE {
      @Override String process(String currentFilter) {
        return currentFilter + "3";
      }
    },
    FOUR {
      @Override String process(String currentFilter) {
        return currentFilter + "4";
      }
    },
    FIVE {
      @Override String process(String currentFilter) {
        return currentFilter + "5";
      }
    },
    SIX {
      @Override String process(String currentFilter) {
        return currentFilter + "6";
      }
    },
    SEVEN {
      @Override String process(String currentFilter) {
        return currentFilter + "7";
      }
    },
    EIGHT {
      @Override String process(String currentFilter) {
        return currentFilter + "8";
      }
    },
    NINE {
      @Override String process(String currentFilter) {
        return currentFilter + "9";
      }
    },
    ZERO {
      @Override String process(String currentFilter) {
        return currentFilter + "0";
      }
    },
    BACK {
      @Override String process(String currentFilter) {
        return currentFilter.substring(0, Math.max(0, currentFilter.length()-1));
      }
    },
    CLEAR {
      @Override String process(String currentFilter) {
        return "";
      }
    };

    String process(String currentFilter) {
      return currentFilter;
    }
  }

  public void buttonClicked(String tag) {
    BUTTON button = BUTTON.valueOf(tag);
    String filter = button.process(adapter.getFilter());
    setFilter(filter == null ? "" : filter);
  }
}
