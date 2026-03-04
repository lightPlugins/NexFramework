package io.nexstudios.framework.data.configuration;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
public class DatabaseConfiguration {

  /**
   * Supported: sqlite, mariadb, mysql
   */
  String type;

  /**
   * SQLite database file path relative to plugin data folder (e.g. "database.sqlite").
   * Only used when type = "sqlite".
   */
  String file;

  /**
   * Host/port/name only used when type = "mariadb" or "mysql".
   */
  String host;
  int port;
  String database;

  String username;
  String password;

  /**
   * If true, adds useSSL=true to the JDBC URL for mysql/mariadb.
   */
  boolean useSSL;

  /**
   * Optional extra JDBC parameters appended to the URL, e.g. "useUnicode=true&characterEncoding=utf8".
   * Without leading '?'.
   */
  @Builder.Default
  String jdbcParameters = "";

  String jdbcUrl;

  String poolName;
  int maximumPoolSize;
  int minimumIdle;

  long connectionTimeoutMillis;
  long idleTimeoutMillis;
  long maxLifetimeMillis;

  /**
   * Optional, user-provided extra Hibernate properties (string -> string).
   * Defaults to empty map when not configured.
   */
  @Builder.Default
  Map<String, String> additionalHibernateProperties = Map.of();

  /**
   * Optional, defaults to "validate".
   * Kept here (instead of "additionalHibernateProperties") because it is a common, important knob.
   */
  @Builder.Default
  String hbm2ddlAuto = "validate";
}