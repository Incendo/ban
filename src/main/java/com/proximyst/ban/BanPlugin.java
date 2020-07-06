package com.proximyst.ban;

import co.aikar.commands.VelocityCommandManager;
import co.aikar.idb.DB;
import co.aikar.idb.DatabaseOptions;
import co.aikar.idb.PooledDatabaseOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.proximyst.ban.boilerplate.Slf4jLoggerProxy;
import com.proximyst.ban.boilerplate.model.MigrationIndexEntry;
import com.proximyst.ban.config.ConfigUtil;
import com.proximyst.ban.config.Configuration;
import com.proximyst.ban.data.SqlQueries;
import com.proximyst.ban.inject.CommandsModule;
import com.proximyst.ban.inject.PluginModule;
import com.proximyst.ban.utils.ResourceReader;
import com.proximyst.ban.utils.ThrowableUtils;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

@Plugin(
    id = BanPlugin.PLUGIN_ID,
    name = BanPlugin.PLUGIN_NAME,
    version = BanPlugin.PLUGIN_VERSION,
    description = BanPlugin.PLUGIN_DESCRIPTION,
    authors = "Proximyst"
)
public class BanPlugin {
  public static final String PLUGIN_ID = "ban";
  public static final String PLUGIN_NAME = "ban";
  public static final String PLUGIN_VERSION = "0.1.0";
  public static final String PLUGIN_DESCRIPTION = "A simple punishment suite for Velocity.";

  public static final Gson COMPACT_GSON = new Gson();

  @NonNull
  private final ProxyServer proxyServer;

  @NonNull
  private final Logger logger;

  @NonNull
  private final Path dataDirectory;

  @NonNull
  private final Injector injector;

  private VelocityCommandManager commandManager;
  private ConfigurationNode rawConfigurationNode;
  private Configuration configuration;

  @Inject
  public BanPlugin(
      @NonNull ProxyServer proxyServer,
      @NonNull Logger logger,
      @NonNull @DataDirectory Path dataDirectory
  ) {
    this.proxyServer = proxyServer;
    this.logger = logger;
    this.dataDirectory = dataDirectory;

    injector = Guice.createInjector(
        new PluginModule(this),
        new CommandsModule(this)
    );
  }

  @Subscribe
  public void onProxyInitialisation(ProxyInitializeEvent event) {
    getLogger().info("Reading configuration file...");
    // Just to ensure the parents exist.
    //noinspection ResultOfMethodCallIgnored
    getDataDirectory().toFile().mkdirs();

    // Load configuration.
    try {
      Path path = getDataDirectory().resolve("config.conf");
      // TODO: Use TOML configuration.
      HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
          .setPath(path)
          .build();

      // Loading...
      rawConfigurationNode = loader.load();
      configuration = ConfigUtil.loadConfiguration(getRawConfigurationNode());

      // Saving...
      ConfigUtil.saveConfiguration(getConfiguration(), getRawConfigurationNode());
      loader.save(getRawConfigurationNode());
    } catch (IOException | ObjectMappingException ex) {
      getLogger().error("Cannot read configuration", ex);
      return;
    }
    getLogger().info("Read configuration!");

    getLogger().info("Opening a pooled database...");
    DB.setGlobalDatabase(PooledDatabaseOptions.builder()
        .options(
            DatabaseOptions.builder()
                .mysql(
                    getConfiguration().getSql().getUsername(),
                    getConfiguration().getSql().getPassword(),
                    getConfiguration().getSql().getDatabase(),
                    getConfiguration().getSql().getHostname() + ":" + getConfiguration().getSql().getPort()
                )
                .useOptimizations(false)
                .logger(new Slf4jLoggerProxy(getLogger()))
                .build()
        )
        .maxConnections(getConfiguration().getSql().getMaxConnections())
        .createHikariDatabase());
    getLogger().info("Database pool opened!");

    getLogger().info("Preparing database...");
    String migrationsIndexJson = ResourceReader.readResource("sql/migrations/migrations-index.json");
    List<MigrationIndexEntry> migrationIndexEntries = COMPACT_GSON
        .fromJson(
            migrationsIndexJson,
            new TypeToken<List<MigrationIndexEntry>>() {
            }.getType()
        );
    try {
      // Ensure the table exists first.
      SqlQueries.CREATE_VERSION_TABLE.forEachQuery(DB::executeUpdate);

      int version = Optional.ofNullable(DB.getFirstRow(SqlQueries.SELECT_VERSION.getQuery()))
          .map(row -> row.getInt("version"))
          .orElse(0);
      migrationIndexEntries.stream()
          .filter(mig -> mig.getVersion() > version)
          .sorted(Comparator.comparingInt(MigrationIndexEntry::getVersion))
          .forEach(mig -> {
            getLogger().info("Migrating database to version " + mig.getVersion() + "...");
            String queries = ResourceReader.readResource("sql/migrations/" + mig.getPath());
            for (String query : queries.split(";")) {
              if (query.trim().isEmpty()) {
                continue;
              }

              try {
                DB.executeUpdate(query);
                DB.executeUpdate(SqlQueries.UPDATE_VERSION.getQuery(), mig.getVersion());
              } catch (SQLException ex) {
                // Streams are kinda stupid...
                ThrowableUtils.sneakyThrow(ex);
              }
            }
          });
    } catch (SQLException ex) {
      getLogger().error("Could not prepare database", ex);
      return;
    }
    getLogger().info("Database prepared!");

    getLogger().info("Initialising plugin essentials...");
    commandManager = new VelocityCommandManager(getProxyServer(), this);
    getLogger().info("Plugin essentials initialised!");

    getLogger().info("Plugin has finished initialisation.");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    getLogger().info("Closing database...");
    DB.close();
    getLogger().info("Closed database!");

    getLogger().info("Plugin disabled correctly.");
  }

  @NonNull
  public ProxyServer getProxyServer() {
    return proxyServer;
  }

  @NonNull
  public Logger getLogger() {
    return logger;
  }

  @NonNull
  public Injector getInjector() {
    return injector;
  }

  @NonNull
  public Path getDataDirectory() {
    return dataDirectory;
  }

  @NonNull
  public Configuration getConfiguration() {
    return configuration;
  }

  @NonNull
  public ConfigurationNode getRawConfigurationNode() {
    return rawConfigurationNode;
  }

  @NonNull
  public VelocityCommandManager getCommandManager() {
    return commandManager;
  }
}