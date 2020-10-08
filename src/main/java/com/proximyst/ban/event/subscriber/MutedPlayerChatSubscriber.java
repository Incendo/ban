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

package com.proximyst.ban.event.subscriber;

import com.google.inject.Inject;
import com.proximyst.ban.BanPlugin;
import com.proximyst.ban.config.MessagesConfig;
import com.proximyst.ban.manager.PunishmentManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MutedPlayerChatSubscriber {
  private final @NonNull BanPlugin main;
  private final @NonNull PunishmentManager manager;
  private final @NonNull MessagesConfig messagesConfig;

  @Inject
  public MutedPlayerChatSubscriber(
      final @NonNull BanPlugin main,
      final @NonNull PunishmentManager manager,
      final @NonNull MessagesConfig messagesConfig
  ) {
    this.main = main;
    this.manager = manager;
    this.messagesConfig = messagesConfig;
  }

  @Subscribe
  public void onChat(final @NonNull PlayerChatEvent event) {
    this.manager.getActiveMute(event.getPlayer().getUniqueId())
        .join() // This *should* be fast, and only on one player's connection thread
        .ifPresent(mute -> {
          event.setResult(ChatResult.denied());
          this.main.getMessageManager().formatMessageWith(
              mute.getReason().isPresent()
                  ? this.messagesConfig.applications.muteReason
                  : this.messagesConfig.applications.muteReasonless,
              mute
          ).thenAccept(event.getPlayer()::sendMessage);
        });
  }
}
