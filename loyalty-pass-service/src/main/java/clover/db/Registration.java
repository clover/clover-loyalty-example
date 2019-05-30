package clover.db;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Registration {
  @Id
  private String passSerialNumber;
  private String deviceId;
  private String passTypeIdentifer;
  private boolean notifications;

  public String getPassTypeIdentifer() {
    return passTypeIdentifer;
  }

  public void setPassTypeIdentifer(String passTypeIdentifer) {
    this.passTypeIdentifer = passTypeIdentifer;
  }



  public boolean isNotifications() {
    return notifications;
  }

  public void setNotifications(boolean notifications) {
    this.notifications = notifications;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getPassSerialNumber() {
    return passSerialNumber;
  }

  public void setPassSerialNumber(String passId) {
    this.passSerialNumber = passId;
  }

}
