package com.project.file_pipeline_processor.infrastructure.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FileDocumentsJdbcRepository {

    private static final String SQL_SELECT = "SELECT file_data FROM file_documents WHERE uuid = ?";
    private static final String SQL_UPDATE_HASH = "UPDATE file_documents SET file_hash = ? WHERE uuid = ?";

    private final JdbcTemplate filesDbJdbcTemplate;

    public FileDocumentsJdbcRepository(@Qualifier("filesDbJdbcTemplate") JdbcTemplate filesDbJdbcTemplate) {
        this.filesDbJdbcTemplate = filesDbJdbcTemplate;
    }

    public Optional<byte[]> findFileDataByUuid(String uuid) {
        return filesDbJdbcTemplate.query(SQL_SELECT, new FileDataMapper(), uuid).stream().findFirst();
    }

    public void updateHashByUuid(String uuid, String hash) {
        int updated = filesDbJdbcTemplate.update(SQL_UPDATE_HASH, hash, uuid);
        if (updated == 0) {
            throw new IllegalArgumentException("UUID not found: " + uuid);
        }
    }

    private static class FileDataMapper implements RowMapper<byte[]> {
        @Override
        public byte[] mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getBytes("file_data");
        }
    }
}
