package myschedule.quartz.extra.job;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import myschedule.quartz.extra.util.HttpUtils;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class GLSSourceMonitorJob implements Job {
	
	private static final Logger logger = LoggerFactory.getLogger(GLSSourceMonitorJob.class);
	
	/**
	 * Contains difference between the current sources reported by GLS and those reported during the last execution
	 */
	private JSONObject sourceDifferences = new JSONObject();
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		// Get the required parameters from JobDataMap
	    JobDataMap dataMap = context.getJobDetail().getJobDataMap();

	    final String toAddress = dataMap.getString("toAddress");
	    final String jobApp = dataMap.getString("jobApp");
	    final String jobEnv = dataMap.getString("jobEnv");
	    final String emailServiceURL = dataMap.getString("emailServiceURL");
	    final String webAddress = dataMap.getString("webAddress");
	    final String webAddressUser = dataMap.getString("webAddressUser");
	    final String webAddressPass = dataMap.getString("webAddressPass");
	    
	    //List of sources persisted between different executions
	    List<String> persistedSources = new ArrayList<String>();
	    if(dataMap.get("sources") != null) {
	    	persistedSources = (List<String>) dataMap.get("sources");
	    }

		for(String g : persistedSources) {
			//logger.info(g);
		}
	    
	    String GLSPath = "";
	    if(dataMap.getString("jobType").equalsIgnoreCase("sources")) {
	    	//TODO: calculate the time since last execution
	    	long startTime;
	    	if(dataMap.get("lastExecutionTime") == null) {
	    		Integer startTimeInteger = (Integer) dataMap.get("startDateinSecs");
	    		startTime = startTimeInteger.longValue();
	    	} else {
	    		startTime = dataMap.getLong("lastExecutionTime");
	    	}
	    	long currentTime = System.currentTimeMillis() / 1000L;
	    	//logger.info(currentTime + " - " + startTime);
	    	if((currentTime - startTime) > 0) {
	    		GLSPath = "/sources?range=" + (currentTime - startTime);
	    	} else {
	    		//start at range=1 to get the input sources at time of job running, 
	    		//range=0 gets all inputs from any time the GLS was running
	    		GLSPath = "/sources?range=60";
	    	}
	    	//Store time of current execution to calculate time ranges of repeated executions
	    	dataMap.put("lastExecutionTime", currentTime); 
	    }
	    final String completeWebAddress = webAddress + GLSPath;
	    
	    try {
	    	String responseStr = "";
	    	
	    	//Construct GET request to GLS
	    	HttpRequestBase request = new HttpGet();
	    	try{
	    		request.setURI(new URI(completeWebAddress));
	    		byte[] message = (webAddressUser + ":" + webAddressPass) .getBytes("UTF-8");
				String encodedUserNamePassword = DatatypeConverter.printBase64Binary(message);
				request.addHeader("Authorization", "Basic " + encodedUserNamePassword);
				request.addHeader("Content-Type", "application/json");
				request.addHeader("Accept", "application/json");

	    	} catch (URISyntaxException e) {
	    		logger.warn("Something went wrong while preparing GLS request on scheduled monitor", e);
	    	}
	    	
	    	HttpClient client = HttpClientBuilder.create().build();
	    	HttpResponse httpResponse = client.execute(request);

	    	responseStr = HttpUtils.httpBodyToString(httpResponse.getEntity().getContent());
	    	//logger.info(responseStr);
	    	
            JSONObject sourceResponse = new JSONObject(responseStr);            
            JSONObject sourceResponseList = sourceResponse.getJSONObject("sources");
            //logger.info(sourceResponseList.toString());
            
            if (haveSourcesChanged(persistedSources, sourceResponseList, dataMap)) {
            	String body = "There has been a change in the log input sources for " + jobApp + "'s " + jobEnv + " environment!\n";
            	JSONArray added = sourceDifferences.getJSONArray("added");
            	JSONArray removed = sourceDifferences.getJSONArray("removed");

            	if(added.length() > 0) {
            		body += "ADDED:\n\n";
            		for(int i = 0; i < added.length(); i++) {
            			body += added.getString(i);
            			if(i < added.length() - 1) {
            				body += ", \n";
            			}
            		}
            	}
            	body +="\n\n";
            	
            	if(removed.length() > 0) {
            		body += "REMOVED:\n\n";
            		for(int i = 0; i < removed.length(); i++) {
            			body += removed.getString(i);
            			if(i < removed.length() - 1) {
            				body += ", \n";
            			}
            		}
            	}
            	body +="\n\n";
            	
                // request body for email service
            	JSONObject requestJson = new JSONObject();
            	requestJson.put("toAddress", toAddress);
            	requestJson.put("subject", " Alert: Log Sources Changed for " + jobApp + " - " + jobEnv);
            	requestJson.put("body", body);
            	String requestBody = requestJson.toString();

                //logger.info("requestbody " + requestBody);
                
                // send email service request
                responseStr = HttpUtils.httpRequestJob(requestBody, emailServiceURL);
                //logger.info("repsonsestr " + responseStr);
       
                // get email response message
                JSONObject emailResponse = new JSONObject(responseStr);
                boolean emailSuccess = emailResponse.getBoolean("success");
                if (emailSuccess) { // success
                	logger.info("Successfully scheduled job");
                } else { // failed for whatever reason
                	logger.error("Failed to schedule job");
                }   	
            } else {	// failed for whatever reason
            	logger.info("No email alert sent due to no change in input sources for " + jobApp + "'s " + jobEnv + " environment.");
            }
		} catch(Exception e) {
			logger.error("Failed to schedule job");
        	e.printStackTrace();
		}
		
	}
	
	/**
	 * Detects whether there are changes between the current list of sources in GLS and the list of sources from last execution
	 * 
	 * @param sources Sources persisted from last execution
	 * @param sourcesResponseList Sources from current execution
	 * @param dataMap JobDataMap that allows for the sources list persisted in this job to be updated 
	 * @return
	 * @throws JSONException
	 */
	private boolean haveSourcesChanged(List<String> sources, JSONObject sourcesResponseList, JobDataMap dataMap) throws JSONException {
		boolean hasListChanged = false;
		
		if(sourcesResponseList.length() < 0) {
			return false;
		} else {
			try {
				getDifferences(sources, sourcesResponseList);
			} catch (JSONException e) {
				logger.error("Problem while creating list of source differences", e);
			}
		}
		
		JSONArray added = sourceDifferences.getJSONArray("added");
		JSONArray removed = sourceDifferences.getJSONArray("removed");

		if(added.length() > 0 || removed.length() > 0) {
			hasListChanged = true;
			updateSourceList(sources, sourcesResponseList, dataMap);
		}
		
		return hasListChanged;
	}

	/**
	 * Updates the persisted source list with the current list of sources reported by GLS
	 * 
	 * @param sources
	 * @param sourcesResponseList
	 * @param dataMap JobDataMap where the list of updated sources will be stored
	 */
	private void updateSourceList(List<String> sources, JSONObject sourcesResponseList, JobDataMap dataMap) {
		Iterator<?> hostNames = sourcesResponseList.keys();
		sources.clear();
		while (hostNames.hasNext()){
			String hostName = (String) hostNames.next();
			sources.add(hostName);
		}

		dataMap.put("sources", sources);
	}
	
	/**
	 * Identifies specific differences in the persisted and current source lists
	 * 
	 * @param sources
	 * @param sourcesResponseList
	 * @throws JSONException
	 */
	private void getDifferences(List<String> sources, JSONObject sourcesResponseList) throws JSONException {
		Iterator<?> hostNames = sourcesResponseList.keys();
		sourceDifferences = new JSONObject();
		JSONArray added = new JSONArray();
		JSONArray removed = new JSONArray();

		//Check for added sources
		while (hostNames.hasNext()){
			boolean isHostPresent = false;
			String hostName = (String) hostNames.next();
			
			for(String source: sources) {
				if(hostName.equals(source)) {
					isHostPresent = true;
				}
			}
			
			if(!isHostPresent) {
				added.put(hostName);
			}
		}
				
		//Check for removed sources
		for(String source: sources) {
			boolean isHostPresent = false;
			hostNames = sourcesResponseList.keys();

			while(hostNames.hasNext()) {
				String hostName = (String) hostNames.next();
				if(source.equals(hostName)) {
					isHostPresent = true;
				}
			}
			
			if(!isHostPresent) {
				removed.put(source);
			}
		}
		
		sourceDifferences.put("added", added);
		sourceDifferences.put("removed", removed);
	}
}