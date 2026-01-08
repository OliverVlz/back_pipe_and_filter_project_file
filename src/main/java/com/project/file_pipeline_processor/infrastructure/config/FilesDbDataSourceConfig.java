package com.project.file_pipeline_processor.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class FilesDbDataSourceConfig {

	@Bean
	@ConfigurationProperties("filesdb.datasource")
	public DataSourceProperties filesDbDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@ConfigurationProperties("filesdb.datasource.hikari")
	public DataSource filesDbDataSource(@Qualifier("filesDbDataSourceProperties") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean
	public JdbcTemplate filesDbJdbcTemplate(@Qualifier("filesDbDataSource") DataSource dataSource) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setFetchSize(1);
		return jdbcTemplate;
	}
}
