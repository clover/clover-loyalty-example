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

package com.clover.loyalty.example.pos.model;

import com.clover.loyalty.example.pos.accounts.CustomerAccounts;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class POSOrder {

  public enum OrderStatus {
    OPEN, CLOSED, LOCKED, PAID, INITIAL, PARTIALLY_PAID {
      @Override public String toString() {
        return "PARTIALLY PAID";
      }
    }
  }

  private CustomerAccounts.CustomerAccount customerAccount;

  private List<POSLineItem> items;
  private List<POSTransaction> payments;
  private POSPayment preAuth;
  private POSDiscount discount;
  private transient String pendingPaymentId;
  public String id;
  public Date date;

  private transient List<OrderObserver> observers = new ArrayList<>();
  private final String TAG = POSOrder.class.getSimpleName();

  POSOrder() {
    items = new ObservableList<>();
    payments = new ObservableList<>();
    discount = new POSDiscount("None", 0);
    date = new Date();
  }

  void addOrderObserver(OrderObserver observer) {
    this.observers.add(observer);
  }

  void removeObserver(OrderObserver observer) {
    this.observers.remove(observer);
  }

  public long getPreDiscountSubTotal() {
    long sub = 0;
    for (POSLineItem li : items) {
      sub += li.getPrice() * li.getQuantity();
    }
    return sub;
  }

  long getPreTaxSubTotal() {
    long sub = 0;
    for (POSLineItem li : items) {
      sub += li.getPrice() * li.getQuantity();
    }
    if (discount != null) {
      sub = discount.appliedTo(sub);
    }
    return sub;
  }

  public long getTippableAmount() {
    long tippableAmount = 0;
    for (POSLineItem li : items) {
      if (li.getItem().isTippable()) {
        tippableAmount += li.getPrice() * li.getQuantity();
      }
    }
    if (discount != null) {
      tippableAmount = discount.appliedTo(tippableAmount);
    }
    return tippableAmount + getTaxAmount(); // shuold match Total if there aren't any "non-tippable" items

  }

  private long getTaxableSubtotal() {
    long sub = 0;
    for (POSLineItem li : items) {
      if (li.getItem().isTaxable()) {
        sub += li.getPrice() * li.getQuantity();
      }
    }
    if (discount != null) {
      sub = discount.appliedTo(sub);
    }
    return sub;
  }

  private long getTaxAmount() {
    return (int) (getTaxableSubtotal() * 0.07);
  }

  private long getTotal() {
    return getPreTaxSubTotal() + getTaxAmount();
  }

  public long getTips() {
    long tips = 0;
    for (POSTransaction posPayment : payments) {
      if (posPayment instanceof POSPayment) {
        tips += ((POSPayment) posPayment).getTipAmount();
      }
    }
    return tips;
  }

  public boolean remoteAllItems(POSLineItem li) {
    boolean removed = items.remove(li);
    if(removed) {
      notifyObserverItemRemoved(li);
    }
    return removed;
  }

  public void setPendingPaymentId (String pendingPaymentId){
    Log.d(TAG, "externalPaymentID set to : " + pendingPaymentId);
    this.pendingPaymentId = pendingPaymentId;
  }

  public String getPendingPaymentId(){
    Log.d(TAG,"returning externalPaymentID: "+pendingPaymentId);
    return pendingPaymentId;
  }


  void addPayment(POSPayment payment) {
    payments.add(payment);
    payment.setOrder(this);
    notifyObserverPaymentAdded(payment);
  }


  void addRefund(POSRefund refund) {
    for (POSTransaction pay : payments) {
      if (pay instanceof POSPayment) {
        if (pay.getId().equals(refund.getId())) {
          ((POSPayment) pay).setPaymentStatus(POSPayment.Status.REFUNDED);
          notifyObserverPaymentChanged(pay);
        }

      }
    }
    payments.add(refund);
    notifyObserverRefundAdded(refund);
  }

  public POSOrder.OrderStatus getStatus() {
    if(items.size() == 0 && payments.size() == 0) {
      return OrderStatus.INITIAL;
    } else {
      long totalPaid = 0;
      for(POSTransaction payment : payments) {
        if(payment instanceof POSPayment) {
          totalPaid += payment.getAmount();
        } else if(payment instanceof POSRefund) {
          totalPaid -= payment.getAmount();
        }
      }
      if(getTotal() > 0 && totalPaid >= getTotal()) {
        return OrderStatus.PAID;
      } else if (totalPaid > 0) {
        return OrderStatus.PARTIALLY_PAID;
      } else {
        return OrderStatus.OPEN;
      }
    }
  }


  protected void removeItem(POSLineItem selectedLineItem) {
    items.remove(selectedLineItem);
    notifyObserverItemRemoved(selectedLineItem);
  }


  public List<POSLineItem> getItems() {
    return items;
  }

  List<POSTransaction> getPayments() {
    return Collections.unmodifiableList(payments);
  }

  public POSPayment getPreAuth() {
    return preAuth;
  }

  public void setPreAuth(POSPayment preAuth) {
    this.preAuth = preAuth;
  }

  public void setDiscount(POSDiscount discount) {
    if(this.discount == discount){
      this.discount = null;
      Log.d(TAG, "discount is the same, removing");
      notifyObserverDiscountChanged(null);
    }
    else {
      this.discount = discount;
      notifyObserverDiscountChanged(discount);
    }
  }

  public void setCustomerAccount(CustomerAccounts.CustomerAccount account) {
    this.customerAccount = account;
    notifyObserverCustomerChanged(customerAccount);
  }

  public CustomerAccounts.CustomerAccount getCustomerAccount() {
    return customerAccount;
  }

  public POSDiscount getDiscount() {
    return discount;
  }

  private void notifyObserverCustomerChanged(CustomerAccounts.CustomerAccount account) {
    for (OrderObserver observer : observers) {
      observer.customerChanged(this, account);
    }
  }
  void notifyObserverItemAdded(POSLineItem targetItem) {
    for (OrderObserver observer : observers) {
      observer.lineItemAdded(this, targetItem);
    }
  }

  void notifyObserverItemChanged(POSLineItem targetItem) {
    for (OrderObserver observer : observers) {
      observer.lineItemChanged(this, targetItem);
    }
  }

  private void notifyObserverPaymentAdded(POSPayment payment) {
    for (OrderObserver observer : observers) {
      observer.paymentAdded(this, payment);
    }
  }

  private void notifyObserverRefundAdded(POSRefund refund) {
    for (OrderObserver observer : observers) {
      observer.refundAdded(this, refund);
    }
  }

  void notifyObserverPaymentChanged(POSTransaction pay) {
    for (OrderObserver observer : observers) {
      observer.paymentChanged(this, pay);
    }
  }

  private void notifyObserverItemRemoved(POSLineItem lineItem) {
    for (OrderObserver observer : observers) {
      observer.lineItemRemoved(this, lineItem);
    }
  }

  private void notifyObserverDiscountChanged(POSDiscount discount) {
    for (OrderObserver observer : observers) {
      observer.discountChanged(this, discount);
    }
  }
}
