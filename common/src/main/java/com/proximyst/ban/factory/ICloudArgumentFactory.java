//
// ban - A punishment suite for Velocity.
// Copyright (C) 2021 Mariell Hoversholm
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
import com.proximyst.ban.commands.cloud.BanIdentityArgument;
import com.proximyst.ban.model.BanIdentity;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface ICloudArgumentFactory {
  <I extends BanIdentity> @NonNull BanIdentityArgument<I> banIdentity(final @Assisted("name") @NonNull String name,
      final @Assisted("required") boolean required);

  <I extends BanIdentity> @NonNull BanIdentityArgument<I> banIdentity(final @Assisted("name") @NonNull String name,
      final @Assisted("required") boolean required,
      final @Assisted("online") boolean online);
}
