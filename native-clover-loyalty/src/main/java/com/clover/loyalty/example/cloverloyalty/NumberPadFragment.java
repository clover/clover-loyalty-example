package com.clover.loyalty.example.cloverloyalty;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NumberPadFragment extends Fragment implements View.OnClickListener {

  public static NumberPadFragment newInstance() {
    Bundle bundle = new Bundle();
    NumberPadFragment fragment = new NumberPadFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    View view =  inflater.inflate(R.layout.fragment_numberpad, container, false);
    view.findViewById(R.id.button1).setOnClickListener(this);
    view.findViewById(R.id.button2).setOnClickListener(this);
    view.findViewById(R.id.button3).setOnClickListener(this);
    view.findViewById(R.id.button4).setOnClickListener(this);
    view.findViewById(R.id.button5).setOnClickListener(this);
    view.findViewById(R.id.button6).setOnClickListener(this);
    view.findViewById(R.id.button7).setOnClickListener(this);
    view.findViewById(R.id.button8).setOnClickListener(this);
    view.findViewById(R.id.button9).setOnClickListener(this);
    view.findViewById(R.id.button0).setOnClickListener(this);
    view.findViewById(R.id.buttonBack).setOnClickListener(this);
    view.findViewById(R.id.buttonClear).setOnClickListener(this);
    return view;
  }

  @Override public void onClick(View view) {
    ((NumberPadFragmentListener)getActivity()).onNumberButton(view.getTag().toString());
  }

  public interface NumberPadFragmentListener {
    void onNumberButton(String tag);
  }
}
