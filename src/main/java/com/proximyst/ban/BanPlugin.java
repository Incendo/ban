//
// ban - A punishment suite for Velocity.
// Copyright (C) 2020 Mariell Hoversholm
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//

package com.proximyst.ban;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.proximyst.ban.boilerplate.VelocityBanSchedulerExecutor;
import com.proximyst.ban.boilerplate.model.MigrationIndexEntry;
import com.proximyst.ban.commands.BanCommand;
import com.proximyst.ban.commands.UnbanCommand;
import com.proximyst.ban.config.ConfigUtil;
import com.proximyst.ban.config.Configuration;
import com.proximyst.ban.data.IDataInterface;
import com.proximyst.ban.data.IMojangApi;
import com.proximyst.ban.data.impl.MojangApiAshcon;
import com.proximyst.ban.data.impl.MySqlInterface;
import com.proximyst.ban.data.jdbi.UuidJdbiFactory;
import com.proximyst.ban.event.subscriber.BannedPlayerJoinSubscriber;
import com.proximyst.ban.event.subscriber.CacheUpdatePlayerJoinSubscriber;
import com.proximyst.ban.event.subscriber.MutedPlayerChatSubscriber;
import com.proximyst.ban.inject.DataModule;
import com.proximyst.ban.inject.PluginModule;
import com.proximyst.ban.manager.MessageManager;
import com.proximyst.ban.manager.PunishmentManager;
import com.proximyst.ban.manager.UserManager;
import com.proximyst.ban.utils.ResourceReader;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;

@Plugin(
    id = BanPlugin.PLUGIN_ID,
    name = BanPlugin.PLUGIN_NAME,
    version = BanPlugin.PLUGIN_VERSION,
    description = BanPlugin.PLUGIN_DESCRIPTION,
    authors = "Proximyst"
)
public class BanPlugin {
  public static final String PLUGIN_ID = "banstret";
  public static final String PLUGIN_NAME = "banstret";
  public static final String PLUGIN_VERSION = "0.1.0";
  public static final String PLUGIN_DESCRIPTION = "A simple punishment suite for Velocitymonstret.";

  public static final Gson COMPACT_GSON = new Gson();

  @NonNull
  private final ProxyServer proxyServer;

  @NonNull
  private final Logger logger;

  @NonNull
  private final Path dataDirectory;

  @NonNull
  private final Injector injector;

  @NonNull
  private final VelocityBanSchedulerExecutor schedulerExecutor;

  private ConfigurationNode rawConfigurationNode;
  private Configuration configuration;
  private IDataInterface dataInterface;
  private PunishmentManager punishmentManager;
  private MessageManager messageManager;
  private UserManager userManager;
  private IMojangApi mojangApi;
  private HikariDataSource hikariDataSource;
  private Jdbi jdbi;

  @Inject
  public BanPlugin(
      @NonNull ProxyServer proxyServer,
      @NonNull Logger logger,
      @NonNull @DataDirectory Path dataDirectory
  ) {
    this.proxyServer = proxyServer;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.schedulerExecutor = new VelocityBanSchedulerExecutor(this);

    injector = Guice.createInjector(
        new PluginModule(this),
        new DataModule(this)
    );
  }

