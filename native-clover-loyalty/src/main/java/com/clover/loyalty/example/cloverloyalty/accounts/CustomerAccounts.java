package com.clover.loyalty.example.cloverloyalty.accounts;



import com.clover.loyalty.example.cloverloyalty.model.POSDiscount;
import com.clover.loyalty.example.cloverloyalty.model.POSItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/*
 * A class for creating mock customer accounts
 */
public class CustomerAccounts {

  /**
   * An array of sample (dummy) items.
   */
  public static final List<CustomerAccount> ITEMS = new ArrayList<CustomerAccount>();

  /**
   * A map of sample (dummy) items, by ID.
   */
  //  public static final Map<String, CustomerAccount> ITEM_MAP = new HashMap<String, CustomerAccount>();

  private static final int COUNT = 50;


  private static void addItem(CustomerAccount item) {
    ITEMS.add(item);
  }

  private static List<AccountTransaction> createTransactions(int count) {
    List<AccountTransaction> transactions = new ArrayList<>();
    transactions.add(new AccountTransaction(UUID.randomUUID().toString().replace("-","").substring(0, 13), new Date(), (int)(Math.floor(Math.random())) + 1));

    for(int i=1; i<count; i++) {
      transactions.add(new AccountTransaction(UUID.randomUUID().toString().replace("-","").substring(0, 13), new Date(), (int)((Math.floor(10 * Math.random())) + 1) * (Math.random() < 0.9 ? 1 : -1)));
    }
    return transactions;
  }

  private static String createPhone(int index) {
    int start = 5;
    String phone = "";
    for(int i=0;i<10; i++) {
      start =  (start + index*index + 1) % 10;
      phone += start;

    }
    return phone;
  }

  public static Offer getOfferById(String id) {
    for (Offer offer : allOffers) {
      if (offer.id.equals(id)) {
        return offer;
      }
    }
    return null;
  }

  public static Offer[] allOffers = new Offer[] {
      new Offer("R1911", "Free small drink", "", 10, new POSItem("R1911", "Small Drink", 0), null),
      new Offer("2J411", "$5 off", "", 50, null, new POSDiscount("$5 Off", 500)),
      new Offer("P4610", "10% Off", "", 40, null, new POSDiscount("10% Off", 0.1f))
  };

  public static Offer[] computeOffers(CustomerAccount account) {
    List<Offer> offers = new ArrayList<>();
    if (account.points > 10) {
      offers.add(allOffers[0]);
    }
    if (account.points > 60) {
      offers.add(allOffers[1]);
    }
    if (account.points > 100) {
      offers.add(allOffers[2]);
    }
    return offers.toArray(new Offer[offers.size()]);
  }

  public static class Offer implements Serializable {
    public Offer(String id, String label, String description, int cost, POSItem item, POSDiscount discount) {
      this.id = id;
      this.label = label;
      this.description = description;
      this.cost = cost;
      this.item = item;
      this.discount = discount;
    }
    public POSDiscount discount;
    public POSItem item;
    String id = UUID.randomUUID().toString().replace("-", "");
    String label = "";
    String description;
    int cost = 0;
  }

  /**
   * A Loyalty customer account object
   */
  public static class CustomerAccount implements Serializable {
    public final int id;
    public final String uuid;
    public String name;
    public String phone;
    public int points;
    public List<AccountTransaction> accountTransactions;
    public String email;


    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }


    public int getPoints() {
      return points;
    }

    public void setPoints(int points) {
      this.points = points;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public CustomerAccount(int id, String uuid, String name, String phone, int points, String email, List<AccountTransaction> transactions) {
      this.id = id;
      this.uuid = uuid;
      this.name = name;
      this.phone = phone;
      this.points = points;
      this.email = email;
      this.accountTransactions = transactions;
    }


    @Override
    public String toString() {
      return "CustomerAccount{" +
          "id=" + id +
          ", uuid='" + uuid + '\'' +
          ", name='" + name + '\'' +
          ", phone='" + phone + '\'' +
          ", points=" + points +
          ", email='" + email + '\'' +
          '}';
    }
  }

  public static class AccountTransaction implements Serializable {
    public final Date date;
    public final long amount;
    public final String orderId;

    public AccountTransaction(String orderId, Date date, long amount) {
      this.amount = amount;
      this.orderId = orderId;
      this.date = date;
    }
  }
}
