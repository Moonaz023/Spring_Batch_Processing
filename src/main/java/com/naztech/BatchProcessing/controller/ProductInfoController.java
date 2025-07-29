package com.naztech.BatchProcessing.controller;
import com.naztech.BatchProcessing.config.BatchResourceHolder;
import com.naztech.BatchProcessing.entity.ProductInfo;
import com.naztech.BatchProcessing.service.ProductInfoService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductInfoController {
    private final ProductInfoService productInfoService;
    private final JobLauncher jobLauncher;
    private final Job jobBean;
    private final BatchResourceHolder batchResourceHolder;

    @PostMapping("/saveProduct")
    public ProductInfo saveProduct(@RequestBody ProductInfo newProduct) {
        return productInfoService.saveProduct(newProduct);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        try {
            //  Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a valid CSV file");
            }
            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Only CSV files are allowed");
            }

            // Create a Resource from the MultipartFile's InputStream
            Resource resource = new InputStreamResource(file.getInputStream()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            // Set the resource in the holder
            batchResourceHolder.setResource(resource);

            // Launch the batch job
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

            jobLauncher.run(jobBean, jobParameters);

            // Clear the resource after job execution
            batchResourceHolder.setResource(null);

            return ResponseEntity.ok("CSV file uploaded and batch processing started successfully");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to process CSV file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to launch batch job: " + e.getMessage());
        }
    }



}
