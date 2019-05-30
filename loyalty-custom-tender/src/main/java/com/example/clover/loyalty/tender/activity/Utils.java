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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.Currency;

/**
 * Created by mmaietta on 9/9/15.
 */
public class Utils {

    public static String nextRandomId() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    public static String longToAmountString(Currency currency, long amt) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        if (currency != null)
            format.setCurrency(currency);

        double currencyAmount = (double) amt / Math.pow(10.0D, (double) format.getCurrency().getDefaultFractionDigits());

        return format.format(currencyAmount);
    }
}