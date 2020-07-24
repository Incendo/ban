package com.proximyst.ban.data.impl;

import com.google.inject.Singleton;
import com.proximyst.ban.boilerplate.model.MigrationIndexEntry;
import com.proximyst.ban.data.IDataInterface;
import com.proximyst.ban.data.SqlQueries;
import com.proximyst.ban.model.Punishment;
import com.proximyst.ban.utils.ResourceReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

/**
 * A MySQL based interface to the data of this plugin.
 */
@Singleton
public class MySqlInterface implements IDataInterface {
  @NonNull
  private final Logger logger;

  @NonNull
  private final Jdbi jdbi;

  public MySqlInterface(@NonNull Logger logger, @NonNull Jdbi jdbi) {
    this.logger = logger;
    this.jdbi = jdbi;
  }

  @Override
  public void applyMigrations(@NonNull final List<MigrationIndexEntry> migrations) {
    jdbi.useHandle(handle -> {
      // Ensure the table exists first.
      SqlQueries.CREATE_VERSION_TABLE.forEachQuery(handle::execute);

      int version = handle.createQuery(SqlQueries.SELECT_VERSION.getQuery())
          .mapTo(int.class)
          .findOne()
          .orElse(0);
      migrations.stream()
          .filter(mig -> mig.getVersion() > version)
          .sorted(Comparator.comparingInt(MigrationIndexEntry::getVersion))
          .forEach(mig -> {
            logger.info("Migrating database to version " + mig.getVersion() + "...");
            String queries = ResourceReader.readResource("sql/migrations/" + mig.getPath());
            for (String query : queries.split(";")) {
              if (query.trim().isEmpty()) {
                continue;
              }

              handle.execute(query);
              handle.execute(SqlQueries.UPDATE_VERSION.getQuery(), mig.getVersion());
            }
          });
    });
  }

  @Override
  @NonNull
  public List<Punishment> getPunishmentsForTarget(@NonNull UUID target) {
    return jdbi.withHandle(handle ->
        handle.createQuery(SqlQueries.SELECT_PUNISHMENTS_BY_TARGET.getQuery())
            .bind(1, target)
            .map(Punishment::fromRow)
            .stream()
            .sorted(Comparator.comparingLong(Punishment::getTime))
            .collect(Collectors.toCollection(ArrayList::new)) // toList has no mutability guarantee
    );
  }

  @Override
  public void addPunishment(@NonNull Punishment punishment) {
    jdbi.useHandle(handle -> handle.execute(
        SqlQueries.CREATE_PUNISHMENT.getQuery(),
        punishment.getPunishmentType().getId(),
        punishment.getTarget(),
        punishment.getPunisher(),
        punishment.getReason(),
        punishment.isLifted(),
        punishment.getLiftedBy(),
        punishment.getTime(),
        punishment.getDuration(),
        punishment.isSilent()
    ));
  }
}
