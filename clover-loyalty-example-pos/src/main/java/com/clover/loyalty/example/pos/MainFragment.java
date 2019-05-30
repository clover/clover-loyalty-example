package com.clover.loyalty.example.pos;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends Fragment {

  public static MainFragment newInstance(){
    MainFragment fragment = new MainFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  public MainFragment(){

  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main, container, false);

    ((TextView)view.findViewById(R.id.outputText)).setMovementMethod(new ScrollingMovementMethod());
    ((TextView)view.findViewById(R.id.outputText)).setText("Starting...", TextView.BufferType.EDITABLE);

    return view;
  }
}
