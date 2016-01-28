package com.adyen.examples.hpp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.google.common.io.BaseEncoding;

/**
 * Create an open invoice Payment (Klarna) On Hosted Payment Page (HPP)
 * 
 * The Adyen Hosted Payment Pages (HPPs) provide a flexible, secure and easy way to allow shoppers to pay for goods or
 * services. By submitting the form generated by this servlet to our HPP a payment will be created for the shopper.
 * 
 * @link /1.HPP/CreateOpenInvoicePaymentOnHpp
 * @author Created by Adyen - Payments Made Easy
 */

@WebServlet(urlPatterns = { "/1.HPP/CreateOpenInvoicePaymentSHA256" })
public class CreateOpenInvoicePayment_SHA_256 extends HttpServlet {

	private final static String HMAC_SHA256_ALGORITHM = "HmacSHA256";
	
	private final static Charset C_UTF8 = Charset.forName("UTF8");
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/**
		 * General HPP settings
		 * - hppUrl: URL of the Adyen HPP to submit the form to
		 * - hmacKey: It is the Shared Secret Key used to compute the merchant signature using the HMAC-SHA256 algorithm
		 * 
		 * Both variables are dependent on the environment which should be used (Test/Live).
		 * HMAC key can be retrieve: Adyen CA >> Skins >> Choose your Skin >> Edit Tab >> use the "Generate new HMAC Key" button to create the keys (the key bytes are "hex encoded")
		 */
		String hppUrl = "https://test.adyen.com/hpp/details.shtml";
		
		byte[] hmacKey =  BaseEncoding.base16().decode("yourHexEncodedHMAC");
	
	   

		/**
		 * Defining variables
		 * The HPP requires certain variables to be posted in order to create a payment possibility for the shopper.
		 * 
		 * The variables that you can post to the HPP are the following:
		 * 
		 * <pre>
		 * merchantReference    : Your reference for this payment.
		 * paymentAmount        : The transaction amount in minor units (e.g. EUR 1,00 = 100).
		 * currencyCode         : The three character ISO currency code.
		 * shipBeforeDate       : The date by which the goods or services specifed in the order must be shipped.
		 *                        Format: YYYY-MM-DD
		 * skinCode             : The code of the skin to be used for the payment.
		 * merchantAccount      : The merchant account for which you want to process the payment.
		 * sessionValidity      : The time by which a payment needs to have been made.
		 *                        Format: YYYY-MM-DDThh:mm:ssTZD
		 * shopperLocale        : A combination of language code and country code used to specify the language to be
		 *                        used in the payment session (e.g. en_GB).
		 * orderData            : A fragment of HTML/text that will be displayed on the HPP. (optional)
		 * countryCode          : Country code according to ISO_3166-1_alpha-2 standard. (optional)
		 * shopperEmail         : The shopper's email address. (recommended)
		 * shopperReference     : An ID that uniquely identifes the shopper, such as a customer id. (recommended)
		 * allowedMethods       : A comma-separated list of allowed payment methods, i.e. "ideal,mc,visa". (optional)
		 * blockedMethods       : A comma-separated list of blocked payment methods, i.e. "ideal,mc,visa". (optional)
		 * offset               : An integer that is added to the normal fraud score. (optional)
		 * merchantSig          : The HMAC signature used by Adyen to test the validy of the form.
		 * </pre>
		 */

		// Generate dates
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime(); // current date
		calendar.add(Calendar.DATE, 1);
		Date sessionDate = calendar.getTime(); // current date + 1 day
		calendar.add(Calendar.DATE, 2);
		Date shippingDate = calendar.getTime(); // current date + 3 days

		
		// Sort order is important (using natural ordering)

        SortedMap<String, String> params = new TreeMap<>();
        params.put("merchantAccount", "YourMerchantAccount");
        params.put("currencyCode", "EUR");
        params.put("paymentAmount", "10000000");
        params.put("sessionValidity", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(sessionDate));
        params.put("shipBeforeDate", new SimpleDateFormat("yyyy-MM-dd").format(shippingDate));
        params.put("shopperLocale", "en_GB"); 
        params.put("merchantReference", "TEST-PAYMENT-" + new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(currentDate));
        params.put("skinCode", "YourSkinCode");
        params.put("countryCode", "DE");
        params.put("shopperEmail", "YourShopperEmail");
        params.put("shopperReference", "TestShopperTest");
        params.put("allowedMethods", "");
        params.put("blockedMethods", "");
        params.put("offset", "");
        params.put("orderData", compressString("Orderdata to display on the HPP can be put here"));
        params.put("brandCode", "klarna");
        params.put("issuerId", "");
        
        
    	// billingAddress
 		params.put("billingAddress.city", "Neuss");
 		params.put("billingAddress.country","DE");
 		params.put("billingAddress.houseNumberOrName", "14");
 		params.put("billingAddress.postalCode", "41460");
 		params.put("billingAddress.stateOrProvince", "");
 		params.put("billingAddress.street", "Hellersbergstraße");
 		params.put("billingAddressType", "1");
     		
     	
 	    // deliveryAddress
        params.put("deliveryAddress.city", params.get("billingAddress.city"));
        params.put("deliveryAddress.country", params.get("billingAddress.country"));
        params.put("deliveryAddress.houseNumberOrName",  params.get("billingAddress.houseNumberOrName"));
        params.put("deliveryAddress.postalCode", params.get("billingAddress.postalCode"));
        params.put("deliveryAddress.stateOrProvince", params.get("billingAddress.stateOrProvince"));
        params.put("deliveryAddress.street", params.get("billingAddress.street"));
        params.put("deliveryAddressType", params.get("billingAddressType"));
            
    		
		// Shopper data
		params.put("shopper.firstName", "Testperson-de");
		params.put("shopper.infix", "");
		params.put("shopper.lastName", "Approved");
		params.put("shopper.gender", "MALE");
		params.put("shopper.dateOfBirthDayOfMonth", "07");
		params.put("shopper.dateOfBirthMonth", "07");
		params.put("shopper.dateOfBirthYear", "1960");
		params.put("shopper.telephoneNumber", "01522113356");
		params.put("shopperType", "1");
            
