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

package com.example.clover.loyalty.tender.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.clover.loyalty.ILoyaltyDataService;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v3.base.Tender;
import com.clover.sdk.v3.customers.CustomerInfo;
import com.clover.sdk.v3.loyalty.LoyaltyDataConfig;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.payments.ServiceChargeAmount;
import com.example.clover.loyalty.tender.CustomerAccount;
import com.example.clover.loyalty.tender.LoyaltyHelper;
import com.example.clover.loyalty.tender.R;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class MerchantLoyaltyTenderActivity extends BaseLoyaltyActivity {

  private TextView customerMessage;
  private String orderId;
  private TextView points;
  private long amount;
  private ImageView swipeImage;
  private ImageView scanImage;
  private ImageView tapImage;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
//    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.activity_merchant_loyalty_tender);

    setResult(RESULT_CANCELED);

    /**
     * @see Intents.ACTION_MERCHANT_TENDER
     */
    amount = getIntent().getLongExtra(Intents.EXTRA_AMOUNT, 0);
    final Currency currency = (Currency) getIntent().getSerializableExtra(Intents.EXTRA_CURRENCY);
    final long taxAmount = getIntent().getLongExtra(Intents.EXTRA_TAX_AMOUNT, 0);
    final ArrayList taxableAmounts = getIntent().getParcelableArrayListExtra(Intents.EXTRA_TAXABLE_AMOUNTS);
    final ServiceChargeAmount serviceCharge = getIntent().getParcelableExtra(Intents.EXTRA_SERVICE_CHARGE_AMOUNT);

    orderId = getIntent().getStringExtra(Intents.EXTRA_ORDER_ID);
    final String merchantId = getIntent().getStringExtra(Intents.EXTRA_MERCHANT_ID);

    final Tender tender = getIntent().getParcelableExtra(Intents.EXTRA_TENDER);

    // Merchant Facing specific fields
    final Order order = getIntent().getParcelableExtra(Intents.EXTRA_ORDER);
    final String note = getIntent().getStringExtra(Intents.EXTRA_NOTE);

    setupViews(amount, currency, orderId, merchantId);
  }

  @Override protected void onStart() {
    start("MSR"); // start listening for swipe
    start("QR_BARCODE"); // start listening for barcode

    // for Mini, we could start the scanner, for Flex, we could just use the button
    // We could also start the scanner
    /*
    BarcodeScanner scanner = new BarcodeScanner();
    Bundle bundle = new Bundle();
    scanner.startScan(new Bundle());
    */


    super.onStart();
  }

  @Override protected void onStop() {
    super.onStop();

    stop("MSR");
    stop("QR_BARCODE");
  }

  @Override public void onLoyaltyDataLoaded(List<LoyaltyDataConfig> loyaltyDataConfigList) {
    // now we decide what we should start, because we know what has been asked for
    for (LoyaltyDataConfig config : loyaltyDataConfigList) {
      if ("MSR".equals(config.getType())) {
        swipeImage.setVisibility(View.VISIBLE);
      } else if ("QR_BARCODE".equals(config.getType())) {
        scanImage.setVisibility(View.VISIBLE);
      } else if ("VAS".equals(config.getType())) {
        tapImage.setVisibility(View.VISIBLE);
      }
    }
  }

  @Override public void onLoyaltyServiceStateChanged(String configType, String state) {
      if ("MSR".equals(configType)) {
        swipeImage.setVisibility(state.equals(ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING) ? View.VISIBLE : View.INVISIBLE);
      } else if ("QR_BARCODE".equals(configType)) {
        scanImage.setVisibility(state.equals(ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING) ? View.VISIBLE : View.INVISIBLE);
      } else if ("VAS".equals(configType)) {
        tapImage.setVisibility(state.equals(ILoyaltyDataService.LOYALTY_SERVICE_STATE_EVENT_RUNNING) ? View.VISIBLE : View.INVISIBLE);
      }
  }

  public void setupViews(final long amount, Currency currency, String orderId, String merchantId) {
    TextView amountText = (TextView) findViewById(R.id.text_amount);
    amountText.setText(Utils.longToAmountString(currency, amount));

    TextView orderIdText = (TextView) findViewById(R.id.text_orderid);
    orderIdText.setText(orderId);
    TextView merchantIdText = (TextView) findViewById(R.id.text_merchantid);
    merchantIdText.setText(merchantId);

    customerMessage = (TextView) findViewById(R.id.customer_message);
    points = (TextView) findViewById(R.id.customer_points);

    Button approveButton = (Button) findViewById(R.id.payButton);
    approveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent data = new Intent();
        data.putExtra(Intents.EXTRA_AMOUNT, amount);
        data.putExtra(Intents.EXTRA_CLIENT_ID, Utils.nextRandomId());
        data.putExtra(Intents.EXTRA_NOTE, "Transaction Id: " + Utils.nextRandomId());

        setResult(RESULT_OK, data);
        finish();
      }
    });

    Button declineButton = (Button) findViewById(R.id.declineButton);
    declineButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent data = new Intent();
        data.putExtra(Intents.EXTRA_DECLINE_REASON, "You pressed the decline button");

        setResult(RESULT_CANCELED, data);
        finish();
      }
    });

    swipeImage = (ImageView) findViewById(R.id.swipe_img);
    scanImage = (ImageView) findViewById(R.id.scan_img);
    tapImage = (ImageView) findViewById(R.id.tap_img);

    CustomerAccount customerAccount = LoyaltyHelper.getCustomerAccountForOrderId(this, orderId);
    if (customerAccount != null) {
      CustomerInfo customerInfo = LoyaltyHelper.getCustomerInfo(customerAccount);
      onCustomerSelected(customerInfo);
    }
  }

  @Override public void onCustomerSelected(CustomerInfo customerInfo) {
    super.onCustomerSelected(customerInfo);
    customerMessage.setText(String.format(getString(R.string.welcome_message), customerInfo.getDisplayString()));
    String points = customerInfo.getExtras().get("POINTS");
    int pts = Integer.parseInt(points);

    this.points.setText(String.format(getString(R.string.balance_msg), points));

    findViewById(R.id.payButton).setVisibility(pts > (amount / 100.0) ? View.VISIBLE : View.GONE);

    LoyaltyHelper.registerOrderForCustomer(this, orderId, customerInfo.getExternalId());

    swipeImage.setVisibility(View.INVISIBLE);
    scanImage.setVisibility(View.INVISIBLE);

  }
}
