package myschedule.quartz.extra.job;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import myschedule.quartz.extra.util.HttpUtils;

/**
 * Job for detecting metric anomalies
 * Queries Elasticsearch for the metric values and sends alert 
 * if the calculated delta percent for a given duration exceeds the configured margin percentage
 * 
 *
 */
public class MetricsAnomalyDetectorJob implements Job {
	
	private static final Logger logger = LoggerFactory.getLogger(MetricsAnomalyDetectorJob.class);
	
	private Map<String, String> METRICS;
	
	private Map<String, Double> metricsDeltaMap = new HashMap<String, Double>();
	private static Map<String, String> currentMetricsMap = new HashMap<String, String>();
	private static Map<String, String> lastCheckedMetricsMap = new HashMap<String, String>();

	private static final String ELASTICSEARCH_DATESTAMP_FORMAT = "yy-MM-dd HH:mm:ss.SSS";
	private static String currentCheckDate = null;
	private static String previousCheckDate = null;
		
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
				
		// Get the required parameters from JobDataMap
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();	
		final double marginPercentage = Double.parseDouble(dataMap.getString("marginPercentage"));
		final String appName = dataMap.getString("appName");
		final String envName = dataMap.getString("envName");
		final String elasticsearchHost = dataMap.getString("elasticsearchHost");
		final String elasticsearchIndexPrefix = dataMap.getString("elasticsearchIndexPrefix");
		final String gl2_source_input = dataMap.getString("gl2_source_input");
		final String toAddress = dataMap.getString("toAddress");
		final String emailServiceURL = dataMap.getString("emailServiceURL");
		
