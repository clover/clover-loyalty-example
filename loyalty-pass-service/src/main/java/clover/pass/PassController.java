package clover.pass;

import clover.db.Customer;
import clover.db.CustomerRepository;
import clover.db.Pass;
import clover.db.Device;
import clover.db.DeviceRepository;
import clover.db.PassRepository;
import clover.db.Registration;
import clover.db.RegistrationsRepository;
import com.google.gson.Gson;
import de.brendamour.jpasskit.PKBarcode;
import de.brendamour.jpasskit.PKField;
import de.brendamour.jpasskit.PKLocation;
import de.brendamour.jpasskit.PKNFC;
import de.brendamour.jpasskit.PKPass;
import de.brendamour.jpasskit.enums.PKBarcodeFormat;
import de.brendamour.jpasskit.passes.PKStoreCard;
import de.brendamour.jpasskit.signing.PKFileBasedSigningUtil;
import de.brendamour.jpasskit.signing.PKPassTemplateFolder;
import de.brendamour.jpasskit.signing.PKSigningInformation;
import de.brendamour.jpasskit.signing.PKSigningInformationUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/v1")
public class PassController {
  private final String passTypeIdentifier = "pass.clover.customer";
  private final String privateKeyPath = "src/main/resources/passes/applepass.p12";
  private final String privateKeyPassword = "clover123";
  private final String appleWWDRCA = "src/main/resources/passes/AppleWWDRCA.pem";
  Customer lastCustomer;
  @Autowired
  private PassRepository passRepository;

  @Autowired
  private DeviceRepository deviceRepository;

  @Autowired
  private RegistrationsRepository registrationsRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  ServerProperties serverProperties;

  public String localhost;

  Gson gson = new Gson();

