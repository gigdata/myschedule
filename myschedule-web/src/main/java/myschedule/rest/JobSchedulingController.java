package myschedule.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;

import javax.ws.rs.DefaultValue;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import myschedule.quartz.extra.SchedulerTemplate;
import myschedule.rest.domain.JobSchedulingInfo;
import myschedule.rest.domain.RunTimeJobDetail;
import myschedule.rest.domain.ScheduleInfo;
import myschedule.rest.exception.BadCronExpressionException;
import myschedule.rest.exception.JobAlreadyExistsException;
import myschedule.rest.exception.WebException;
import myschedule.rest.util.JobDetailSerializer;
import myschedule.rest.util.JobHelper;
import myschedule.rest.util.ResponseBuilder;
import myschedule.rest.util.SchedulerHelper;
import myschedule.web.MySchedule;


@Controller
public class JobSchedulingController extends BaseController {
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public String listScheduler(ModelMap model) {
		MySchedule sch = MySchedule.getInstance();
		Gson gson = new Gson();
		return gson.toJson(sch.getSchedulerSettingsNames()); 
	}
	
	@RequestMapping(value = "/gettimezone", method = RequestMethod.GET)
	@ResponseBody
	public String getTimeZone() {
		return "{\"timezone\": \"" + TimeZone.getDefault().getID() + "\"}";
	}
	
	@RequestMapping(value = "/{sid}", method = RequestMethod.GET)
	@ResponseBody
	public String getScheduler(@PathVariable String sid, ModelMap model) {
		MySchedule sch = MySchedule.getInstance();
		Gson gson = new Gson();
		return gson.toJson(sch.getSchedulerSettings(sid));
 
	}

	@RequestMapping(value = "/{sid}/job", method = RequestMethod.GET)
	@ResponseBody
	public String getJobs(@PathVariable String sid) {
		MySchedule mysch = MySchedule.getInstance();
		Gson gson = new Gson();
		SchedulerTemplate schtemp = mysch.getScheduler(sid);
		List<JobDetail> list = schtemp.getAllJobDetails();
		
		return gson.toJson(list);
 
	}
	
	
	@RequestMapping(value = "/{id}/trigger", method = RequestMethod.GET)
	@ResponseBody
	public String getTriggers(@PathVariable String id) {
		MySchedule mysch = MySchedule.getInstance();
		Gson gson = new Gson();
		SchedulerTemplate schtemp = mysch.getScheduler(id);
		List<JobDetail> list = schtemp.getAllJobDetails();
		return gson.toJson(list);
 
	}
	
	/** SchedueJob service. 
	 * @param jobSchedulingInfo
	 * @return
	 * @throws ClassNotFoundException
	 * @throws JobAlreadyExistsException
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/schedulejob", method = RequestMethod.POST)
	public @ResponseBody  String scheduleJob(@RequestBody  JobSchedulingInfo jobSchedulingInfo) throws ClassNotFoundException, JobAlreadyExistsException, SchedulerException {
		
		JobSchedulingInfo jsi = new JobSchedulingInfo();
		jsi = jobSchedulingInfo;

		try {
			
			/**
			 * Need to create a serializer because Gson wouldn't serialize JobInfo correctly;
			 * The variable jobClass of type Class throws an unserializable exception 
			 * Serializer written in JobDetailSerializer
			 * @tfuntani
			 */
			GsonBuilder gsonBuilder = new GsonBuilder();
	        gsonBuilder.registerTypeAdapter(Class.class, new JobDetailSerializer());
	        Gson gson = gsonBuilder.create();
	        
			//Gson gson = new Gson();
        	RunTimeJobDetail jobdetails = scheduler().scheduleJob(JobHelper.buildJobDetail(jsi.getJobInfo()), 
        			JobHelper.buildTrigger(jsi.getScheduleInfo().getTriggerInfo(), JobHelper.buildSchedulerBuilder(jsi.getScheduleInfo())));
        	
        	return gson.toJson(jobdetails);
        
		} catch (BadCronExpressionException e) {
			try {
				throw new WebException();
			} catch (WebException e1) {
							e1.printStackTrace();
			}

          //  return ResponseBuilder.badRequest(e.getLocalizedMessage());
        }
		return "BAD_REQUEST: Failed to schedule a Job";
    }

	/**
	 * @param group
	 * @param name
	 * @return
	 * @throws SchedulerException
	 */
	// TODO: Redo this method, since jobExistsOrException accepts jobName and jobGroup as parameters not {sid} and {jid}
