package myschedule.rest;

import myschedule.quartz.extra.SchedulerTemplate;
import myschedule.rest.util.SchedulerHelper;
import myschedule.web.MySchedule;



public class BaseController {
	
	protected SchedulerHelper scheduler() {
		return scheduler("default-scheduler");
	}
	
	protected SchedulerHelper scheduler(String sid) {
		MySchedule mysch = MySchedule.getInstance();
    	SchedulerTemplate template = mysch.getScheduler(sid);
        return new SchedulerHelper(template.getScheduler());	}

}