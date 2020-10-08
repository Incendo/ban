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

package com.proximyst.ban.utils;

import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
  void accept(final T input) throws E;

  default @NonNull Consumer<T> toConsumer() {
    return input -> {
      try {
        this.accept(input);
      } catch (final Exception ex) {
        ThrowableUtils.sneakyThrow(ex);
      }
    };
  }
}
