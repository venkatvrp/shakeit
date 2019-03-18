package org.shakeit.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shakeit.model.Server;
import org.shakeit.model.Shakeit;
import org.shakeit.model.Url;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ShakeItEngine {

	static final ResourceBundle resourceBundle = ResourceBundle.getBundle("shakeit");	
	static final Logger skitlogger = LogManager.getLogger(ShakeItEngine.class);
	static final String MSIW = "msiw";
	static final String NSPORTAL = "nsportal";
	static final String TOTAL = "total";
	static final String PASS = "pass";
	static final String FAIL = "fail";
	static Map<String,Object> levelMap = null;

	public static void main(String[] args) {
		ShakeItEngine shakeItEng = new ShakeItEngine();
		levelMap = new HashMap<>();
		
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
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
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
		try {
			jaxbContext = JAXBContext.newInstance(Shakeit.class);
			Unmarshaller unmarshaller;
			unmarshaller = jaxbContext.createUnmarshaller();
			Shakeit shakeit = (Shakeit) unmarshaller.unmarshal(file);
			
			List<Url> nspUrlList = new ArrayList<>();
			List<Url> msiwUrlList = new ArrayList<>();
			
			for(Url url:shakeit.getApplications().getUrl()) {
				if(url.getApplication().equalsIgnoreCase(MSIW)) {
					msiwUrlList.add(url);
				}else if(url.getApplication().equalsIgnoreCase(NSPORTAL)) {
					nspUrlList.add(url);
				}
			}

			for(Server server:shakeit.getServers().getServer()) {
				if(server.getEnv().equalsIgnoreCase(envIp)) {
					if(server.getApplication().equalsIgnoreCase(NSPORTAL)) {
						accessServer(nspUrlList, server,appName);	
					}else if(server.getApplication().equalsIgnoreCase(MSIW)) {
						accessServer(msiwUrlList, server,appName);	
					}					
				}
			}
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
		for(Url appUrl:urlList) {
			if(appUrl.getApplication().equalsIgnoreCase(appName)) {
				for(Url serverUrl:server.getUrl()) {				
					if(serverUrl.getType().equalsIgnoreCase(appUrl.getType())) {
						try {
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
				skitlogger.warn("SSLHandshake warning");
				responseCode = 200;
			}catch(ConnectException ce) {
				skitlogger.error("Connection error");
				responseCode = 500;
			}catch(SocketTimeoutException se) {
				skitlogger.error("Socket Timeout error");
				responseCode = 500;
			}catch(Exception e) {
				skitlogger.error(e.getMessage());
				responseCode = 500;
			}
			if(responseCode!=200) {
				skitlogger.info("ERROR :: "+responseCode + " URL:: " +url);
				updateStatistics(-1,level);
			}else {
				skitlogger.info("OK :: " +url);
				updateStatistics(200,level);
			}
		}			
		return status;
	}
	
	/**
	 * Updates the statistics objects  with status
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
		if(levelMap!=null) {
			levelMap.forEach((k,v)->{
				Map<String,Integer> statMap = (Map<String, Integer>) v;
				int successRate = statMap.get(PASS) * 100 /statMap.get(TOTAL);
				skitlogger.info("+++++++++++++++++++++++ LEVEL "+k+" URLs ++++++++++++++++++++++++++");
				skitlogger.info("Total number of hits 			 	:: "+statMap.get(TOTAL));
				skitlogger.info("Number of Successful hits 			:: "+statMap.get(PASS));
				skitlogger.info("Number of Failure hits		    	:: "+statMap.get(FAIL));
				skitlogger.info("Success Rate				    	:: "+successRate+"%");
				if(successRate>=Integer.parseInt(resourceBundle.getString("url.level."+k+".criteria"))) {
					skitlogger.info("Shakedown Status :: SUCCESS");
				}else {
					skitlogger.info("Shakedown Status :: FAILURE");
				}
				skitlogger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			});
		}		
	}
	
	
}
