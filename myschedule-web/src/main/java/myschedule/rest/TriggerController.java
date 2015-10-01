package myschedule.rest;

import myschedule.rest.exception.WebException;
import myschedule.rest.util.ResponseBuilder;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;



@Controller
public class TriggerController extends BaseController {

	/**
	 * @param group
	 * @param name
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/trigger/gettrigger",  method = RequestMethod.GET)
    public  @ResponseBody String getTrigger(@RequestParam("group") String group, @RequestParam("name") String name) throws SchedulerException {
       
		Trigger trigger;
		try {
			trigger = triggerExistsOrException(group, name);
			} catch (Exception e) {
				 return ResponseBuilder.badRequest("Not found trigger "+name);
			}
		Gson gson = new Gson();
		String json = gson.toJson(trigger);
		System.out.println(json);
		return json;
    }

    /**
     * @param group
     * @param name
     * @return
     * @throws SchedulerException
     */
    @RequestMapping(value = "/trigger/getjob",  method = RequestMethod.GET)
    public  @ResponseBody String getJob(@RequestParam("group") String group, @RequestParam("name") String name) throws SchedulerException {
    	Trigger trigger;
		try {
			trigger = triggerExistsOrException(group, name);
			} catch (Exception e) {
				 return ResponseBuilder.badRequest("Not found trigger "+name);
			}
		
		JobDetail jd = scheduler().getQuartzScheduler().getJobDetail(trigger.getJobKey());
		Gson gson = new Gson();
		String json = gson.toJson(jd);
		System.out.println(json);
		return json;
		
    }

    private Trigger triggerExistsOrException(String group, String name) throws SchedulerException {
        Trigger trigger = scheduler().getTrigger(group, name);
        if(trigger == null)
			try {
				throw new WebException();
			} catch (WebException e) {
				
			}
        return trigger;
    }
   
}

