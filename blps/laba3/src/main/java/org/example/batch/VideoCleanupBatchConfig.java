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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class VideoCleanupBatchConfig {

    @Value("${app.cleanup.uploading-timeout-hours:24}")
    private int timeoutHours;

    @Bean
    public Job videoCleanupJob(JobRepository jobRepository, Step videoCleanupStep) {
        return new JobBuilder("videoCleanupJob", jobRepository)
                .start(videoCleanupStep)
                .build();
    }

    @Bean
    public Step videoCleanupStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  RepositoryItemReader<Video> videoCleanupItemReader,
                                  ItemProcessor<Video, Video> videoCleanupItemProcessor,
                                  ItemWriter<Video> videoCleanupItemWriter) {
        return new StepBuilder("videoCleanupStep", jobRepository)
                .<Video, Video>chunk(10, transactionManager)
                .reader(videoCleanupItemReader)
                .processor(videoCleanupItemProcessor)
                .writer(videoCleanupItemWriter)
                .build();
    }

    @Bean
    @Scope("step")
    public RepositoryItemReader<Video> videoCleanupItemReader(VideoRepository videoRepository) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(timeoutHours);
        return new RepositoryItemReaderBuilder<Video>()
                .name("videoCleanupItemReader")
                .repository(videoRepository)
                .methodName("findByStatusAndUpdatedAtBefore")
                .arguments(VideoStatus.UPLOADING, cutoff)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(10)
                .build();
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
