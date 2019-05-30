##### What you will need:
1. test iPhone.
2. test Android tablet.
3. Knottypine associated with loyalty pass (currently set up for pass.clover.customer)
4. MySQL on machine hosting the service.


##### Service setup:
1. Make sure your machine is on the same network as test iPhone and test Android tablet.
2. Navigate to loyalty-pass-service.
3. In application.properties, change values to your machine's ip address and port used.
    1. To find your machine's IP address on a Mac, go to System Preferences > Network > Under Status, it is listed.
4. In burtPass.json, change 'webServiceURL' to match the above ip address and port.
4. Run 'gradle bootRun'.
5. In a Terminal window, run 'mysql -uroot'.


##### Clover Loyalty Example setup:
1. Navigate to remote-clover-loyalty.
    1. Run 'gradle build'.
    2. Connect to a knottypine.
    3. Run 'gradle uninstallDebug installDebug'.
2. Navigate to clover-loyalty-example-pos.
    1. Run 'gradle build'.
    2. Connect to your test Android tablet.
    3. Run 'gradle uninstallDebug installDebug'.
3. Launch clover-loyalty-example-pos on the Android tablet.
4. Launch Network Pay Display on knottypine.
5. Enter the ip address for the network connection to knottypine.
6. Click the 'VAS' check box.
    1. Enter the ip address and port number of where you started the service.
7. Click Connect.


### Using the Clover Loyalty Example:
##### Creating a Customer
1. Click 'Start Custom Activity'.
2. On the right side, enter a phone number.
3. Check that the service created the user in your local MySQL db.
    1. Run 'select * from customer;'. There should be an entry with the previously typed phone number.

##### Making a Sale and Receiving the Pass
1. Click 'Do Sale'.
2. Select tip amount.
3. Using the iPhone, hold the phone up to the knottypine.
4. Continue through the payment flow.
5. Check your iPhone. There should have been a notification about a 'Burt's Taco Palace' pass.
    1. Also check the Notifications Center on the iPhone by sliding down from the top of the screen.
6. Add pass to Apple Wallet.

>After taking multiple payments logged in as same Customer, you should be able to go into the pass and pull down to update the points.


### More info on creating Pass Type IDs for Apple Passes
1. Sign in to developer.apple.com/account.
2. Click 'Certificates, Identifiers, and Profiles'
3. On the left panel, under Identifiers, click 'Pass Type IDs'
4. To the right of 'iOS Pass Type IDs', click the '+' Sign
5. Follow Apple instructions.
6. Once the ID is created, return to Pass Type IDs page.
7. Click on your newly created pass type ID.
8. Click Edit.
9. Click Create Certificate.
10. Follow instructions to create a CSR.
11. Click Continue.
12. Upload just created CSR.
13. Download certificate.
14. In your Downloads folder, click on .cer file just downloaded.
15. Open Keychain Access.
16. You should see a certificate for your created pass type ID.

If you would like to export this certificate for signing, make sure you open up the certificate so that both 'Pass Type ID **' and private key are showing.  Highlight both make it says
"Export 2 items...". Save the new certificate as a .p12 file.

NOTE: You will need to be a part of the Clover Apple Developer Program