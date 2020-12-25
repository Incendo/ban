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

package com.proximyst.ban.factory;

import com.google.inject.assistedinject.Assisted;
import com.proximyst.ban.config.MessageKey;
import com.proximyst.ban.message.IMessageComponent;
import com.proximyst.ban.message.MessageComponent;
import com.proximyst.ban.message.PlaceholderMessage;
import com.proximyst.ban.message.StaticMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface IMessageFactory {
  @NonNull MessageComponent staticComponent(final @Assisted("name") @NonNull String name,
      final @Assisted("value") @Nullable String value);

  @NonNull MessageComponent awaitedComponent(final @Assisted @NonNull String name,
      final @Assisted @NonNull CompletableFuture<@Nullable String> future);

  @NonNull MessageComponent multiComponent(final @Assisted @NonNull Map<String, String> placeholders);

  @NonNull MessageComponent multiComponent(
      final @Assisted @NonNull CompletableFuture<@NonNull Map<String, String>> placeholders);

  @NonNull StaticMessage staticMessage(final @Assisted @NonNull MessageKey messageKey);

  @NonNull PlaceholderMessage placeholderMessage(final @Assisted @NonNull MessageKey messageKey,
      final @Assisted @NonNull IMessageComponent @NonNull ... messageComponents);
}
