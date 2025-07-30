package com.m23ITSolution.BatchProcessing.config;

import com.m23ITSolution.BatchProcessing.entity.ProductInfo;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

  private final JobRepository jobRepository;

  private final EntityManagerFactory entityManagerFactory;

  private final PlatformTransactionManager transactionManager;

  private final JobExecutionListenerImpl listener;

  private final BatchResourceHolder batchResourceHolder;

  @Value("${batch.csv.file:product_info.csv}")
  private String csvFilePath;

  @Value("${batch.csv.fields:name,description,unitPrice}")
  private String[] csvFieldNames;

  @Bean
  public Job jobBean() {
    return new JobBuilder("jobBean", jobRepository)
        .start(steps())
        .listener(listener)
        .build();
  }

  @Bean
  public Step steps() {
    return new StepBuilder("jobSteps", jobRepository)
        .<ProductInfo, ProductInfo>chunk(100, transactionManager)
        .reader(reader())
        .processor(itemProcessor())
        .writer(itemWriter())
        .faultTolerant()
        .skip(Exception.class)
        .skipLimit(10)
        .taskExecutor(taskExecutor())
        .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<ProductInfo> reader() {
    Resource resource = batchResourceHolder.getResource();
    if (resource == null) {
      throw new IllegalStateException("No resource set for batch processing");
    }
    return new FlatFileItemReaderBuilder<ProductInfo>()
        .name("itemReader")
        .resource(resource)
        .linesToSkip(1)
        .delimited()
        .names(csvFieldNames)
        .targetType(ProductInfo.class)
        .build();
  }
  @Bean
  public ItemProcessor<ProductInfo, ProductInfo> itemProcessor() {
    return new CustomItemProcessor();
  }

  @Bean
  public JpaItemWriter<ProductInfo> itemWriter() {
    return new JpaItemWriterBuilder<ProductInfo>()
        .entityManagerFactory(entityManagerFactory)
        .usePersist(true)
        .build();
  }

  @Bean
  public TaskExecutor taskExecutor(){
    SimpleAsyncTaskExecutor asyncTaskExecutor=new SimpleAsyncTaskExecutor();
    asyncTaskExecutor.setConcurrencyLimit(10);
    return asyncTaskExecutor;
  }


}