		try {
			
			logger.info("Detecting metric anomalies for - App: " + appName + " | Env: " + envName);
			
			fillMetricsMap(context.getTrigger().getKey().getName());
			
			// get previous check date and current check date and format the date objects to Elasticsearch format
			SimpleDateFormat dateParser = new SimpleDateFormat(ELASTICSEARCH_DATESTAMP_FORMAT);
			previousCheckDate = dateParser.format(getFromTimestamp(context.getFireTime(), context.getNextFireTime()));
			currentCheckDate = dateParser.format(context.getFireTime());
			
			logger.info("Duration: " + previousCheckDate + " to " + currentCheckDate);
			
			// ES search
			JestClientFactory factory = new JestClientFactory();
			factory.setHttpClientConfig(new HttpClientConfig.Builder("http://" + elasticsearchHost).multiThreaded(true).readTimeout(60000).build());
			JestClient jclient = factory.getObject();
			
			String query = queryGenerator(gl2_source_input, appName, envName, previousCheckDate, currentCheckDate);
			Search search = new Search.Builder(query).addIndex(elasticsearchIndexPrefix).build();
			SearchResult result = jclient.execute(search);
			
			JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
			
			// need at least two hits
			if (hits.size() <= 1) {
				
				logger.info("ES query with duration: " + previousCheckDate + " to " + currentCheckDate + " did not return at least two results.");
				
				String to = dateParser.format(extendTimestamp(context.getFireTime()));
				
				logger.info("Redoing search with next closest duration period. Duration: " + previousCheckDate + " to " + to);
				
				query = queryGenerator(gl2_source_input, appName, envName, previousCheckDate, to);
				search = new Search.Builder(query).addIndex(elasticsearchIndexPrefix).build();
				result = jclient.execute(search);
				
				hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
				
			}
			
			if (hits.size() > 1) {
				
				putMetricsToMaps(hits.get(0), hits.get(hits.size() - 1)); // first hit, last hit	
				
				// ES time/datestamp will not match exactly with user configured duration
				// we're taking closest available timestamps for calculating anomalies
				logger.info("Actual duration (based on timestamp returned by ES): " + 
						hits.get(hits.size() - 1).getAsJsonObject().getAsJsonObject("_source").get("DATESTAMP").getAsString() + " to " +
						hits.get(0).getAsJsonObject().getAsJsonObject("_source").get("DATESTAMP").getAsString());
				
				// check if any of the delta values exceeds the margin percentage; send an alert if so
				for (double value : metricsDeltaMap.values()) {
				    if (Math.abs(value) > Math.abs(marginPercentage)) {
				    	sendAlert(toAddress, emailServiceURL, marginPercentage, appName, envName);
				    	break;
				    }
				}
			} else {
				logger.info("Unable to check for metrics anomalies. Not enough hits returned from ES query.");
			}
			
		} catch (IOException e) {
			logger.error("Exception while retrieving results from Elasticsearch.", e);
		}
	
	}
	
	/**
	 * Add/remove other metrics from metrics maps, depending on the recurrence and relevance of the metric
	 * @param recurrence
	 */
	private void fillMetricsMap(String recurrence) {
		
		METRICS = new HashMap<String, String>();
		METRICS.put("totalEvents", "Graylog Total Events");
		METRICS.put("totalDocuments", "Elasticsearch Total Documents");
		METRICS.put("totalQueries", "Elasticsearch Total Queries");
		METRICS.put("avgQueryResponseTime", "Average Query Response Time");
		METRICS.put("envSize", "Environment Size");
		
		if (recurrence.contains("hourly")) {
			METRICS.put("totalEventsPerHour", "Total Graylog Events Per Hour");
		} else if (recurrence.contains("daily")) {
			METRICS.put("totalEventsToday", "Total Graylog Events Daily");
			METRICS.put("totalDocumentsToday", "Total Elasticsearch Documents Daily");
		}
		
	}
	
	/**
	 * Calculate the difference in hours between the fire time and the next fire time - this will give us the duration
	 * By subtracting the duration from the fire time, we can get the previous fire time,
	 * which is essentially the date value of "from" in the timestamp range field for the ES query
	 * @param fireTime
	 * @param nextFireTime
	 * @return date value of "from" for timestamp field in ES query
	 */
	private Date getFromTimestamp(Date fireTime, Date nextFireTime) {
		
		Calendar c1 = GregorianCalendar.getInstance();
		c1.setTime(fireTime);				
		Calendar c2 = GregorianCalendar.getInstance();
		c2.setTime(nextFireTime);
		
		long diffMillis = c2.getTimeInMillis() - c1.getTimeInMillis();
		int diffHours = (int) Math.ceil(((double) diffMillis / (double) (1000 * 60 * 60)));
		logger.info("Hours between each check: " + diffHours);

		c1.add(Calendar.HOUR, 0 - diffHours);
		
		return c1.getTime();
		
	}
	
	/**
	 * Add one hour to date to extend timestamp range when original job trigger duration 
	 * doesn't cover enough timestamp for ES to return at least two results
	 * @param date
	 * @return date + 1 hour
	 */
	private Date extendTimestamp(Date date) {
		
		Calendar c = GregorianCalendar.getInstance();
		c.setTime(date);				
		c.add(Calendar.HOUR, 1);
		
		return c.getTime();
		
	}
	
	/**
	 * Put metrics from first hit to current metrics map and metrics from last hit to last checked metrics map
	 * Compute delta for each metric and put into metrics delta map
	 * The hits are ordered by timestamp (descending) so first hit is the most recent while the last hit was the previous most recent
	 * @param firstHit
	 * @param lastHit
	 */
	private void putMetricsToMaps(JsonElement firstHit, JsonElement lastHit) {

		JsonObject source1 = firstHit.getAsJsonObject().getAsJsonObject("_source");
		JsonObject source2 = lastHit.getAsJsonObject().getAsJsonObject("_source");
		
		for (Map.Entry<String, String> entry : METRICS.entrySet()) {
			String metric = entry.getKey();
			currentMetricsMap.put(metric, source1.get(metric).getAsString());
			lastCheckedMetricsMap.put(metric, source2.get(metric).getAsString());			
			metricsDeltaMap.put(metric, computeDelta(source1.get(metric).getAsString(), source2.get(metric).getAsString()));	
		}
		
	}
	
	private double computeDelta(String val1, String val2) {
		
		return ((Double.parseDouble(val1) - Double.parseDouble(val2)) / Double.parseDouble(val2) * 100.00);
		
	}

	private void sendAlert(String toAddress, String emailServiceURL, double marginPercentage, String appName, String envName) {

		String subject = " Metrics Anomaly Alert: " + appName + "-" + envName;
		String body = emailBodyTemplate(marginPercentage);

        String requestBody = "{\"toAddress\":\"" + toAddress + "\", \"subject\":\"" + subject + "\", \"body\":\"" + body + "\"}";

		try {
			
			String responseStr = HttpUtils.httpRequestJob(requestBody, emailServiceURL);	        
	        JSONObject emailResponse = new JSONObject(responseStr);
	        boolean emailSuccess = emailResponse.getBoolean("success");
	        if (emailSuccess) {
	        	logger.info("Successfully emailed alert.");
	        } else {
	        	logger.error("Failed to email alert. Response from email API: " + emailResponse);
	        } 
	        
		} catch (IOException e) {
			logger.error("Exception while trying to retrieve response to email service request.", e);
		} catch (JSONException e) {
			logger.error("Exception while trying to convert response to JSON object.", e);
		}

	}
	
	/**
	 * Create HTML body for email alert
	 * @param marginPercentage
	 * @return HTML body for email alert
	 */
	private String emailBodyTemplate(double marginPercentage) {
		
		// reformat date strings for better readability
		String previousCheckDateFormatted = previousCheckDate;
		String currentCheckDateFormatted = currentCheckDate;
		try {
			
			final String NEW_FORMAT = "MM/dd/yyyy, hh:mm:ss.SSS a";
			SimpleDateFormat sdf1 = new SimpleDateFormat(ELASTICSEARCH_DATESTAMP_FORMAT);
			SimpleDateFormat sdf2 = new SimpleDateFormat(ELASTICSEARCH_DATESTAMP_FORMAT);
			
			Date d1 = sdf1.parse(previousCheckDate);
			sdf1.applyPattern(NEW_FORMAT);
			previousCheckDateFormatted = sdf1.format(d1);
			
			Date d2 = sdf2.parse(currentCheckDate);
			sdf2.applyPattern(NEW_FORMAT);
			currentCheckDateFormatted = sdf1.format(d2);
			
		} catch (ParseException e) {
			logger.error("Exception while reformatting date strings.", e);
		}
		
		String body = "";
		
		for (Map.Entry<String, Double> entry : metricsDeltaMap.entrySet()) {
			
			double calculatedDelta = entry.getValue();
			
			body += "<p style='font-family:Consolas, Calibri; font-size:12px;'>";
			body += "<strong><u>" + METRICS.get(entry.getKey()) + ":</u></strong><br/>";
			
			String oldValue = lastCheckedMetricsMap.get(entry.getKey());
			String newValue = currentMetricsMap.get(entry.getKey());
			
			// convert bytes values to readable strings with units
			if (entry.getKey().equals("envSize")) {
				oldValue = humanReadableBytes(Long.parseLong(oldValue));
				newValue = humanReadableBytes(Long.parseLong(newValue));
			} else if (entry.getKey().equals("avgQueryResponseTime")) {
				oldValue = NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(oldValue)) + " ms";
				newValue = NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(newValue)) + " ms";
			} else {
				oldValue = NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(oldValue));
				newValue = NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(newValue));
			}
			
			body += oldValue + " >>> " + newValue + "<br/>";

			String color = "#000000";
			if (!Double.isNaN(calculatedDelta) && (calculatedDelta < (0.0 - Math.abs(marginPercentage)))) {
				color = "#ff0000"; // red
			} else if (!Double.isNaN(calculatedDelta) && (calculatedDelta > marginPercentage)) {
				color = "#006400"; // green
			}
			
			if (color.equals("#000000") && !Double.isNaN(calculatedDelta)) {
				body += "<span>&Delta;: <strong>"
						+ "<span>" + String.format("%.3f", Math.abs(calculatedDelta)) + "%</span></strong></span>";
			} else if (Double.isNaN(calculatedDelta)) {
				body += "<span style='background-color:#ffff00;'>&Delta;: <strong>"
						+ "<span style='color: " + color + ";'>--- (base value is 0) </span></strong></span>";
			} else {
				body += "<span style='background-color:#ffff00;'>&Delta;: <strong>"
						+ "<span style='color: " + color + ";'>" + String.format("%.3f", calculatedDelta) + "%</span></strong></span>";
			}
			
			body += "</p>";
		}
		
		String template = "<p style='font-family:Consolas, Calibri; font-size:12px;'>One or more of the metrics were flagged. Please check.<br/>"
				+ "Your configured margin percentage is <strong>" + String.format("%.2f", marginPercentage) + "%</strong>.<br/>"
				+ "Duration: <u>" + previousCheckDateFormatted + "</u> to <u>" + currentCheckDateFormatted + "</u></p>"
				+ body;
		
		return template;
		
	}
	
	/**
	 * Generate Elasticsearch query
	 * @param appName
	 * @param envName
	 * @param from
	 * @param to
	 * @return ES query string
	 */
	private String queryGenerator(String gl2_source_input, String appName, String envName, String from, String to) {
		
		return "{" +
			"	\"query\": {" +
			"    	\"bool\": {" +
			"			\"must\": [" +
			"				{" +
			"					\"term\": {" +
			"						\"message.gl2_source_input\": \"" + gl2_source_input + "\"" +
			"					}" +
			"        		}," +
			"				{" +
			"					\"term\": {" +
			"						\"message.appName\": \"" + appName + "\"" +
			"					}" +
			"        		}," +
			"				{" +
			"					\"term\": {" +
			"						\"message.envName\": \"" + envName + "\"" +
			"					}" +
			"        		}," +
			"        		{" +
			"         			 \"range\": {" +
			"            			\"DATESTAMP\": {" +
			"              				\"gte\": \"" + from + "\"," +
			"              				\"lte\": \"" + to + "\"" +
			"            			}" +
			"          			}" +
			"        		}" +
			"      		]," +
			"      		\"must_not\": []," +
			"      		\"should\": []" +
			"    	}" +
			"  	}," +
			"  	\"from\": 0," +
			"  	\"size\": 100," +
			"  	\"sort\": [" + 
			"		{" + 
			"			\"timestamp\": {" +
			"				\"order\": \"desc\"" +
			"			}" +
			" 		}" +
			"	]," +
			"  	\"facets\": {}" +
			"}";
		
	}
	
	/**
	 * Convert bytes value to a human readable string
	 * @param bytes
	 * @return converted bytes value to a string with the proper unit
	 */
	private String humanReadableBytes(long bytes) {
		
		long base = 1024L;
		if (bytes < base) { // KB
			return bytes + " KB";
		}
		
		int exponent = (int) Math.round((Math.log(bytes) / Math.log(base)));
		if (exponent < 2) {
			exponent = 2;
		}

		String unitList = "MGTPE";
		String units = unitList.charAt(exponent - 2) + "B";
		
		return String.format("%.3f %s", (double) bytes/Math.pow(base, exponent), units);
		
	}

}
