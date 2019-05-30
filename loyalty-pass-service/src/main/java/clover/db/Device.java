package clover.db;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Device {

  @Id
  private String deviceLibraryIdentifier;
  private String pushToken;

  public String getDeviceLibraryIdentifier() {
    return deviceLibraryIdentifier;
  }

  public void setDeviceLibraryIdentifier(String deviceLibraryIdentifier) {
    this.deviceLibraryIdentifier = deviceLibraryIdentifier;
  }

  public String getPushToken() {
    return pushToken;
  }

  public void setPushToken(String pushToken) {
    this.pushToken = pushToken;
  }



}
