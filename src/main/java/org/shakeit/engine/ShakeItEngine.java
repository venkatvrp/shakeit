package org.shakeit.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.shakeit.model.Server;
import org.shakeit.model.Shakeit;
import org.shakeit.model.Url;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ShakeItEngine {

	static ResourceBundle resourceBundle = null;
	static final Logger skitlogger = LogManager.getLogger(ShakeItEngine.class);
	static final String TOTAL = "total";
	static final String PASS = "pass";
	static final String FAIL = "fail";
	static final String ALL = "_all";
	static final String CURRENT = "_current";

	static Map<String,Object> levelMap = null;
	static Map<String,Integer> progressMap = null;
	static Set<String> hostSet = null;
	static Map<String,Set<String>> errSetMap = null; 
	static Map<String,List<Url>> appListMap = null;
	static Set<String> errUrlSet = null;
	
	int progressInt = 0;

	public static void main(String[] args) {
		
		try {
			File file = new File(System.getProperty("user.dir")+"/config");
			URL[] urls= {file.toURI().toURL()};		
			ClassLoader loader = new URLClassLoader(urls);
			resourceBundle = ResourceBundle.getBundle("shakeit",Locale.getDefault(), loader);	
		}catch(Exception e){
			skitlogger.error("Error occurred while loading the properties file.");
		}
		
		ShakeItEngine shakeItEng = new ShakeItEngine();
		levelMap = new HashMap<>();
		progressMap = new HashMap<>(); 
		hostSet = new HashSet<>();
		errSetMap = new HashMap<>(); 
		appListMap = new HashMap<>();
		errUrlSet = new HashSet<>();
		
		// To create objects for the configured error and applications
		getResourceBundleValues("error.").forEach((rsrcKey,rsrcVal)->errSetMap.put(rsrcKey,new HashSet<>()));
		getResourceBundleValues("app.").forEach((rsrcKey,rsrcVal)->appListMap.put(rsrcVal,new ArrayList<Url>()));
		
		if (args != null && args[0].length() > 0) {
			String envIp = args[0];
			String appName = args[1];
			String xmlUrlStr = "file:///"+System.getProperty("user.dir")+"/config/"+resourceBundle.getString("shakedown.xml.path");
			skitlogger.info("Validating XML..");
			Document doc = shakeItEng.validateXML(xmlUrlStr);
			if (doc != null) {
				skitlogger.info("XML is valid.");
				if(!envIp.isEmpty()) {
					shakeItEng.extractValuesfromXML(envIp,appName);
				}else {
					skitlogger.error("Please specify shakedown environment (DEV/STG/PROD)");
				}
				
			} else {
				skitlogger.error("XML validation failed. Please check the xml.");
			}
		}else {
			skitlogger.error("Please specify the target environment/argument empty");
		}	
		skitlogger.info("Shakedown Completed !!");
		shakeItEng.printStatistics();		
	}

	/**
	 * Create HTTP Connection
	 * @param url
	 * @return integer
	 * @throws IOException
	 */
	private static int getURLResponseCode(String url) throws IOException {
		if (url != null && !url.isEmpty()) {
			URL urlObj = new URL(url);
			hostSet.add(urlObj.getHost());
			
			org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
			
			// Parses the DOM for the specified error and updates the object with URL
			errSetMap.forEach((resrcKey,setObj)->{
				if(!doc.getElementsContainingText(resourceBundle.getString(resrcKey)).isEmpty()) {
					setObj.add(url);
				}
			});						
		 
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(6000);
			return con.getResponseCode();
		} else {
			return 404;
		}
		
	}

	/**
	 * Parses the XML and checks for the validity
	 * 
	 * @param rssFeedUrl
	 * @return boolean
	 * @throws IOException
	 */
	private Document validateXML(String rssFeedUrl) {
		
		Document doc = null;
		try(Reader reader = new InputStreamReader(new URL(rssFeedUrl).openStream(), "UTF-8");) {			
			InputSource xmlSource = new InputSource(reader);
			xmlSource.setEncoding("UTF-8");
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.parse(xmlSource);
		}catch (ParserConfigurationException | SAXException | IOException e) {
			skitlogger.error("Error occurred while validating XML: " + e.getMessage());		
		}
		return doc;
	}

	/**
	 * Extracts the values from XML
	 * @param envIp
	 * @return boolean
	 */
	private boolean extractValuesfromXML(String envIp,String appName) {
		File file = new File(System.getProperty("user.dir")+"/config/"+resourceBundle.getString("shakedown.xml.path"));
		skitlogger.info("Shakedown being performed on "+envIp.toUpperCase()+ " environment");
		JAXBContext jaxbContext = null;
		Map<String,Integer> countMap = new HashMap<>();
		
		// Initializes counter and progress objects
		appListMap.forEach((aName,count)->{
			countMap.put(aName,0);
			progressMap.put(aName+CURRENT,0);
		});
		
		
		try {
			jaxbContext = JAXBContext.newInstance(Shakeit.class);
			Unmarshaller unmarshaller;
			unmarshaller = jaxbContext.createUnmarshaller();
			Shakeit shakeit = (Shakeit) unmarshaller.unmarshal(file);
			
			// Stores URL specific to applications in the respective object in map
			shakeit.getApplications().getUrl().forEach(url->appListMap.get(url.getApplication()).add(url));
			
			// Counts the number of Server for the specified application 
			shakeit.getServers().getServer().forEach(server->{
				if(server.getEnv().equalsIgnoreCase(envIp)) {					
					countMap.put(server.getApplication(), countMap.get(server.getApplication())+1);										
				}
			});	
			
			// Multiply the URLs with number of server to get exact total URLs
			countMap.forEach((aName,count)->progressMap.put(aName+ALL, appListMap.get(aName).size()*count));		

			// Access server specific to the environment
			shakeit.getServers().getServer().forEach(server->{
				if(server.getEnv().equalsIgnoreCase(envIp)) {
					try {
						accessServer(appListMap.get(server.getApplication()), server,appName);
					} catch (IOException e) {
						skitlogger.error("IO Exception occurred" +e.getMessage());
					}					
				}
			});				
			
		} catch (JAXBException e) {
			skitlogger.error("Error while unmarshalling "+e.getMessage());
		}catch(Exception e) {
			skitlogger.error("Error occurred "+e.getMessage());
		}
		
		return true;
	}
	
	/**
	 * Trigger the list of URLs on the specified server
	 * @param urlList
	 * @param server
	 * @throws IOException
	 */
	private void accessServer(List<Url> urlList,Server server,String appName) throws IOException {
		
		for(Url appUrl:urlList){
			if(appUrl.getApplication().equalsIgnoreCase(appName)) {
				
				// Trigger progress only every alternate URL hit to avoid duplicate percentage display
				if(progressMap.get(appName+CURRENT)%3==1) {
					// To calculate and display progress
					showProgress(appName);
				}
				
				for(Url serverUrl:server.getUrl()) {				
					if(serverUrl.getType().equalsIgnoreCase(appUrl.getType())) {
						try {
							// Updates the current URL hit count in the Map
							progressMap.put(appName+CURRENT, progressInt++);
							connectToURL(serverUrl.getValue()+appUrl.getValue(),appUrl.getLevel());	
							
						}catch(SSLHandshakeException se) {
							skitlogger.warn("SSLHandshake warning");
						}
						
					}					
				}				
			}
		}
	}
	
	/**
	 * Makes HTTP connection to the image URL to check for the validity
	 * @param element
	 * @param entry
	 * @return CountStat
	 * @throws IOException
	 */
	private static int connectToURL(String url,String level) throws IOException{
		int status = 0;	
		int responseCode = 0;
		if(url!=null && !url.isEmpty()) {	
			try {
				updateStatistics(0,level);
				responseCode = getURLResponseCode(url);				
			}catch(SSLHandshakeException se) {
				skitlogger.debug("SSLHandshake warning");
				responseCode = 200;
			}catch(ConnectException ce) {
				skitlogger.debug("Connection error");
				responseCode = 500;
			}catch(SocketTimeoutException se) {
				skitlogger.debug("Socket Timeout error");
				responseCode = 500;
			}catch(Exception e) {
				skitlogger.debug(e.getMessage());
				responseCode = 500;				
			}
			if(responseCode!=200) {
				skitlogger.debug("ERROR :: "+responseCode + " URL:: " +url);
				updateStatistics(-1,level);
				errUrlSet.add(url);
			}else {
				skitlogger.debug("OK :: " +url);				
				updateStatistics(200,level);
			}
		}			
		return status;
	}
	
	/**
	 * Updates the statistics objects with different HTTP status
	 * @param status
	 * @param level
	 */
	@SuppressWarnings("unchecked")
	private static void updateStatistics(int status,String level) {	
		Map<String,Integer> statMap = (Map<String, Integer>)levelMap.get(level);
		if(statMap==null) {
			statMap = new HashMap<>();			
			statMap.put(TOTAL,0);
			statMap.put(PASS,0);
			statMap.put(FAIL,0);
		}		
	
		if(status==0)statMap.put(TOTAL, statMap.get(TOTAL)+1);
		if(status==200)statMap.put(PASS, statMap.get(PASS)+1);
		if(status==-1)statMap.put(FAIL, statMap.get(FAIL)+1);	
			
	
		levelMap.put(level,statMap);
	}
	
	/**
	 * Prints/Logs the statistics
	 */
	@SuppressWarnings("unchecked")
	private void printStatistics() {
		
		errSetMap.forEach((resrcKey,setObj)->{
			if(setObj!=null && !setObj.isEmpty()) {			
				skitlogger.info("+++++++++++++++++++++++ "+resrcKey.toUpperCase()+" URLS +++++++++++++++++++++++++++");
				setObj.forEach(tmpDsbleErr->skitlogger.info(tmpDsbleErr));		
			}
		});	
				
		if(errUrlSet!=null && !errUrlSet.isEmpty()) {			
			skitlogger.info("+++++++++++++++++++++++ HTTP ERROR URLs +++++++++++++++++++++++++++");
			errUrlSet.forEach(urlErr->skitlogger.info(urlErr));		
		}
		if(hostSet!=null && !hostSet.isEmpty()) {			
			skitlogger.info("+++++++++++++++++++++++ SERVERS SCANNED +++++++++++++++++++++++++++");
			hostSet.forEach(hostname->skitlogger.info(hostname));		
		}
		if(levelMap!=null && !levelMap.isEmpty()) {
			levelMap.forEach((k,v)->{
				Map<String,Integer> statMap = (Map<String, Integer>) v;
				int successRate = statMap.get(PASS) * 100 /statMap.get(TOTAL);
				skitlogger.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				skitlogger.info("Level:: "+k+" > Total:: "+statMap.get(TOTAL) +" > Hits:: "+statMap.get(PASS)+" > Miss:: "+statMap.get(FAIL)+" > Rate:: "+successRate+"% " );
				// Success rate differs for every level base on the configuration
				if(successRate>=Integer.parseInt(resourceBundle.getString("level.url."+k+".criteria"))) {
					skitlogger.info("Shakedown Status :: SUCCESS");
				}else {
					skitlogger.info("Shakedown Status :: FAILURE");
				}
			});
		}		
	}
	
	/**
	 * Shows progress based on the URL hits
	 * @param appName
	 */
	private void showProgress(String appName){		
		switch (progressMap.get(appName+CURRENT)*100/progressMap.get(appName+ALL)) {
			case 10:
	        	skitlogger.info("10% Completed");
	            break;    
			case 20:
	        	skitlogger.info("20% Completed");
	            break;
	        case 40:
	        	skitlogger.info("40% Completed");
	            break;
	        case 60:
	        	skitlogger.info("60% Completed");
	            break;
	        case 80:
	        	skitlogger.info("80% Completed");
	            break;
	        case 95:
	        	skitlogger.info("95% Completed");
	        	skitlogger.info("Collecting Statistics...");
	            break;
	        default:	
	        	skitlogger.debug("progressMap "+progressMap);
	            break;
	    }			
	}
	
	/**
	 * Searches the key in the resource bundle and returns the value
	 * @param searchKeyStr
	 * @return String
	 */
	private static Map<String,String> getResourceBundleValues(String searchKeyStr) {
		Map<String,String> rsrcKeyMap = new HashMap<>();
		Enumeration<String> rsrceKeyEnum = resourceBundle.getKeys();		
		while (rsrceKeyEnum.hasMoreElements()) {
		    String resrceKey = rsrceKeyEnum.nextElement();
		    if(resrceKey.indexOf(searchKeyStr)>-1) {
		    	rsrcKeyMap.put(resrceKey, resourceBundle.getString(resrceKey));
		    }
		}
		return rsrcKeyMap;
	}
}
