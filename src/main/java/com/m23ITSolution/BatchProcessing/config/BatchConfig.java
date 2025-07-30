package com.m23ITSolution.BatchProcessing.config;

import com.m23ITSolution.BatchProcessing.entity.BatchProgress;
import com.m23ITSolution.BatchProcessing.entity.ProductInfo;
import com.m23ITSolution.BatchProcessing.repository.BatchProgressRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
@Configuration
@RequiredArgsConstructor
@IntegrationComponentScan
public class BatchConfig {

  private final JobRepository jobRepository;
  private final EntityManagerFactory entityManagerFactory;
  private final PlatformTransactionManager transactionManager;
  private final JobLauncher jobLauncher;
  private final JobExecutionListenerImpl listener;
  private final BatchProgressRepository progressRepository;

  @Value("${batch.csv.directory:C:\\tmp\\Batch}")
  private String csvDirectoryPath;

  @Value("${batch.csv.file:product_infoNew.csv}")
  private String csvFileName;

  @Value("${batch.csv.fields:name,description,unitPrice}")
  private String[] csvFieldNames;

  private long lastModifiedTime = 0;

  @Bean
  public Job jobBean(Step jobSteps) {
    return new JobBuilder("jobBean", jobRepository)
        .start(jobSteps)
        .listener(listener)
        .build();
  }

  @Bean
  public Step steps(ItemStreamReader<ProductInfo> incrementalReader) {
    return new StepBuilder("jobSteps", jobRepository)
        .<ProductInfo, ProductInfo>chunk(100, transactionManager)
        .reader(incrementalReader)
        .processor(itemProcessor())
        .writer(itemWriter())
        .faultTolerant()
        .skip(Exception.class) // Consider specifying more granular exceptions, e.g., DataIntegrityViolationException
        .skipLimit(10)
        .build();
  }

  @Bean
  public ItemStreamReader<ProductInfo> incrementalReader() {
    Resource resource = new FileSystemResource(new File(csvDirectoryPath, csvFileName));
    return new IncrementalFlatFileItemReader(resource, csvFieldNames, progressRepository);
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
  public IntegrationFlow fileTranslationFlow() {
    return IntegrationFlow.from(
            Files.inboundAdapter(new File(csvDirectoryPath))
                .patternFilter(csvFileName)
                .preventDuplicates(false),
            e -> e.poller(Pollers.fixedDelay(5000)))
        .filter(this::isFileModified)
        .handle(message -> {
          File file = (File) message.getPayload();
          try {
            log.info("Processing file: {}", file.getName());
            handleFileProcessing(file);
          } catch (Exception e) {
            log.error("Failed to process file: {}", file.getName(), e);
            throw new RuntimeException("Failed to process file: " + file.getName(), e);
          }
        })
        .get();
  }

  private boolean isFileModified(Object payload) {
    File file = (File) payload;
    try {
      if (!file.exists() || !file.canRead()) {
        log.warn("File {} does not exist or is not readable", file.getName());
        return false;
      }
      BasicFileAttributes attrs = java.nio.file.Files.readAttributes(file.toPath(), BasicFileAttributes.class);
      long currentModifiedTime = attrs.lastModifiedTime().toMillis();
      if (currentModifiedTime > lastModifiedTime) {
        log.info("Detected modification in file: {}, last modified: {}", file.getName(), currentModifiedTime);
        lastModifiedTime = currentModifiedTime;
        return true;
      }
      log.debug("No changes detected in file: {}", file.getName());
      return false;
    } catch (Exception e) {
      log.error("Error checking file attributes: {}", file.getName(), e);
      throw new RuntimeException("Error checking file attributes: " + file.getName(), e);
    }
  }

  private void handleFileProcessing(File file) throws Exception {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addLong("timestamp", System.currentTimeMillis());
    jobLauncher.run(jobBean(steps(incrementalReader())), paramsBuilder.toJobParameters());
  }

  public static class IncrementalFlatFileItemReader implements ItemStreamReader<ProductInfo> {
    private final FlatFileItemReader<ProductInfo> delegate = new FlatFileItemReader<>();
    private long lineCount = 0;
    private final BatchProgressRepository progressRepository;

    public IncrementalFlatFileItemReader(Resource resource, String[] csvFieldNames, BatchProgressRepository progressRepository) {
      this.progressRepository = progressRepository;
      delegate.setResource(resource);
      delegate.setLinesToSkip(1); // Skip header
      DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
      tokenizer.setNames(csvFieldNames);

      BeanWrapperFieldSetMapper<ProductInfo> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
      fieldSetMapper.setTargetType(ProductInfo.class);

      DefaultLineMapper<ProductInfo> lineMapper = new DefaultLineMapper<>();
      lineMapper.setLineTokenizer(tokenizer);
      lineMapper.setFieldSetMapper(fieldSetMapper);

      delegate.setLineMapper(lineMapper);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
      BatchProgress progress = progressRepository.findById(1).orElse(new BatchProgress());
      lineCount = progress.getLastLine() != null ? progress.getLastLine() : executionContext.getLong("lastProcessedLine", 0L);
      log.info("Starting reader at line: {}", lineCount);
      delegate.open(executionContext);
      for (long i = 0; i < lineCount; i++) {
        try {
          delegate.read();
        } catch (Exception e) {
          throw new RuntimeException("Error skipping lines at position " + i, e);
        }
      }
    }

    @Override
    public ProductInfo read() throws Exception {
      ProductInfo item = delegate.read();
      if (item != null) {
        lineCount++;
        log.debug("Read item at line {}: {}", lineCount, item);
      }
      return item;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
      log.info("Updating ExecutionContext with lastProcessedLine: {}", lineCount);
      executionContext.putLong("lastProcessedLine", lineCount);
      delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
      delegate.close();
    }
  }
}