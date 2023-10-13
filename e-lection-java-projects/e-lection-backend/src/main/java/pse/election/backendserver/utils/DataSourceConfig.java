package pse.election.backendserver.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class provides the data source configuration.
 * */
@Configuration
public class DataSourceConfig {

  private static final String MY_SQL_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String MY_SQL_DEFAULT_URL = "jdbc:mysql://localhost:3306/e_lection_db";
  private static final String URL = "url";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";

  @Value("${dataSource:#{null}}")
  private String dataSourceConfigPath;

  @Bean
  public DataSource dataSource() throws IOException {
    return this.dataSourceConfigPath == null
        ? this.defaultDataSource() : this.dataSourceFromConfig();
  }

  private DataSource dataSourceFromConfig() throws IOException {
    final File dataSourceConfig = new File(dataSourceConfigPath);
    assert dataSourceConfig.exists();
    assert dataSourceConfig.isFile();
    assert dataSourceConfig.canRead();

    final TomlMapper mapper = new TomlMapper();
    final Map<String, String> fileContent = mapper.readValue(dataSourceConfig,
        new TypeReference<>() {
        });

    final String url = fileContent.getOrDefault(URL, MY_SQL_DEFAULT_URL);

    return DataSourceBuilder.create()
        .username(fileContent.get(USERNAME))
        .password(fileContent.get(PASSWORD))
        .driverClassName(fileContent.get(MY_SQL_DRIVER_CLASS_NAME))
        .url(fileContent.getOrDefault(URL, MY_SQL_DEFAULT_URL))
        .build();
  }

  private DataSource defaultDataSource() {
    return DataSourceBuilder.create()
        .username("sa")
        .password("sa")
        .url("jdbc:h2:mem:db")
        .driverClassName("org.h2.Driver")
        .build();
  }
}