  @RequestMapping("/getPassUrl")
  public PassResponse passResponse() throws Exception {
    System.out.println("pass url hit");
    localhost = "http:/" + serverProperties.getAddress() + ":" + serverProperties.getPort();
    File file = new File("src/main/resources/passes/burtPass.json");
    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    InputStream inputStream =  resource.getInputStream();
    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      response.append(line);
    }
    rd.close();
    Pass pass = createPass(gson.fromJson(response.toString(), Map.class));
    createPkPass(pass);
    System.out.println("Localhost: " + localhost);
    UrlObject urlObject = new UrlObject(localhost+ "/v1/download");
    return new PassResponse(urlObject);

  }

  @RequestMapping(value = "/pass/update", method = RequestMethod.POST)
  public HttpStatus updatePass(@RequestBody Map<String, Object> payload, @RequestHeader("Content-Type") String contentType) {
    if (contentType.equals("application/json") && !payload.isEmpty()) {
      Pass pass = createPass(payload);
      passRepository.save(pass);
      createPkPass(pass);
      System.out.println("Pass updated!");
      return HttpStatus.OK;
    } else {
      System.out.println("Pass updated not accepted");
      return HttpStatus.NOT_ACCEPTABLE;
    }
  }

  @RequestMapping("/download")
  public ResponseEntity download() throws FileNotFoundException  {
    File file = new File("src/main/resources/passes/mypass.pkpass");
    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    System.out.println("Pass downloaded!");
    return ResponseEntity.ok().contentLength(file.length()).contentType(MediaType.parseMediaType("application/vnd.apple.pkpass")).body(resource);
  }



  @RequestMapping(value = "/passes/"+passTypeIdentifier+"/{serialNumber}", method = RequestMethod.GET)
  public ResponseEntity getLatestVersionOfPass(@PathVariable String serialNumber, @RequestHeader("Authorization") String authorization, @Nullable @RequestHeader("If-Modified-Since") String modifiedDate) {
    //authorization value will be 'ApplePass', followed by a space, followed by the pass’s authorization token as specified in the pass.
    //Support standard HTTP caching on this endpoint: Check for the If-Modified-Since header, and return HTTP status code 304 if the pass has not changed.
    if (authorization.startsWith("ApplePass")) {
      Optional<Pass> passQuery = passRepository.findById(serialNumber);
      if (passQuery.isPresent()) {
        System.out.println("Pass found by serial number: " + serialNumber);
        Pass pass = passQuery.get();
        Long lastUpdate = Long.valueOf(pass.getLastUpdate());
        //Apple is supposed to send a If-Modified-Since header with a value but I have not seen one
        //so we will always update otherwise the pass would never update
        modifiedDate = modifiedDate != null ? modifiedDate : "1";
        if (lastUpdate != null && modifiedDate != null && Long.valueOf(modifiedDate) < lastUpdate) {
          try {
            createPkPass(pass);
            lookupPass(serialNumber);
            return download();
          } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
          }
        } else {
          return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
      }
    }
    return ResponseEntity.badRequest().build();

  }

  private void lookupPass(String serial) {
    if (lastCustomer != null &&  lastCustomer.getPassSerialNumber() != null) {
      Optional<Pass> optionalPass = passRepository.findById(serial);
      if (optionalPass.isPresent()) {
        Pass pass = optionalPass.get();
        lastCustomer.setPassSerialNumber(serial);
        customerRepository.save(lastCustomer);
      }
    }
  }

  @RequestMapping(value = "/devices/{deviceLibraryIdentifier}/registrations/"+passTypeIdentifier+"/{serialNumber}", method = RequestMethod.POST)
  public HttpStatus registerDeviceForNotifications(@PathVariable String deviceLibraryIdentifier, @PathVariable String serialNumber, @RequestBody Map<String, Object> payload, @RequestHeader("Authorization") String authorization) {
    /*
    The POST payload is a JSON dictionary containing a single key and value:

    pushToken
    The push token that the server can use to send push notifications to this device.

    RESPONSE:
    If the serial number is already registered for this device, returns HTTP status 200.
    If registration succeeds, returns HTTP status 201.
    If the request is not authorized, returns HTTP status 401.
    Otherwise, returns the appropriate standard HTTP status.
     */
    if (authorization.startsWith("ApplePass")) {
      Optional<Device> optionalDevice = deviceRepository.findById(deviceLibraryIdentifier);
      //look up device and set pushToken if found.
      if (optionalDevice.isPresent()) {
        Device device = optionalDevice.get();
        device.setPushToken(payload.containsKey("pushToken") ? payload.get("pushToken").toString() : null);
        deviceRepository.save(device);
        System.out.println("Push token saved for device");
      } else {
        //create new device entry
        Device device = new Device();
        device.setPushToken(payload.containsKey("pushToken") ? payload.get("pushToken").toString() : null);
        device.setDeviceLibraryIdentifier(deviceLibraryIdentifier);
        deviceRepository.save(device);
        System.out.println("Push token saved for device");
      }
      if (lastCustomer != null) {
        lastCustomer.setDeviceIdentifier(deviceLibraryIdentifier);
        lastCustomer.setDevicePushToken(payload.get("pushToken").toString());
        lastCustomer.setPassSerialNumber(serialNumber);
        customerRepository.save(lastCustomer);
        System.out.println("Push token and serial number saved for customer");
      }
      //Register device and pass
      Registration registration = new Registration();
      registration.setNotifications(true);
      registration.setDeviceId(deviceLibraryIdentifier);
      registration.setPassTypeIdentifer(passTypeIdentifier);
      registration.setPassSerialNumber(serialNumber);
      registrationsRepository.save(registration);
      System.out.println("Registration completed");
      return HttpStatus.CREATED;

    } else {
      return HttpStatus.UNAUTHORIZED;
    }
  }

  @RequestMapping(value = "/devices/{deviceLibraryIdentifier}/registrations/"+passTypeIdentifier)
  public ResponseEntity getSerialNumbersForPassesAssociatedWithDevice(@PathVariable String deviceLibraryIdentifier, @RequestParam(value = "passesUpdatedSince", defaultValue = "0") String tag) {
    /*
    If there are matching passes, returns HTTP status 200 with a JSON dictionary with the following keys and values:
    lastUpdated (string)
    The current modification tag.

    serialNumbers (array of strings)
    The serial numbers of the matching passes.

    If there are no matching passes, returns HTTP status 204.

    Otherwise, returns the appropriate standard HTTP status.
     */

    Iterable<Registration> registrations = registrationsRepository.findAll();
    Iterator iterator = registrations.iterator();

    List<String> registrationList = new ArrayList();
    while (iterator.hasNext()) {
      Registration registration = (Registration) iterator.next();
      if (registration.getDeviceId().equals(deviceLibraryIdentifier)) {
        registrationList.add(registration.getPassSerialNumber());
      }
    }
    if (registrationList.size() > 0) {
      Map<String, Object> passes = new HashMap<>();
      passes.put("lastUpdated", Long.toString(new Date().getTime()));
      passes.put("serialNumbers", registrationList);
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(passes);
    } else {
      return ResponseEntity.noContent().build();
    }
  }

  @RequestMapping(value = "/devices/{deviceLibraryIdentifier}/registrations/"+passTypeIdentifier+"/{serialNumber}", method = RequestMethod.DELETE)
  public HttpStatus unregisterDevice(@PathVariable String deviceLibraryIdentifier, @PathVariable String serialNumber, @RequestHeader("Authorization") String authorization) {
    //authorization value will be 'ApplePass', followed by a space, followed by the clover.pass’s authorization token as specified in the clover.pass.
    if (authorization.startsWith("ApplePass")) {
      Optional<Registration> optionalRegistration = registrationsRepository.findById(serialNumber);
      if (optionalRegistration.isPresent()) {
        Registration registration = optionalRegistration.get();
        registrationsRepository.delete(registration);
        System.out.println("Device unregistered");
        return HttpStatus.OK;
      } else {
        return HttpStatus.BAD_REQUEST;
      }
    } else {
      return HttpStatus.UNAUTHORIZED;
    }
  }

  @RequestMapping(value = "/customer/update/{id}/{points}")
  public ResponseEntity updateUser(@PathVariable int id, @PathVariable String points) {
    Optional<Customer> customerOptional = customerRepository.findById(id);
    if (customerOptional.isPresent()) {
      Customer customer = customerOptional.get();
      customer.setPoints(customer.getPoints() + Integer.valueOf(points));
      updatePass(String.valueOf(customer.getPoints()));
      customerRepository.save(customer);
      System.out.println("Customer (" + id + ") updated with " + points + " points");
      return ResponseEntity.ok().body(gson.toJson(customer));
    }
    return ResponseEntity.badRequest().build();
  }

  private void updatePass(String points) {
    Map values = new HashMap();
    values.put("key", "points");
    values.put("label", "free taco points");
    values.put("value", points);
    List primaryList = new ArrayList();
    primaryList.add(values);
    Map map = new HashMap();
    map.put("primaryFields", primaryList);
    Map storecard = new HashMap();
    storecard.put("storeCard", map);
    Pass pass =  createPass(storecard);
    createPkPass(pass);
    passRepository.save(pass);
    System.out.println("Pass updated with points");
  }

  @RequestMapping(value = "/customer/query/phone/{phoneNumber}")
  public ResponseEntity queryPhoneNumber(@PathVariable String phoneNumber) {
    Iterable<Customer> customerIterable = customerRepository.findAll();
    Iterator<Customer> customers = customerIterable.iterator();
    Customer customer;
    while (customers.hasNext()) {
      customer = customers.next();
      if (customer.getPhone().equals(phoneNumber)) {
        lastCustomer = customer;
        System.out.println("Existing custmoer found by #: " + phoneNumber);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson(customer));
      }
    }
    //Didn't find the customer, so create one
    customer = new Customer();
    customer.setPhone(phoneNumber);
    customer.setName("Unknown User");
    lastCustomer = customer;
    customerRepository.save(customer);
    System.out.println("New customer created for #: " + phoneNumber);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson(customer));
  }

  @RequestMapping(value = "/customer/query/vas/{vasData}")
  public ResponseEntity queryVasData(@PathVariable String vasData) {
    Iterable<Pass> passIterable = passRepository.findAll();
    Iterator<Pass> passIterator = passIterable.iterator();
    while (passIterator.hasNext()) {
      Pass pass = passIterator.next();
      if (pass.getNfc() != null) {
        PKNFC pknfc = gson.fromJson(pass.getNfc(), PKNFC.class);
        String message = pknfc.getMessage();
        if (message.equals(vasData)) {
          Iterable<Customer> customerIterable = customerRepository.findAll();
          Iterator<Customer> customerIterator = customerIterable.iterator();
          while (customerIterator.hasNext()) {
            Customer customer = customerIterator.next();
            if (pass.getSerialNumber().equals(customer.getPassSerialNumber())) {
              System.out.println("Customer found for vas data");
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson(customer));
            }
          }
        }
      }
    }
    Customer customer = new Customer();
    Map<String, Object> map = new HashMap<>();
    map.put("serialNumber", RandomStringUtils.random(12));
    Pass pass = createPass(map);
    createPkPass(pass);
    customer.setPassSerialNumber(pass.getSerialNumber());
    customer.setName("Unknown User");
    passRepository.save(pass);
    customerRepository.save(customer);
    System.out.println("Customer created for vas data");
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson(customer));
  }

  @RequestMapping(value = "/customer/clear/{id}")
  public HttpStatus clearCustomer(@PathVariable String id) {
    if (lastCustomer != null && lastCustomer.getId() == Integer.valueOf(id)) {
      lastCustomer = null;
    }
    System.out.println("Customer cleared");
    return HttpStatus.OK;
  }

  @RequestMapping(value = "customer/query/id/{id}")
  public ResponseEntity queryUserById(@PathVariable int id) {
    Optional<Customer> customerOptional = customerRepository.findById(id);
    if (customerOptional.isPresent()) {
      Customer customer = customerOptional.get();
      System.out.println("Customer found by id: " + id);
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson(customer));
    }
    System.out.println("Customer not found by id: " + id);
    return ResponseEntity.badRequest().build();
  }

  private Pass createPass(Map<String, Object> payload) {

    Pass pass = new Pass();
    pass.setFormatVersion(1);
    if (!payload.isEmpty()) {
      pass.setPassTypeIdentifier(payload.containsKey("passTypeIdentifier") ? payload.get("passTypeIdentifier").toString() : "pass.clover.customer");
      pass.setSerialNumber(payload.containsKey("serialNumber") ? payload.get("serialNumber").toString() : "123456789");
      pass.setTeamIdentifier(payload.containsKey("teamIdentifier") ? payload.get("teamIdentifier").toString() : "Q993233M8T");
      pass.setWebServiceURL(localhost);
      pass.setAuthenticationToken(payload.containsKey("authenticationToken") ? payload.get("authenticationToken").toString(): "vxwxd7J8AlNNFPS8k0a0FfUFtq0ewzFdc");
      pass.setOrganizationName(payload.containsKey("organizationName") ? payload.get("organizationName").toString() : "Burt's Taco Palace");
      pass.setDescription(payload.containsKey("description") ? payload.get("description").toString() : "Burt's Taco Palace");
      pass.setLogoText(payload.containsKey("logoText") ? payload.get("logoText").toString() : "Burt's Taco Palace");
      pass.setForegroundColor(payload.containsKey("foregroundColor") ? payload.get("foregroundColor").toString() : "rgb(255, 255, 255)");
      pass.setBackgroundColor(payload.containsKey("backgroundColor") ? payload.get("backgroundColor").toString() : "rgb(55, 117, 50)");
    } else {
      pass.setPassTypeIdentifier("pass.clover.customer");
      pass.setSerialNumber("123456789");
      pass.setTeamIdentifier("Q993233M8T");
      pass.setWebServiceURL(localhost);
      pass.setAuthenticationToken("vxwxd7J8AlNNFPS8k0a0FfUFtq0ewzFdc");
      pass.setOrganizationName("Burt's Taco Palace");
      pass.setDescription("Burt's Taco Palace");
      pass.setLogoText("Burt's Taco Palace");
      pass.setForegroundColor("rgb(255, 255, 255)");
      pass.setBackgroundColor("rgb(55, 117, 50)");
    }
    pass.setLastUpdate(String.valueOf(new Date().getTime()));

    //location
    PKLocation location = new PKLocation();
    if (!payload.isEmpty() && payload.containsKey("locations")) {
      ArrayList locationList = (ArrayList) payload.get("locations");
      Map locationMap = (Map) locationList.get(0);
      location.setLongitude(locationMap.containsKey("longitude") ? (double) locationMap.get("longitude") : -1);
      location.setLatitude(locationMap.containsKey("latitude") ? (double) locationMap.get("latitude") : -1);
      List locationList1 = new ArrayList();
      locationList1.add(location);
      pass.setLocations(gson.toJson(locationList1));
    } else {
      location.setLongitude(-1);
      location.setLatitude(-1);
      List locationList1 = new ArrayList();
      locationList1.add(location);
      pass.setLocations(gson.toJson(locationList1));
    }

    //create barcode
    PKBarcode barcode = new PKBarcode();
    if (!payload.isEmpty() && payload.containsKey("barcode")) {
      Map barcodeMap = (Map) payload.get("barcode");
      barcode.setMessage(barcodeMap.containsKey("message") ? barcodeMap.get("message").toString() : "123456789");
      barcode.setMessageEncoding(barcodeMap.containsKey("messageEncoding") ? Charset.forName(barcodeMap.get("messageEncoding").toString()) : Charset.forName("iso-8859-1"));
      barcode.setFormat(barcodeMap.containsKey("format") ? PKBarcodeFormat.valueOf(barcodeMap.get("format").toString()) :PKBarcodeFormat.PKBarcodeFormatPDF417);
      pass.setBarcode(gson.toJson(barcode));
    } else {
      //set defaults
      barcode.setMessage("123456789");
      barcode.setMessageEncoding(Charset.forName("iso-8859-1"));
      barcode.setFormat(PKBarcodeFormat.PKBarcodeFormatPDF417);
      pass.setBarcode(gson.toJson(barcode));
    }




    //storeCard
    if (!payload.isEmpty() && payload.containsKey("storeCard")) {
      PKStoreCard storeCard = new PKStoreCard();
      Map cardMap = (Map) payload.get("storeCard");
      if (cardMap.containsKey("primaryFields")) {
        List<PKField> primaryFields = new ArrayList<>();
        PKField primaryField = new PKField();
        ArrayList primaryList = (ArrayList) cardMap.get("primaryFields");
        Map primaryMap = (Map) primaryList.get(0);
        primaryField.setKey(primaryMap.containsKey("key") ? primaryMap.get("key").toString() : "points");
        primaryField.setValue(primaryMap.get("value"));
        primaryField.setLabel(primaryMap.containsKey("label") ? primaryMap.get("label").toString() :"free taco points");
        primaryFields.add(primaryField);
        storeCard.setPrimaryFields(primaryFields);
      }

      if (cardMap.containsKey("auxiliaryFields")) {
        List<PKField> auxFields = new ArrayList<>();
        PKField auxField = new PKField();
        ArrayList auxList = (ArrayList) cardMap.get("auxiliaryFields");
        Map auxMap = (Map) auxList.get(0);
        auxField.setKey(auxMap.containsKey("key") ? auxMap.get("key").toString() : "deal");
        auxField.setLabel(auxMap.containsKey("label") ? auxMap.get("label").toString() : "Deal of the Day");
        auxField.setValue(auxMap.containsKey("value") ? auxMap.get("value").toString() : "Tacos");
        auxFields.add(auxField);
        storeCard.setAuxiliaryFields(auxFields);
      } else {
        List<PKField> auxFields = new ArrayList<>();
        PKField auxField = new PKField();
        auxField.setKey( "deal");
        auxField.setLabel("Deal of the Day");
        auxField.setValue("Tacos");
        auxFields.add(auxField);
        storeCard.setAuxiliaryFields(auxFields);
      }
      pass.setStoreCard(gson.toJson(storeCard));
    } else {
      //set defaults
      PKStoreCard storeCard = new PKStoreCard();
      List<PKField> primaryFields = new ArrayList<>();
      PKField primaryField = new PKField();

      primaryField.setKey( "points");
      primaryField.setValue(0);
      primaryField.setLabel("free taco points");
      primaryFields.add(primaryField);
      storeCard.setPrimaryFields(primaryFields);

      List<PKField> auxFields = new ArrayList<>();
      PKField auxField = new PKField();
      auxField.setKey("deal");
      auxField.setLabel("Deal of the Day");
      auxField.setValue("Tacos");
      auxFields.add(auxField);
      storeCard.setAuxiliaryFields(auxFields);
      pass.setStoreCard(gson.toJson(storeCard));
    }


    //nfc
    PKNFC pknfc = new PKNFC();
    if (!payload.isEmpty() && payload.containsKey("nfc")) {
      Map nfcMap = (Map) payload.get("nfc");
      pknfc.setMessage(nfcMap.get("message").toString());
      pknfc.setEncryptionPublicKey(nfcMap.get("encryptionPublicKey").toString());
      pass.setNfc(gson.toJson(pknfc));
    } else {
      //set defaults
      pknfc.setMessage("44XBAPX69H");
      pknfc.setEncryptionPublicKey("MDkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDIgAD5dZCSNJkbSSUTTW/OOc0I+i07ucXt0bxcyodzrFwvrA=");
      pass.setNfc(gson.toJson(pknfc));
    }
    System.out.println("Pass created");
    return pass;

  }

  private void createPkPass(Pass pass) {
    try {
      PKSigningInformationUtil pkSigningInformationUtil = new PKSigningInformationUtil();
      PKSigningInformation pkSigningInformation = pkSigningInformationUtil.loadSigningInformationFromPKCS12AndIntermediateCertificate(
          privateKeyPath, privateKeyPassword, appleWWDRCA
      );
      PKPass pkPass = new PKPass();
      pkPass.setFormatVersion(pass.getFormatVersion());
      pkPass.setPassTypeIdentifier(pass.getPassTypeIdentifier());
      pkPass.setSerialNumber(pass.getSerialNumber());
      pkPass.setTeamIdentifier(pass.getTeamIdentifier());
      pkPass.setWebServiceURL(new URL(localhost));
      pkPass.setAuthenticationToken(pass.getAuthenticationToken());
      pkPass.setBackgroundColor(pass.getBackgroundColor());
      pkPass.setForegroundColor(pass.getForegroundColor());

      //location
      if (pass.getLocations() != null) {
        List locationList = gson.fromJson(pass.getLocations(), List.class);
        Map locationMap = (Map) locationList.get(0);
        PKLocation location = new PKLocation();
        location.setLatitude((double) locationMap.get("latitude"));
        location.setLongitude((double) locationMap.get("longitude"));
        List locations = new ArrayList();
        locations.add(location);
        pkPass.setLocations(locations);
      }

      //barcode
      if (pass.getBarcode() != null) {
        PKBarcode barcode = gson.fromJson(pass.getBarcode(), PKBarcode.class);
        pkPass.setBarcode(barcode);
      }

      pkPass.setOrganizationName(pass.getOrganizationName());
      pkPass.setDescription(pass.getDescription());
      pkPass.setLogoText(pass.getLogoText());

      //storeCard
      if (pass.getStoreCard() != null) {
        PKStoreCard storeCard = gson.fromJson(pass.getStoreCard(), PKStoreCard.class);
        pkPass.setStoreCard(storeCard);
      }

      //nfc
      if (pass.getNfc() != null) {
        PKNFC pknfc = gson.fromJson(pass.getNfc(), PKNFC.class);
        pkPass.setNFC(pknfc);
      }

      if (pkPass.isValid()) {
        String pathToTemplate = "src/main/resources/passes/Burts.raw";
        PKPassTemplateFolder passTemplateFolder = new PKPassTemplateFolder(pathToTemplate);
        PKFileBasedSigningUtil pkFileBasedSigningUtil = new PKFileBasedSigningUtil();
        byte[] passZipAsByteArray = pkFileBasedSigningUtil.createSignedAndZippedPkPassArchive(pkPass, passTemplateFolder, pkSigningInformation);
        String outputFile = "src/main/resources/passes/mypass.pkpass";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(passZipAsByteArray);
        IOUtils.copy(inputStream, new FileOutputStream(outputFile));
        System.out.println("PKPass created");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
