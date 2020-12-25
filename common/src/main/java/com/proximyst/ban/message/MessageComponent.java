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

package com.proximyst.ban.message;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.proximyst.ban.factory.IBanExceptionalFutureLoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MessageComponent implements IMessageComponent {
  private final @Nullable String name;
  private final @NonNull CompletableFuture<@Nullable ?> future;

  private MessageComponent(final @Nullable String name,
      final @Nullable Object object,
      final @NonNull IBanExceptionalFutureLoggerFactory banExceptionalFutureLoggerFactory) {
    this.name = name;
    if (object instanceof CompletableFuture) {
      this.future = ((CompletableFuture<?>) object)
          .exceptionally(banExceptionalFutureLoggerFactory.createLogger(this.getClass()).cast());
    } else {
      this.future = CompletableFuture.completedFuture(object);
    }
  }

  @AssistedInject
  public MessageComponent(final @Assisted("name") @NonNull String name,
      final @Assisted("value") @Nullable String value,
      final @NonNull IBanExceptionalFutureLoggerFactory banExceptionalFutureLoggerFactory) {
    this(name, (Object) value, banExceptionalFutureLoggerFactory);
  }

  @AssistedInject
  public MessageComponent(final @Assisted @NonNull String name,
      final @Assisted @NonNull CompletableFuture<@Nullable String> value,
      final @NonNull IBanExceptionalFutureLoggerFactory banExceptionalFutureLoggerFactory) {
    this(name, (Object) value, banExceptionalFutureLoggerFactory);
  }

  @AssistedInject
  public MessageComponent(final @Assisted @NonNull Map<String, String> placeholders,
      final @NonNull IBanExceptionalFutureLoggerFactory banExceptionalFutureLoggerFactory) {
    this(null, placeholders, banExceptionalFutureLoggerFactory);
  }

  @AssistedInject
  public MessageComponent(final @Assisted @NonNull CompletableFuture<@NonNull Map<String, String>> placeholders,
      final @NonNull IBanExceptionalFutureLoggerFactory banExceptionalFutureLoggerFactory) {
    this(null, placeholders, banExceptionalFutureLoggerFactory);
  }

  @Override
  public @Nullable String name() {
    return this.name;
  }

  @Override
  public @NonNull CompletableFuture<@Nullable ?> await() {
    return this.future;
  }
}
