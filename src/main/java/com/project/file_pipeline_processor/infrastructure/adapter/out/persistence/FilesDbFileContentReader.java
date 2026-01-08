package com.project.file_pipeline_processor.infrastructure.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FilesDbFileContentReader {

	private static final String SQL = "SELECT file_data FROM file_documents WHERE uuid = ?";

	private final JdbcTemplate filesDbJdbcTemplate;

	public FilesDbFileContentReader(@Qualifier("filesDbJdbcTemplate") JdbcTemplate filesDbJdbcTemplate) {
		this.filesDbJdbcTemplate = filesDbJdbcTemplate;
	}

	public Optional<byte[]> findFileDataByUuid(String uuid) {
		return filesDbJdbcTemplate.query(SQL, new FileDataMapper(), uuid)
				.stream()
				.findFirst();
	}

	private static class FileDataMapper implements RowMapper<byte[]> {
		@Override
		public byte[] mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getBytes("file_data");
		}
	}
}
