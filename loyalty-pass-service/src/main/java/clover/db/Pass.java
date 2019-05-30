package clover.db;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Pass {

  @Id
  private String serialNumber;
  private String passTypeIdentifier;
  private String lastUpdate;
  private Integer formatVersion;
  private String teamIdentifier;
  private String webServiceURL;
  private String locations; //json of list
  private String barcode; // json of PKBarcode
  private String organizationName;
  private String description;
  private String logoText;
  private String foregroundColor;
  private String backgroundColor;
  private String storeCard; //json of PKStoreCard
  private String nfc; //json of PKNFC
  private String authenticationToken;

  public String getAuthenticationToken() {
    return authenticationToken;
  }

  public void setAuthenticationToken(String authenticationToken) {
    this.authenticationToken = authenticationToken;
  }


  public Integer getFormatVersion() {
    return formatVersion;
  }

  public void setFormatVersion(Integer formatVersion) {
    this.formatVersion = formatVersion;
  }

  public String getTeamIdentifier() {
    return teamIdentifier;
  }

  public void setTeamIdentifier(String teamIdentifier) {
    this.teamIdentifier = teamIdentifier;
  }

  public String getWebServiceURL() {
    return webServiceURL;
  }

  public void setWebServiceURL(String webServiceURL) {
    this.webServiceURL = webServiceURL;
  }

  public String getLocations() {
    return locations;
  }

  public void setLocations(String locations) {

    this.locations = locations;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLogoText() {
    return logoText;
  }

  public void setLogoText(String logoText) {
    this.logoText = logoText;
  }

  public String getForegroundColor() {
    return foregroundColor;
  }

  public void setForegroundColor(String foregroundColor) {
    this.foregroundColor = foregroundColor;
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(String backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  public String getStoreCard() {
    return storeCard;
  }

  public void setStoreCard(String storeCard) {
    this.storeCard = storeCard;
  }

  public String getNfc() {
    return nfc;
  }

  public void setNfc(String nfc) {
    this.nfc = nfc;
  }

  public String getPassTypeIdentifier() {
    return passTypeIdentifier;
  }

  public void setPassTypeIdentifier(String passTypeIdentifier) {
    this.passTypeIdentifier = passTypeIdentifier;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(String lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

}
