package com.clover.loyalty.example.pos;
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

import android.content.DialogInterface;
import android.widget.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.net.URI;
import java.net.URISyntaxException;

public class StartupActivity extends Activity {

  public static final String TAG = StartupActivity.class.getSimpleName();
  public static final String EXAMPLE_APP_NAME = "EXAMPLE_APP";
  public static final String LAN_PAY_DISPLAY_URL = "LAN_PAY_DISPLAY_URL";
  public static final String CONNECTION_MODE = "CONNECTION_MODE";
  public static final String USB = "USB";
  public static final String LAN = "LAN";
  public static final String WS_CONFIG = "WS";
  boolean enableQuickpay = false, enableVas = false, enablePhone = true, enableId = true, enableBarcode = true;
  String serverIp;
  String serverPort;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_startup);

    loadBaseURL();

    if (null != getActionBar()) {
      getActionBar().hide();
    }

    RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup);
    group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
        TextView textView = (TextView) findViewById(R.id.lanPayDisplayAddress);
        textView.setEnabled(checkedId == R.id.lanRadioButton);
      }
    });

    Button connectButton = (Button)findViewById(R.id.connectButton);
    connectButton.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        cleanConnect(v);
        return true;
      }
    });

    // initialize...
    TextView textView = (TextView) findViewById(R.id.lanPayDisplayAddress);
    String url = this.getSharedPreferences(EXAMPLE_APP_NAME, Context.MODE_PRIVATE).getString(LAN_PAY_DISPLAY_URL,  getString(R.string.lan_pay_address));

    textView.setText(url);
    textView.setEnabled(((RadioGroup)findViewById(R.id.radioGroup)).getCheckedRadioButtonId() == R.id.lanRadioButton);

    String mode = this.getSharedPreferences(EXAMPLE_APP_NAME, Context.MODE_PRIVATE).getString(CONNECTION_MODE, USB);

    ((RadioButton)findViewById(R.id.lanRadioButton)).setChecked(LAN.equals(mode));
    ((RadioButton)findViewById(R.id.usbRadioButton)).setChecked(!LAN.equals(mode));

    // QuickPay may include VAS.  Allow the pass endpoint to be configured.
    ((CheckBox)findViewById(R.id.cbQUICKPAY)).setChecked(enableQuickpay);
    ((CheckBox)findViewById(R.id.cbQUICKPAY)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        enableQuickpay = isChecked;
        StartupActivity.this.onCheckedChanged(buttonView, isChecked);
      }
    });

    ((CheckBox)findViewById(R.id.cbVAS)).setChecked(enableVas);
    ((CheckBox)findViewById(R.id.cbVAS)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        enableVas = isChecked;
        StartupActivity.this.onCheckedChanged(buttonView, isChecked);
      }
    });
    ((CheckBox)findViewById(R.id.cbPhone)).setChecked(enablePhone);
    ((CheckBox)findViewById(R.id.cbPhone)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        enablePhone = isChecked;
      }
    });
    ((CheckBox)findViewById(R.id.cbId)).setChecked(enableId);
    ((CheckBox)findViewById(R.id.cbId)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        enableId = isChecked;
      }
    });
    ((CheckBox)findViewById(R.id.cbBarcode)).setChecked(enableBarcode);
    ((CheckBox)findViewById(R.id.cbBarcode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        enableBarcode = isChecked;
      }
    });
  }

  private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    LinearLayout vasInfo = findViewById(R.id.vasInfo);
    vasInfo.setVisibility(View.GONE);
    if (isChecked) {
      vasInfo.setVisibility(View.VISIBLE);
    }
  }


  private boolean loadBaseURL() {

    String _serverBaseURL = PreferenceManager.getDefaultSharedPreferences(this).getString(CloverLoyaltyPOSActivity.EXAMPLE_POS_SERVER_KEY, "wss://10.0.0.101:12345/remote_pay");

    TextView tv = (TextView)findViewById(R.id.lanPayDisplayAddress);
    tv.setText(_serverBaseURL);

    Log.d(TAG, _serverBaseURL);
    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  public void cleanConnect(View view) {
    connect(view, true);
  }

  public void connect(View view) {
    connect(view, false);
  }

  public void connect(View view, boolean clearToken) {

    RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup);
    SharedPreferences prefs = this.getSharedPreferences(EXAMPLE_APP_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    URI uri = null;
    String config;

    EditText ip = findViewById(R.id.computer_ip_address);
    serverIp = ip.getText().toString();
    EditText port = findViewById(R.id.computer_port);
    serverPort = port.getText().toString();

    if(group.getCheckedRadioButtonId() == R.id.usbRadioButton) {
      config = USB;
      editor.putString(CONNECTION_MODE, USB);
      editor.apply();
    } else { // (group.getCheckedRadioButtonId() == R.id.lanRadioButton)
      String uriStr = ((TextView)findViewById(R.id.lanPayDisplayAddress)).getText().toString();
      config = WS_CONFIG;
      uri = parseValidateAndStoreURI(uriStr);
    }
    connect(uri, config, clearToken);
  }

  private void connect(URI uri, String config, boolean clearToken) {
    Intent intent = new Intent();
    intent.setClass(this, CloverLoyaltyPOSActivity.class);

    if(config.equals("USB") || (config.equals(WS_CONFIG) && uri != null)) {
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_CLOVER_CONNECTOR_CONFIG, config);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_CLEAR_TOKEN, clearToken);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_WS_ENDPOINT, uri);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_ENABLE_QUICK_PAY, enableQuickpay);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_ENABLE_VAS, enableVas);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_ENABLE_BARCODE, enableBarcode);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_ENABLE_PHONE, enablePhone);
      intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_ENABLE_ID, enableId);
      if (!serverIp.isEmpty() && !serverPort.isEmpty()) {
        intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_SERVER_IP, serverIp);
        intent.putExtra(CloverLoyaltyPOSActivity.EXTRA_SERVER_PORT, serverPort);
      }

      startActivity(intent);
    }
  }

  private URI parseValidateAndStoreURI(String uriStr) {
    try {
      SharedPreferences prefs = this.getSharedPreferences(EXAMPLE_APP_NAME, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      URI uri = new URI(uriStr);
      String addressOnly = String.format("%s://%s:%d%s", uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath());
      editor.putString(LAN_PAY_DISPLAY_URL, addressOnly);
      editor.putString(CONNECTION_MODE, LAN);
      editor.apply();
      return uri;
    } catch(URISyntaxException e) {
      Log.e(TAG, "Invalid URL" ,e);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Error");
      builder.setMessage("Invalid URL");
      builder.show();
      return null;
    }
  }
}