  @Subscribe
  public void onProxyInitialisation(ProxyInitializeEvent event) {
    if (!getProxyServer().getConfiguration().isOnlineMode()) {
      getLogger().error("This plugin cannot function on offline mode.");
      getLogger().error("This plugin depends on Mojang's API and the presence of online mode players.");
      getLogger().error("Please either enable online mode, or find a new punishments plugin.");
      return;
    }

    long start = System.currentTimeMillis();
    TimeMeasurer tm = new TimeMeasurer(getLogger());

    tm.start("Reading configuration file");
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

    tm.start("Opening database pool");
    try {
      DriverManager.registerDriver(new org.mariadb.jdbc.Driver());
    } catch (SQLException ex) {
      getLogger().error("Could not register a SQL driver", ex);
      return;
    }

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(getConfiguration().sql.jdbcUri);
    hikariConfig.setUsername(getConfiguration().sql.username);
    hikariConfig.setPassword(getConfiguration().sql.password);
    hikariConfig.setMaximumPoolSize(getConfiguration().sql.maxConnections);
    hikariDataSource = new HikariDataSource(hikariConfig);
    jdbi = Jdbi.create(hikariDataSource)
        .setSqlLogger(new SqlLogger() {
          @Override
          public void logException(StatementContext context, SQLException ex) {
            getLogger().warn("Could not execute JDBI statement.", ex);
          }
        })
        .registerArgument(new UuidJdbiFactory());
    dataInterface = new MySqlInterface(getLogger(), getJdbi());

    tm.start("Preparing database");
    String migrationsIndexJson = ResourceReader.readResource("sql/migrations/migrations-index.json");
    List<MigrationIndexEntry> migrationIndexEntries = COMPACT_GSON
        .fromJson(
            migrationsIndexJson,
            new TypeToken<List<MigrationIndexEntry>>() {
            }.getType()
        );
    try {
      getDataInterface().applyMigrations(migrationIndexEntries);
    } catch (Exception ex) {
      getLogger().error("Could not prepare database", ex);
      return;
    }

    tm.start("Initialising plugin essentials");
    mojangApi = new MojangApiAshcon(getSchedulerExecutor());
    punishmentManager = new PunishmentManager(this);
    messageManager = new MessageManager(this, getConfiguration().messages);
    userManager = new UserManager(this);

    tm.start("Registering subscribers");
    getProxyServer().getEventManager().register(this, getInjector().getInstance(BannedPlayerJoinSubscriber.class));
    getProxyServer().getEventManager().register(this, getInjector().getInstance(MutedPlayerChatSubscriber.class));
    getProxyServer().getEventManager().register(this, getInjector().getInstance(CacheUpdatePlayerJoinSubscriber.class));

    tm.start("Registering commands");
    getInjector().getInstance(BanCommand.class).register(getProxyServer().getCommandManager());
    getInjector().getInstance(UnbanCommand.class).register(getProxyServer().getCommandManager());

    tm.finish();
    getLogger().info("Plugin has finished initialisation in {}ms.", System.currentTimeMillis() - start);
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    long start = System.currentTimeMillis();
    TimeMeasurer tm = new TimeMeasurer(getLogger());

    tm.start("Unregistering listeners");
    getProxyServer().getEventManager().unregisterListeners(this);

    tm.start("Closing database");
    if (hikariDataSource != null) {
      hikariDataSource.close();
    }

    tm.finish();
    getLogger().info("Plugin disabled correctly in {}ms.", System.currentTimeMillis() - start);
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
  public IDataInterface getDataInterface() {
    return dataInterface;
  }

  @NonNull
  public PunishmentManager getPunishmentManager() {
    return punishmentManager;
  }

  @NonNull
  public MessageManager getMessageManager() {
    return messageManager;
  }

  @NonNull
  public UserManager getUserManager() {
    return userManager;
  }

  @NonNull
  public IMojangApi getMojangApi() {
    return mojangApi;
  }

  @NonNull
  public Jdbi getJdbi() {
    return jdbi;
  }

  @NonNull
  public VelocityBanSchedulerExecutor getSchedulerExecutor() {
    return schedulerExecutor;
  }

  private static class TimeMeasurer {
    private final Logger logger;
    private long start;
    private String current;

    public TimeMeasurer(Logger logger) {
      this.logger = logger;
    }

    public void start(String stage) {
      if (start != 0) {
        finish();
      }

      start = System.nanoTime();
      current = stage;
    }

    public void finish() {
      if (start == 0) {
        return;
      }

      long duration = System.nanoTime() - start;
      start = 0;
      logger.info("Finished stage ({}ms): {}", TimeUnit.NANOSECONDS.toMillis(duration), current);
    }
  }
}
