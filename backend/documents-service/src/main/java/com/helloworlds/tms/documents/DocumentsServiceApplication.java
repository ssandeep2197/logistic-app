package com.helloworlds.tms.documents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Document storage + OCR + LLM extraction.  Rate-confirmation pipeline:
 * upload to S3 → SQS-style event → PDFBox text extract (or Textract for scans)
 * → Anthropic Sonnet structured output → human review queue → load creation.
 * Phase-1 stub.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentsServiceApplication {
    public static void main(String[] args) { SpringApplication.run(DocumentsServiceApplication.class, args); }
}
