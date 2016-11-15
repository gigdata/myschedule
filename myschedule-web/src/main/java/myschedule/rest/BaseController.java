package myschedule.rest;

import java.util.ArrayList;
import java.util.List;

import myschedule.quartz.extra.SchedulerTemplate;
import myschedule.rest.util.SchedulerHelper;
import myschedule.web.MySchedule;

public class BaseController {
	
	protected SchedulerHelper scheduler() {
		return scheduler("default-scheduler");
	}
	
	protected SchedulerHelper scheduler(String sid) {
		MySchedule mysch = MySchedule.getInstance();
		if (sid == "default-scheduler") {	
			// get the default scheduler sid
			List<String> settingsNames = new ArrayList<String>();
			settingsNames = mysch.getSchedulerSettingsNames();
			sid = (settingsNames.get(0));
		}
    	SchedulerTemplate template = mysch.getScheduler(sid);
        return new SchedulerHelper(template.getScheduler());	
    }

}