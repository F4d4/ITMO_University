package org.example.batch;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.example.repository.VideoRepository;
import org.example.service.MinioService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
public class VideoCleanupBatchConfig {

    @Value("${app.cleanup.uploading-timeout-seconds:1}")
    private int timeoutSeconds;

    @Bean
    public Job videoCleanupJob(JobRepository jobRepository, Step videoCleanupStep) {
        return new JobBuilder("videoCleanupJob", jobRepository)
                .start(videoCleanupStep)
                .build();
    }

    @Bean
    public Step videoCleanupStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  ItemReader<Video> videoCleanupItemReader,
                                  ItemProcessor<Video, Video> videoCleanupItemProcessor,
                                  ItemWriter<Video> videoCleanupItemWriter) {
        return new StepBuilder("videoCleanupStep", jobRepository)
                .<Video, Video>chunk(2, transactionManager)
                .reader(videoCleanupItemReader)
                .processor(videoCleanupItemProcessor)
                .writer(videoCleanupItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Video> videoCleanupItemReader(VideoRepository videoRepository) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        List<Video> videos = videoRepository.findByStatusAndUpdatedAtBefore(VideoStatus.UPLOADING, cutoff);
        return new ListItemReader<>(videos);
    }

    @Bean
    public ItemProcessor<Video, Video> videoCleanupItemProcessor(MinioService minioService) {
        return video -> {
            log.info("Batch: удаление MinIO файла для видео id={}, key={}", video.getId(), video.getMinioKey());
            minioService.deleteObject(video.getBucketName(), video.getMinioKey());
            return video;
        };
    }

    @Bean
    public ItemWriter<Video> videoCleanupItemWriter(VideoRepository videoRepository) {
        return chunk -> {
            List<Long> ids = chunk.getItems().stream().map(Video::getId).toList();
            log.info("Batch: удаление {} записей о мусорных видео из БД: {}", ids.size(), ids);
            videoRepository.deleteAllByIdInBatch(ids);
        };
    }
}
