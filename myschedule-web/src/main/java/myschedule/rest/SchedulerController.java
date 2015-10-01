package myschedule.rest;

import myschedule.rest.exception.WebException;
import myschedule.rest.util.ResponseBuilder;
import myschedule.rest.util.SchedulerHelper;

import org.quartz.SchedulerException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;



@Controller
public class SchedulerController extends BaseController {


    
    @RequestMapping(value = "/scheduler/standby",  method = RequestMethod.GET)
     public @ResponseBody String standbyScheduler(@RequestParam("sid") String sid) throws SchedulerException {
        
    	try {
			schedulerExistsOrException(sid).standby();
		} catch (Exception e) {
			 return ResponseBuilder.badRequest("Not found scheduler "+sid);
		}
         return ResponseBuilder.success("/scheduler/standby", sid+" standby.");
    }

   
    @RequestMapping(value = "/scheduler/start",  method = RequestMethod.GET)
    public @ResponseBody String  startScheduler(@RequestParam("sid") String sid) throws SchedulerException {
      try{
    	schedulerExistsOrException(sid).start();
      } catch (Exception e) {
			 return ResponseBuilder.badRequest("Not found scheduler "+sid);
		}
    	return ResponseBuilder.success("/scheduler/start", sid+" started.");
    }

    @RequestMapping(value = "/scheduler/shutdown",  method = RequestMethod.GET)
     public @ResponseBody String shutdownScheduler(@RequestParam("sid") String sid) throws SchedulerException {
        try{
    	schedulerExistsOrException(sid).getQuartzScheduler().shutdown();
        } catch (Exception e) {
			 return ResponseBuilder.badRequest("Not found scheduler "+sid);
		}
    	return ResponseBuilder.success("/scheduler/shutdown", sid+" shutdown.");
    }

    /**
     * Set the scehduler as default. Would be used by JobSchedulingResource
     * @param sid
     * @return
     * @throws SchedulerException
     */
    private SchedulerHelper schedulerExistsOrException(String sid) {
        SchedulerHelper schedulerHelper = scheduler(sid);
        if(schedulerHelper == null)
			try {
				throw new WebException("Scheduler not exist");
			} catch (WebException e) {
				e.printStackTrace();
			}
        return schedulerHelper;
    }
}

