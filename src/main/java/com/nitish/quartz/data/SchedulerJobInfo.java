package com.nitish.quartz.data;

import lombok.Data;

@Data
public class SchedulerJobInfo {

    private String jobName;
    private String jobGroup;
    private String jobStatus;
    private String jobClass;
    private String cronExpression;
    private String description;
    private Long repeatTime;
    private Boolean cronJob;

}