package clover.db;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;
  private String uuid = UUID.randomUUID().toString();
  public String name;
  public String phone;
  public int points;
  public String  accountTransactions; //JSON list -- List<AccountTransaction>
  public String email;
  public String externalId;
  public String deviceIdentifier;
  public String devicePushToken;
  public String passSerialNumber;

  public String getPassSerialNumber() {
    return passSerialNumber;
  }

  public void setPassSerialNumber(String passSerialNumber) {
    this.passSerialNumber = passSerialNumber;
  }

  public int getId() {
    return id;
  }

  public String getUuid() {
    return uuid;
  }

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

  public String getAccountTransactions() {
    return accountTransactions;
  }

  public void setAccountTransactions(String accountTransactions) {
    this.accountTransactions = accountTransactions;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public String getDeviceIdentifier() {
    return deviceIdentifier;
  }

  public void setDeviceIdentifier(String deviceIdentifier) {
    this.deviceIdentifier = deviceIdentifier;
  }

  public String getDevicePushToken() {
    return devicePushToken;
  }

  public void setDevicePushToken(String devicePushToken) {
    this.devicePushToken = devicePushToken;
  }


}