		// invoice lines
		params.put("openinvoicedata.numberOfLines", "3");
		params.put("openinvoicedata.refundDescription", params.get("merchantReference"));
		
		params.put("openinvoicedata.line1.currencyCode", "EUR");
		params.put("openinvoicedata.line1.description", "Apples");
		params.put("openinvoicedata.line1.itemAmount", "7860");
		params.put("openinvoicedata.line1.itemVatAmount", "1117");
		params.put("openinvoicedata.line1.itemVatPercentage", "1900");
		params.put("openinvoicedata.line1.numberOfItems","1");
		params.put("openinvoicedata.line1.vatCategory", "High");
		
		params.put("openinvoicedata.line2.currencyCode", "EUR");
		params.put("openinvoicedata.line2.description", "Pear");
		params.put("openinvoicedata.line2.itemAmount", "6754");
		params.put("openinvoicedata.line2.itemVatAmount", "1117");
		params.put("openinvoicedata.line2.itemVatPercentage", "1900");
		params.put("openinvoicedata.line2.numberOfItems","1");
		params.put("openinvoicedata.line2.vatCategory", "High");
		
		params.put("openinvoicedata.line3.currencyCode", "EUR");
		params.put("openinvoicedata.line3.description", "Pineapple");
		params.put("openinvoicedata.line3.itemAmount", "9876");
		params.put("openinvoicedata.line3.itemVatAmount", "1117");
		params.put("openinvoicedata.line3.itemVatPercentage", "1900");
		params.put("openinvoicedata.line3.numberOfItems","1");
		params.put("openinvoicedata.line3.vatCategory", "High");
       
        
		/**
		 * Signing the form
		 * 
		 * The merchant signature is used by Adyen to verify if the posted data is not altered by the shopper. The
		 * signature must be encrypted according to the procedure below.
		 */

	    // Calculate the data to sign
	        String signingData = Stream.concat(params.keySet().stream(), params.values().stream())
	                .map(signingTest -> escapeVal(signingTest))
	                .collect(Collectors.joining(":"));
        
     
        // Create the signature and add it to the parameter map
	        try {
	            params.put("merchantSig",calculateHMAC(signingData, hmacKey));
	        } catch (SignatureException e) {
	            e.printStackTrace();
	            return;
	        }
	     

		// Set request parameters for use on the JSP page
	    
	    params.forEach((keyName, keyValue) -> request.setAttribute(keyName, keyValue));
	    request.setAttribute("hppUrl", hppUrl);
	    
		// Set correct character encoding
		response.setCharacterEncoding("UTF-8");

		// Forward request data to corresponding JSP page
		request.getRequestDispatcher("/1.HPP/create-payment-on-hpp.jsp").forward(request, response);
	}

	/**
	 * Generates GZIP compressed and Base64 encoded string.
	 */
	private String compressString(String input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(output);

		gzip.write(input.getBytes("UTF-8"));
		gzip.close();
		output.close();

		return Base64.encodeBase64String(output.toByteArray());
	}

	// To escape embedded "\" characters as "\\", and embedded ":" as "\:".
	private static String escapeVal(String val) {
        if(val == null) { return ""; }
        return val.replace("\\", "\\\\").replace(":", "\\:");
    }
	
	// To calculate the HMAC SHA-256 
	private static String calculateHMAC(String data, byte[] key)  throws java.security.SignatureException {
        try {
        	
            // Create an hmac_sha256 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA256_ALGORITHM);
           
 
            // Get an hmac_sha256 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            
            mac.init(signingKey);
           
 
            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes(C_UTF8));
            
 
            // Base64-encode the hmac
            return  BaseEncoding.base64().encode(rawHmac);
 
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }
}