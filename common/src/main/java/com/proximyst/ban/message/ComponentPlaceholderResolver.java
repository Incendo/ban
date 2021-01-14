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

package com.proximyst.ban.message;

import com.google.common.collect.Multimap;
import com.proximyst.moonshine.component.placeholder.IPlaceholderResolver;
import com.proximyst.moonshine.component.placeholder.PlaceholderContext;
import com.proximyst.moonshine.component.placeholder.ResolveResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComponentPlaceholderResolver<R> implements IPlaceholderResolver<R, Component> {
  @Override
  public ResolveResult resolve(final String placeholderName, final Component value, final PlaceholderContext<R> ctx,
      final Multimap<String, @Nullable Object> flags) {
    return ResolveResult.ok(placeholderName, MiniMessage.get().serialize(value));
  }
}
