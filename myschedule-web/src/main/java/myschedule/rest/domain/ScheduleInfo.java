package myschedule.rest.domain;

/**
 * 
 * @author gigdata@semaifour.com
 *
 */
public class ScheduleInfo {
  /*  public ScheduleInfo(SimpleSchedulerInfo simpleScheduleInfo,
			TriggerInfo triggerInfo) {
		super();
		this.simpleScheduleInfo = simpleScheduleInfo;
		this.triggerInfo = triggerInfo;
	}
    public ScheduleInfo(){}*/
	private SimpleSchedulerInfo simpleScheduleInfo;
    private CronSchedulerInfo cronScheduleInfo;
    private TriggerInfo triggerInfo;
    private boolean simpleSchedule = true;

    public SimpleSchedulerInfo getSimpleScheduleInfo() {
        return simpleScheduleInfo;
    }

    public ScheduleInfo setSimpleScheduleInfo(SimpleSchedulerInfo simpleScheduleInfo) {
        this.simpleScheduleInfo = simpleScheduleInfo;
        return this;
    }
    public boolean isSimpleSchedule() {
        return simpleSchedule;
    }

    public ScheduleInfo setSimpleSchedule(boolean simpleSchedule) {
        this.simpleSchedule = simpleSchedule;
        return this;
    }

    public CronSchedulerInfo getCronScheduleInfo() {
        return cronScheduleInfo;
    }

    public ScheduleInfo setCronScheduleInfo(CronSchedulerInfo cronScheduleInfo) {
        this.cronScheduleInfo = cronScheduleInfo;
        return this;
    }

    public TriggerInfo getTriggerInfo() {
        return triggerInfo;
    }

    public ScheduleInfo setTriggerInfo(TriggerInfo triggerInfo) {
        this.triggerInfo = triggerInfo;
        return this;
    }
}
