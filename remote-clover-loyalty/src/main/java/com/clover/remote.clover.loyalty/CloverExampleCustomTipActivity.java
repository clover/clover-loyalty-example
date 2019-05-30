package com.clover.remote.clover.loyalty;
/*
 * Copyright (C) 2016 Clover Network, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.clover.cfp.activity.CFPConstants;
import com.clover.cfp.activity.CloverCFPActivity;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * An example custom activity that models custom tip collection.
 *
 * This example accepts tip configurations that model either a percentage, or a static amount.
 *
 * Additionally, this example does an intelligent display whereby it does not show amounts less than $1, instead
 * showing $1 as a mininmum suggested tip amount.
 *
 * Enhancements could include a button to allow fo custom tip amounts to be entered.
 *
 *
 * Created by michaelhampton on 4/9/19.
 */
public class CloverExampleCustomTipActivity extends CloverCFPActivity {

  private static String TAG = CloverExampleCustomTipActivity.class.getSimpleName();
  private Gson gson = new Gson();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_custom_tip);

    // Get the initialization payload sent in with the CustomActivityRequest as the payload.
    String payload = getIntent().getStringExtra(CFPConstants.EXTRA_PAYLOAD);
    // The payload must be parsed.
    CustomTipConfigurationMessage customTipConfigurationMessage = null;
    try {
      customTipConfigurationMessage = gson.fromJson(payload, CustomTipConfigurationMessage.class);
    } catch(Exception e) {
      Log.d(TAG, String.format("Could not parse passed payload to a CustomTipConfigurationMessage, %s", payload));
    }

    if (customTipConfigurationMessage != null) {

      if (customTipConfigurationMessage.tipConfigs != null) {
        // Build a table of tip suggestions
        TableRow tr = null;
        TableLayout mTlayout = findViewById(R.id.tipbuttons);
        int i = 0;
        // This flag indicates if we already have a $1 tip already.  We
        // Would probably want to have additional de-duplicate checking in a prod application.
        boolean onDollarTipAlreadyIncluded = false;
        for (CustomTipConfiguration tipConfig : customTipConfigurationMessage.tipConfigs) {
          Spanned text;
          String tipString;
          Float tipAmount;
          final Float finalTipAmount;
          // If the tip configuration is a percentage...
          if (tipConfig.percentage) {
            tipAmount = (float) Math.round((customTipConfigurationMessage.amountToBaseTipOn * tipConfig.value.longValue()) * 0.01f);
            tipString = String.format("%d&#37; ($ %.2f)", tipConfig.value, tipAmount.floatValue() / 100f);
            // Add some smart tip logic here.
            if (tipAmount < 100) {
              // The tip suggestion results in a tip less than a dollar, do not encourage the customer to be so cheap.
              // Make the minimum tip a dollar
              tipAmount = 100f;
              tipString = "$ 1";
            }
          } else {
            tipAmount = Float.valueOf(tipConfig.value * 100); // specified as dollars
            tipString = String.format("$ %d", tipConfig.value);
          }
          if (tipAmount == 100) {
            if (onDollarTipAlreadyIncluded) {
              // We already have a $1 tip, do not add another
              continue;
            } else {
              onDollarTipAlreadyIncluded = true;
            }
          }

          // Ok, time to do the UI.
          // Make a button dynamically...
          Button btn = new Button(this);
          // Make rows of two buttons per row...
          if (i % 2 == 0) {
            tr = new TableRow(this);
            mTlayout.addView(tr);
          }
          // Calculate the final amount with the base + tip...
          final Float totalAmount = Float.valueOf(customTipConfigurationMessage.amountToBaseTipOn + tipAmount);
          // Make a label for the button....
          String styledText = String.format("<font color='#000000'>"
                                            + "%s"
                                            + "</font><br/>"
                                            + "<font color='#000000'>"
                                            + "Total will be %.2f"
                                            + "</font>", tipString, totalAmount/100f);

          // Set the label on the button...
          text = Html.fromHtml(styledText);
          btn.setText(text);
          // Set an id for the button...
          btn.setId(i);
          // Set up what happens when the button is clicked.
          // In this example, we send the tip to the merchant facing process.
          finalTipAmount = tipAmount;
          btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              sendAddTip(finalTipAmount.longValue());
            }
          });
          // Put the button in the row
          tr.addView(btn);
          // Go o nto the (potential) next one
          i++;
        }
      }
      if (customTipConfigurationMessage.amountToBaseTipOn != null) {
        TextView baseAmount = findViewById(R.id.baseAmount);
        float baseAmountDisp = customTipConfigurationMessage.amountToBaseTipOn.floatValue() / 100f;
        baseAmount.setText(String.format("Tip for sale amount $%.2f", baseAmountDisp));
      }
    }
  }

  /**
   * Send a custom message to the merchant facing process.  The merchant facing process must
   * understand how to parse this message.
   * @param amount the tip amount we will send back
   */
  private void sendAddTip(long amount) {
    try {
      String message = gson.toJson(new CustomTipSelectedMessage(amount));
      Log.d(this.getClass().getSimpleName(), String.format("sending message: %s", message));
      sendMessage(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendNoTip(View view) {
    try {
      String message = gson.toJson(new CustomTipSelectedMessage(0L));
      Log.d(this.getClass().getSimpleName(), String.format("sending message: %s", message));
      sendMessage(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onMessage(String s) {
    try {
      Log.d(this.getClass().getSimpleName(), String.format("got message: %s", s));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
  Note: these classes should be in a shared library with the example custom POS.
    We have not done this for simplicity.
   */
  class CustomTipSelectedMessage {
    final Long tipAmount;

    CustomTipSelectedMessage(Long tipAmount) {
      this.tipAmount = tipAmount;
    }
  }

  public class CustomTipConfigurationMessage {
    final Long amountToBaseTipOn;
    final ArrayList<CustomTipConfiguration> tipConfigs;

    public CustomTipConfigurationMessage(Long amountToBaseTipOn, ArrayList<CustomTipConfiguration> tipConfigs) {
      this.amountToBaseTipOn = amountToBaseTipOn;
      this.tipConfigs = tipConfigs;
    }
  }

  public class CustomTipConfiguration {
    final boolean percentage;
    final Long value;

    public CustomTipConfiguration(boolean isPercentage, Long value) {
      percentage = isPercentage;
      this.value = value;
    }
  }
}
