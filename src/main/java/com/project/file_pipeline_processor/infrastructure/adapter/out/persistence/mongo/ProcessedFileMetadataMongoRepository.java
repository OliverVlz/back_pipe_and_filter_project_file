package com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedFileMetadataMongoRepository extends MongoRepository<ProcessedFileMetadataDocument, String> {

}
