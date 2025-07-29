package com.naztech.BatchProcessing.config;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class BatchResourceHolder {
  private Resource resource;

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }
}