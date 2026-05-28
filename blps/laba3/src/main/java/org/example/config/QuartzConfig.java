package org.example.config;

import org.example.batch.VideoCleanupQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail videoCleanupJobDetail() {
        return JobBuilder.newJob(VideoCleanupQuartzJob.class)
                .withIdentity("videoCleanupJob", "cleanup")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger videoCleanupTrigger(JobDetail videoCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(videoCleanupJobDetail)
                .withIdentity("videoCleanupTrigger", "cleanup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))
                .build();
    }
}
