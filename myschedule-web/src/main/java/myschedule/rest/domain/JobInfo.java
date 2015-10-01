package myschedule.rest.domain;

import java.util.HashMap;
import java.util.Map;


public class JobInfo {
  
	private Class jobClass;
    private String name;
    private String group;
    private String description;
    private boolean storeDurably;
    private boolean requestRecovery;
    private Map<String, Object> jobParams = new  HashMap<String, Object> ();

    public Class getJobClass() {
        return jobClass;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public boolean isStoreDurably() {
        return storeDurably;
    }

    public Map<String, Object> getJobParams() {
        return jobParams;
    }

    public JobInfo setJobClass(Class jobClass) {
        this.jobClass = jobClass;
        return this;
    }

    public JobInfo setName(String name) {
        this.name = name;
        return this;
    }

    public JobInfo setGroup(String group) {
        this.group = group;
        return this;
    }

    public JobInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public JobInfo setStoreDurably(boolean storeDurably) {
        this.storeDurably = storeDurably;
        return this;
    }

    public JobInfo setJobParams(Map<String, Object> jobParams) {
        this.jobParams = jobParams;
        return this;
    }

    public JobInfo setRequestRecovery(boolean requestRecovery) {
        this.requestRecovery = requestRecovery;
        return this;
    }

    public boolean isRequestRecovery() {
        return requestRecovery;
    }
}
