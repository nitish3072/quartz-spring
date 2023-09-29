package com.nitish.quartz.service;

import com.nitish.quartz.core.QuartzJob;
import com.nitish.quartz.core.JobSchedulerCreator;
import com.nitish.quartz.data.SchedulerJobInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Transactional
@Service
public class SchedulerJobService {

    private static final Logger log = LogManager.getLogger(SchedulerJobService.class);

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JobSchedulerCreator scheduleCreator;

    public <T extends QuartzJob> void saveOrUpdate(T quartzJob,
                                                   SchedulerJobInfo jobInfo,
                                                   Map<String, String> data) {
        if (jobInfo.getCronExpression()!=null && jobInfo.getCronExpression().length() > 0) {
            jobInfo.setJobClass(quartzJob.getJobClass().getName());
            jobInfo.setCronJob(true);
        } else {
            jobInfo.setJobClass(quartzJob.getJobClass().getName());
            jobInfo.setCronJob(false);
        }

        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            JobDetail jobDetail = JobBuilder
                    .newJob(quartzJob.getJobClass())
                    .withIdentity(jobInfo.getJobName(), jobInfo.getJobGroup()).build();
            if (!scheduler.checkExists(jobDetail.getKey())) {

                jobDetail = scheduleCreator.createJob(
                        quartzJob.getJobClass(),
                        false,
                        context,
                        jobInfo.getJobName(),
                        jobInfo.getJobGroup(),
                        data);

                Trigger trigger;
                if (jobInfo.getCronJob()) {
                    trigger = scheduleCreator.createCronTrigger(
                            jobInfo.getJobName(),
                            new Date(),
                            jobInfo.getCronExpression(),
                            SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
                } else {
                    trigger = scheduleCreator.createSimpleTrigger(
                            jobInfo.getJobName(),
                            new Date(),
                            jobInfo.getRepeatTime(),
                            SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT,
                            SimpleTrigger.REPEAT_INDEFINITELY);
                }
                scheduler.scheduleJob(jobDetail, trigger);
                log.info(">>>>> jobName = [" + jobInfo.getJobName() + "]" + " scheduled.");
            } else {
                Trigger newTrigger;
                if (jobInfo.getCronJob()) {

                    newTrigger = scheduleCreator.createCronTrigger(
                            jobInfo.getJobName(),
                            new Date(),
                            jobInfo.getCronExpression(),
                            SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
                } else {
                    newTrigger = scheduleCreator.createSimpleTrigger(
                            jobInfo.getJobName(),
                            new Date(),
                            jobInfo.getRepeatTime(),
                            SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW,
                            SimpleTrigger.REPEAT_INDEFINITELY);
                }
                schedulerFactoryBean.getScheduler().rescheduleJob(TriggerKey.triggerKey(jobInfo.getJobName()), newTrigger);
                log.info(">>>>> jobName = [" + jobInfo.getJobName() + "]" + " updated and scheduled.");
            }
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
        log.info(">>>>> jobName = [" + jobInfo.getJobName() + "]" + " created.");
    }

    public boolean deleteJob(SchedulerJobInfo jobInfo) {
        try {
            boolean deleted = schedulerFactoryBean.getScheduler().deleteJob(new JobKey(jobInfo.getJobName(), jobInfo.getJobGroup()));
            log.info(">>>>> jobName = [" + jobInfo.getJobName() + "]" + " deleted: " + deleted);
            return deleted;
        } catch (SchedulerException e) {
            log.error("Failed to delete job - {}", jobInfo.getJobName(), e);
            return false;
        }
    }

}
