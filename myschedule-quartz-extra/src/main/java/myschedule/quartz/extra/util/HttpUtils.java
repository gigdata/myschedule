package myschedule.quartz.extra.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	
	/**
	 * POST request to reports email service
	 * @param requestBody - request body to send with the email request
	 * @return responseCode
	 */
	public static String httpRequestJob(String requestBody, String url) throws IOException {
		
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
		
		StringEntity entity = new StringEntity(requestBody);
		entity.setContentType("application/json");
		
		post.setEntity(entity);
		
		HttpResponse httpResponse = client.execute(post);
        
		String response = httpBodyToString(httpResponse.getEntity().getContent());
        logger.info(response);            
       
	    return response;
      
	}
	
	public static String httpBodyToString(InputStream is) throws IOException {
		
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        StringBuffer requestBody = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            requestBody.append(inputLine);
        }
        in.close();

		return requestBody.toString();
		
	}
}
