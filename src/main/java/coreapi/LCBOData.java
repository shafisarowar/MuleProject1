package coreapi;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class LCBOData {
    
	/**
     * Returns a map containing the current price, regular price, 
     * discount (if there is one, otherwise null), and discount end date
     * (if there is one, otherwise null).
     * 
     * This data is grabbed from the product page on lcbo.com that corresponds
     * to the uniqueID passed into this function.
     * 
     * @param uniqueId The unique ID of the product to get information for
     * @return A hashmap containing price information, with the following keys:
     * currentPrice - The current price you can purchase the product for
     * regularPrice - The regular, non-sale price of the product
     * discount - The amount that is taken off, if there is a sale. (Is null if product is not on sale)
     * discountEndDate - The day the sale ends (Is null if product is not on sale)
     * 
     * The return value can also be null, if the product cannot be found, or if there is an error.
     */
    public Map<String, String> getPrices(String uniqueId) {
        
        try {
            String url = "http://www.lcbo.com/lcbo/product/sku/" + uniqueId;
            Document doc = Jsoup.connect(url).followRedirects(true).get();

            //25/02/2019 -- LCBO Change -
            // LCBO Changed the layout of the page, so we had to fix this.
            // Previously, there existed a single element on the page that contained
            // the regular price, current price, discount and the end date
            // but as of this date, they split these elements up so we have to go after them individually.
            // First change we made, was for current price, we had to go after .price
            // Then, for the other stuff, we had to go after the listPrice class, which contained
            // one line that had the old price and the discount.
            // Finally, to get the discount end date, we went after the limitedOffer class.
            
            Element price = doc.select(".price").first();
            Element discountPrices = doc.select(".listPrice").first();
            
            if (price == null){
                throw new Exception("Could not obtain price for product #" + uniqueId);
            }

            String[] lines = null;
            if (discountPrices != null){
                lines = discountPrices.wholeText().split("\n");   
            }
            
            //class: listPrice_save
            
            String currentPrice = getCurrentPrice(price);
            
            String discountEndDate = null;
            String regularPrice = currentPrice;
            String discount = null;

            if (lines != null){
                for (String line : lines) {
                    if (line.trim().startsWith("WAS")){
                         regularPrice = getPriceFromString(line.trim());
                    } 
                    else if (line.trim().startsWith("SAVE")) {
                         discount = getPriceFromString(line.trim());
                     }
                }   
            }
           
            // Only get the discountEndDate if the product is on sale
            
            if (discount != null || !regularPrice.equals(currentPrice)) {
                discountEndDate = getDiscountEndDate(doc);
            }                
            
//            System.out.println("currentPrice: " + currentPrice);
//            System.out.println("regularPrice: " + regularPrice);
//            System.out.println("discount: " + discount);
//            System.out.println("discountEndDate: " + discountEndDate);
            
            Map<String, String> returnMap = new HashMap<String, String>();
            returnMap.put("currentPrice", currentPrice);
            returnMap.put("regularPrice", regularPrice);
            returnMap.put("discount", discount);
            returnMap.put("discountEndDate", discountEndDate);

            return returnMap;

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the current price of a product, by extracting it from the "price-value" HTML element.
     * 
     * @param prodPrices The ".price" element on the product page
     * @returns A string containing the price of the product (eg. 27.95), or null if the price cannot be found.
     */
    private String getCurrentPrice(Element price) {
        try {
            String priceData = price.text().replaceAll("\\$", "");
            return priceData;
        } catch (Exception e) {
            System.out.println("getCurrentPrice() Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the discount end date of a product, from the "limitedOffer" element, 
     * 
     * @param doc The entire product page as a Document
     * @returns The discount end date in the format yyyy-MM-dd, or null if it doesn't exist.
     */
    private String getDiscountEndDate(Document doc) {
        try {
            String dateString = null;
            String discountLine = doc.getElementsByClass("limitedOffer").first().text();

            //25/02/2019 - LCBO Update - Changed this to go after limitedOffer Element
            //which contains : <span class="limitedOffer"><b>Limited Time Offer</b><br>Until March 2, 2019</span>
            //This element's text is only that one line 
            dateString = discountLine.replace("Limited Time Offer Until", "").trim();
            dateString = convertDate(dateString);
            return dateString;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Grabs the price out of a string.
     * Eg. WAS  $61.00 -> 61.00
     * 
     * @param str The string containing the price
     * @returns The price if it exists, otherwise null
     */
    private String getPriceFromString(String str) {
        Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)");
        Matcher matcher = regex.matcher(str);

        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Converts a date from "MMMM dd, yyyy" to "yyyy-mm-dd".
     * Eg. May 27, 2018 -> 2018-05-27
     * 
     * @param str The date string
     * @returns The re-formatted date string, or null if there is an error
     */
    private String convertDate(String str) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("MMMM dd, yyyy");
            Date date = df.parse(str);
            df.applyPattern("yyyy-MM-dd");
            return df.format(date);
        } catch(Exception e) {
            System.out.println("convertDate() Error converting " + str + ": " + e.getMessage());
            return null;
        }
    }
    
}
