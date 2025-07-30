package com.m23ITSolution.BatchProcessing.config;

import com.m23ITSolution.BatchProcessing.entity.BatchProgress;
import com.m23ITSolution.BatchProcessing.repository.BatchProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionListenerImpl implements JobExecutionListener {
    private final BatchProgressRepository progressRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job Started");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job Completed");
            long lastLine = jobExecution.getStepExecutions().stream()
                .findFirst()
                .map(stepExecution -> stepExecution.getExecutionContext().getLong("lastProcessedLine", 0L))
                .orElse(0L);
            log.info("Saving last processed line: {}", lastLine);
            BatchProgress progress = progressRepository.findById(1).orElse(new BatchProgress());
            progress.setId(1);
            progress.setLastLine((int) lastLine);
            progressRepository.save(progress);
        }
    }
}