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

package com.example.clover.loyalty.tender;

public class CustomerAccount {

  public CustomerAccount() {

  }

  public CustomerAccount(String id, String firstName, String lastName, String accountId, String phoneNumber, int points) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.accountId = accountId;
    this.phoneNumber = phoneNumber;

    this.points = points;

    this.orders = new String[0];
  }

  public String id;
  public String firstName;
  public String lastName;
  public String accountId;
  public String phoneNumber;

  public String[] orders;

  public int points = 0;
}
