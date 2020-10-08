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

package com.proximyst.ban.inject;

import com.google.inject.AbstractModule;
import com.proximyst.ban.BanPlugin;
import com.proximyst.ban.config.Configuration;
import com.proximyst.ban.config.MessagesConfig;
import com.proximyst.ban.config.SqlConfig;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

public final class PluginModule extends AbstractModule {
  private final @NonNull BanPlugin main;

  public PluginModule(final @NonNull BanPlugin main) {
    this.main = main;
  }

  @Override
  protected void configure() {
    this.bind(BanPlugin.class).toInstance(this.main);
    this.bind(Logger.class).toInstance(this.main.getLogger());
    this.bind(ProxyServer.class).toInstance(this.main.getProxyServer());
    this.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(this.main.getDataDirectory());
    this.bind(File.class).annotatedWith(DataDirectory.class).toProvider(() -> this.main.getDataDirectory().toFile());

    this.bind(Configuration.class).toProvider(this.main::getConfiguration);
    this.bind(SqlConfig.class).toProvider(() -> this.main.getConfiguration().sql);
    this.bind(MessagesConfig.class).toProvider(() -> this.main.getConfiguration().messages);
  }
}
