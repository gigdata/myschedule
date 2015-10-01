package myschedule.rest.util;

import myschedule.rest.domain.RunTimeJobDetail;
import myschedule.rest.exception.JobAlreadyExistsException;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 
 * @author gigdata@semaifour.com
 *
 */
public class SchedulerHelper {
	
    private org.quartz.Scheduler quartzScheduler;

    public SchedulerHelper(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    /**
     * @param jobDetail
     * @param trigger
     * @return
     * @throws SchedulerException
     * @throws JobAlreadyExistsException
     */
    public RunTimeJobDetail scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException, JobAlreadyExistsException {
        if(quartzScheduler.checkExists(jobDetail.getKey()))
            throw  new JobAlreadyExistsException(jobDetail.getKey().getName()+" already exists");
        quartzScheduler.scheduleJob(jobDetail, trigger);
        return getJobDetails(jobDetail.getKey().getGroup(), jobDetail.getKey().getName());
    }

    /**
     * @param jobKey
     * @throws SchedulerException
     */
    public void removeJob(JobKey jobKey) throws SchedulerException{
        quartzScheduler.deleteJob(jobKey);
    }

    /**
     * @throws SchedulerException
     */
    public void removeAllJobs() throws SchedulerException {
        for (String groupName : quartzScheduler.getJobGroupNames()) {
            for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                quartzScheduler.deleteJob(jobKey);
            }
        }
    }

    /**
     * @return
     * @throws SchedulerException
     */
    public List<RunTimeJobDetail> listAllJobs() throws SchedulerException {
        List<RunTimeJobDetail> jobs = new ArrayList<RunTimeJobDetail>();
        for (String groupName : quartzScheduler.getJobGroupNames()) {
            for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                jobs.add(getJobDetails(groupName, jobKey.getName()));
            }
        }
        return jobs;
    }

    /**
     * @param groupExp
     * @param nameExp
     * @return
     * @throws SchedulerException
     */
    public List<RunTimeJobDetail> searchJobs(String groupExp, String nameExp) throws SchedulerException {
        Pattern groupPattern = Pattern.compile(groupExp);
        Pattern namePattern = Pattern.compile(nameExp);

        List<RunTimeJobDetail> jobs = new ArrayList<RunTimeJobDetail>();
        for (String groupName : quartzScheduler.getJobGroupNames()) {
            if(groupPattern.matcher(groupName).matches()) {
                for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    if(namePattern.matcher(jobKey.getName()).matches()) {
                        jobs.add(getJobDetails(groupName, jobKey.getName()));
                    }
                }
            }
        }
        return jobs;
    }

    /**
     * Search Trigger
     * @param group
     * @param name
     * @return
     * @throws SchedulerException
     */
   
    public Trigger getTrigger(String group, String name) throws SchedulerException {
        return quartzScheduler.getTrigger(new TriggerKey(name, group));
    }

    /**
     * @param group
     * @param name
     * @return
     * @throws SchedulerException
     */
    public RunTimeJobDetail getJobDetails(String group, String name) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        return new RunTimeJobDetail().
                setJobDetail(quartzScheduler.getJobDetail(jobKey)).
                setTriggers(quartzScheduler.getTriggersOfJob(jobKey));
    }

    /**
     * @param jobKey
     * @throws SchedulerException
     */
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        quartzScheduler.pauseJob(jobKey);
    }

    /**
     * @param jobKey
     * @throws SchedulerException
     */
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        quartzScheduler.resumeJob(jobKey);
    }

    /**
     * @param metricAlias
     * @param duration
     * @param interval
     * @return
     */
    public String buildTriggerName(String metricAlias, String duration, String interval) {
        return String.format("Trigger.%s.%s.%s",metricAlias,duration,interval);
    }

    /**
     * @return
     */
    public Scheduler getQuartzScheduler() {
        return quartzScheduler;
    }

    /**
     * @param quartzScheduler
     */
    public void setQuartzScheduler(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    /**
     * @return
     * @throws SchedulerException
     */
    public SchedulerHelper standby() throws SchedulerException {
        this.quartzScheduler.standby();
        return this;
    }

    /**
     * @return
     * @throws SchedulerException
     */
    public SchedulerHelper start() throws SchedulerException {
        this.quartzScheduler.start();
        return this;
    }
}

