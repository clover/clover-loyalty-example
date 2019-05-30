package com.clover.loyalty.example.pos;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class CertificateUtils {
  public static KeyStore createTrustStore() {
    try {
      String storeType = KeyStore.getDefaultType();
      KeyStore trustStore = KeyStore.getInstance(storeType);
      char[] TRUST_STORE_PASSWORD = "clover".toCharArray();
      trustStore.load(null, TRUST_STORE_PASSWORD);

      // Load the old "dev" cert.  This should be valid for all target environments (dev, stg, sandbox, prod).
      Certificate cert = loadCertificateFromResource("/certs/device_ca_certificate.crt");
      trustStore.setCertificateEntry("dev", cert);

      // Now load the environment specific cert (e.g. prod).  Always retrieving this cert from prod as it is really
      // only valid in prod at this point, and we also don't have a mechanism within the SDK of specifying the target
      // environment.
      cert = loadCertificateFromResource("/certs/env_device_ca_certificate.crt");
      trustStore.setCertificateEntry("prod", cert);

      return trustStore;
    } catch(Throwable t) {
      t.printStackTrace();
    }
    return null;
  }

  private static Certificate loadCertificateFromResource(String name) {
    System.out.println("Loading cert:  " + name);

    InputStream is = null;
    try {
      is = CloverLoyaltyPOSActivity.class.getResourceAsStream(name);

      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertificate(is);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception ex) {
          // NO-OP
        }
      }
    }
  }
}
