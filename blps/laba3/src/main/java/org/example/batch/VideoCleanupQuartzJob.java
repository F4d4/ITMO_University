package org.example.batch;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public class VideoCleanupQuartzJob implements Job {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("videoCleanupJob")
    private org.springframework.batch.core.Job videoCleanupBatchJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Quartz: запуск Spring Batch задания очистки мусорных видео");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(videoCleanupBatchJob, params);
            log.info("Quartz: задание очистки завершено");
        } catch (Exception e) {
            log.error("Quartz: ошибка выполнения задания очистки: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