/*	@RequestMapping(value = "/{sid}/job/{jid}", method = RequestMethod.GET)
	public @ResponseBody String getJobInfo(@PathVariable("sid") String sid, @PathVariable("jid") String jid) throws SchedulerException {

		RunTimeJobDetail runTimeJobDetail = jobExistsOrException(sid, jid);
		Gson gson = new Gson();
		String json = gson.toJson(runTimeJobDetail);
		System.out.println(json);

		return json;
	}*/

	/**
	 * @param group
	 * @param name
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/deletejob",  method = RequestMethod.DELETE)
    public @ResponseBody String deleteJob(@RequestParam("group") String group, @RequestParam("name") String name) throws SchedulerException {
        jobExistsOrException(group, name);
        scheduler().removeJob(new JobKey(name, group));
        return ResponseBuilder.resourceDeleted("Job", "{Group:" + group + ", name:" + name + "}");
    }

	/**
	 * @param group
	 * @param name
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/getjobtriggers", method = RequestMethod.GET)
    public @ResponseBody String getJobTriggers(@RequestParam("group") String group, @RequestParam("name") String name) throws SchedulerException {
      
		List<? extends Trigger> triggers  = jobExistsOrException(group, name).getTriggers();
		Gson gson = new Gson();
		String json = gson.toJson(triggers);
		System.out.println(json);
		
		return json;
    }

	/**
	 * @param group
	 * @param name
	 * @param scheduleInfo
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/addjobtrigger", method = RequestMethod.POST)

	public @ResponseBody RunTimeJobDetail addJobTrigger(@RequestParam("group") String group, @RequestParam("name") String name, ScheduleInfo scheduleInfo) throws SchedulerException {
        RunTimeJobDetail runTimeJobDetail = jobExistsOrException(group, name);
        try {
            scheduler().getQuartzScheduler().scheduleJob(JobHelper.buildTriggerForJob(scheduleInfo.getTriggerInfo(),
                    JobHelper.buildSchedulerBuilder(scheduleInfo), runTimeJobDetail.getJobDetail()));
            runTimeJobDetail = jobExistsOrException(group, name);
        } catch (Exception e) {
        	try {
				throw new WebException();
			} catch (WebException e1) {
							e1.printStackTrace();
			} 
        }
		return runTimeJobDetail; 
    }

	/**
	 * @param jobGroup
	 * @param jobName
	 * @param triggerGroup
	 * @param triggerName
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/getjobtrigger" ,  method = RequestMethod.GET)
    public @ResponseBody Trigger getJobTrigger(@RequestParam("jobGroup") String jobGroup,
                                          @RequestParam("jobName") String jobName,
                                          @RequestParam("triggerGroup") String triggerGroup,
                                          @RequestParam("triggerName") String triggerName) throws SchedulerException {
        return jobExistsOrException(jobGroup, jobName).
                getTrigger(triggerGroup, triggerName);
    }

	/**
	 * @param jobGroup
	 * @param jobName
	 * @param triggerGroup
	 * @param triggerName
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/unschedulejobtrigger",  method = RequestMethod.GET)
	   public @ResponseBody String unScheduleJobTrigger(@RequestParam("jobGroup") String jobGroup,
                                    @RequestParam("jobName") String jobName,
                                    @RequestParam("triggerGroup") String triggerGroup,
                                    @RequestParam("triggerName") String triggerName) throws SchedulerException {
        Trigger trigger = jobExistsOrException(jobGroup, jobName).
                getTrigger(triggerGroup, triggerName);

        if(trigger != null)
            scheduler().getQuartzScheduler().unscheduleJob(trigger.getKey());
        return ResponseBuilder.response("OK", "unschedulejobtrigger successful");
    }

    /**
     *
     * @param groupExp Regular Expression for Job Group
     * @param nameExp  Regular Expression for Job Name
     * @return
     * @throws SchedulerException
     */
	/**
	 * @param groupExp
	 * @param nameExp
	 * @return
	 * @throws SchedulerException
	 */
	@RequestMapping(value = "/jobs/searchjobs",  method = RequestMethod.GET)	
	public @ResponseBody List<String> searchJobs(@RequestParam("group") @DefaultValue(".+") String groupExp,
                                             @RequestParam("name") @DefaultValue(".+") String nameExp) throws SchedulerException {
        
		List<String> response = new ArrayList<String>();
		try{
			GsonBuilder gsonBuilder = new GsonBuilder();
	        gsonBuilder.registerTypeAdapter(Class.class, new JobDetailSerializer());
	        Gson gson = gsonBuilder.create();
	        
			List<RunTimeJobDetail> list = scheduler().searchJobs(groupExp, nameExp);
			for (RunTimeJobDetail job : list){
				String json = gson.toJson(job);
				response.add(json);
				
			}
			System.out.println(response);
        }
        catch(PatternSyntaxException e) {
            try {
				throw new WebException("Unable to find fobs.");
			} catch (WebException e1) {
				e1.printStackTrace();
			}
        }
		return response;
    }

	/** Delete all jobs service.
	 * @return
	 * @throws SchedulerException
	 */
	
	@RequestMapping(value = "/jobs/deletealljobs", method = RequestMethod.DELETE)	
    public @ResponseBody String deleteAllJobs() throws SchedulerException {
        scheduler().removeAllJobs();
        return ResponseBuilder.response("OK", "[ All jobs are deleted successfully]");
    }

    private RunTimeJobDetail jobExistsOrException(String jobGroup, String jobName) throws SchedulerException {
     
    	// TODO: This assumes all schedules are saved to the default scheduler
    	// Need a way to pass {sid} of job and use it to create instance of schedulerHelper
    	SchedulerHelper schedulerHelper = scheduler();
    	RunTimeJobDetail runTimeJobDetail = schedulerHelper.getJobDetails(jobGroup, jobName);
        if(runTimeJobDetail == null){
        	try {
				throw new WebException("Unable to find fobs.");
			} catch (WebException e1) {
				e1.printStackTrace();
			}
        	}
        return runTimeJobDetail;
    }
}

