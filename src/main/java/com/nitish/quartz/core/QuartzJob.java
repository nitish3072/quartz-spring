package com.nitish.quartz.core;

import org.springframework.scheduling.quartz.QuartzJobBean;

public abstract class QuartzJob extends QuartzJobBean {

    public abstract Class<? extends QuartzJob> getJobClass();

}
