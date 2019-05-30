package com.clover.loyalty.example.pos;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoyaltyServiceHelper {

  private final static String localhost = "http://" + CloverLoyaltyPOSActivity.serverIp + ":" + CloverLoyaltyPOSActivity.serverPort + "/v1";

  public static String queryService(String endpoint) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(localhost + endpoint);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);

      DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
      dataOutputStream.close();



      if (connection.getResponseCode() == 200) {
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        StringBuffer response = new StringBuffer();
        String line;
        while ((line = rd.readLine()) != null) {
          response.append(line);
          response.append('\r');
        }
        rd.close();
        return response.toString();
      }

    } catch (Exception e) {
      if (connection != null) {
        connection.disconnect();
      }
      return null;
    }
    return null;
  }
}
