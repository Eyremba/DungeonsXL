/*
 * Copyright (C) 2012-2016 Frank Baumann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.dre2n.dungeonsxl.command;

import io.github.dre2n.commons.command.BRCommand;
import io.github.dre2n.commons.compatibility.CompatibilityHandler;
import io.github.dre2n.commons.util.FileUtil;
import io.github.dre2n.commons.util.messageutil.MessageUtil;
import io.github.dre2n.dungeonsxl.DungeonsXL;
import io.github.dre2n.dungeonsxl.config.DMessages;
import io.github.dre2n.dungeonsxl.player.DPermissions;
import io.github.dre2n.dungeonsxl.world.DEditWorld;
import io.github.dre2n.dungeonsxl.world.DResourceWorld;
import io.github.dre2n.dungeonsxl.world.DWorlds;
import java.io.File;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel Saukel
 */
public class DeleteCommand extends BRCommand {

    DungeonsXL plugin = DungeonsXL.getInstance();

    public DeleteCommand() {
        setCommand("delete");
        setMinArgs(1);
        setMaxArgs(2);
        setHelp(DMessages.HELP_CMD_DELETE.getMessage());
        setPermission(DPermissions.DELETE.getNode());
        setPlayerCommand(true);
        setConsoleCommand(true);
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        DWorlds dWorlds = plugin.getDWorlds();

        DResourceWorld resource = dWorlds.getResourceByName(args[1]);
        if (resource == null) {
            MessageUtil.sendMessage(sender, DMessages.ERROR_NO_SUCH_MAP.getMessage(args[1]));
            return;
        }

        if (args.length == 2 && CompatibilityHandler.getInstance().isSpigot() && sender instanceof Player) {
            ClickEvent onClickConfirm = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dungeonsxl delete " + args[1] + " true");
            TextComponent confirm = new TextComponent(ChatColor.GREEN + "[ YES ]");
            confirm.setClickEvent(onClickConfirm);

            ClickEvent onClickDeny = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dungeonsxl delete " + args[1] + " false");
            TextComponent deny = new TextComponent(ChatColor.DARK_RED + "[ NO ]");
            deny.setClickEvent(onClickDeny);

            MessageUtil.sendMessage(sender, DMessages.CMD_DELETE_BACKUPS.getMessage());
            ((Player) sender).spigot().sendMessage(confirm, new TextComponent(" "), deny);

            return;
        }

        for (DEditWorld editWorld : dWorlds.getEditWorlds()) {
            if (editWorld.getResource().equals(resource)) {
                editWorld.delete(false);
            }
        }
        dWorlds.removeResource(resource);
        FileUtil.removeDirectory(resource.getFolder());

        if (args[2].equalsIgnoreCase("true")) {
            for (File file : DungeonsXL.BACKUPS.listFiles()) {
                if (file.getName().startsWith(resource.getName() + "-")) {
                    FileUtil.removeDirectory(file);
                }
            }
        }

        MessageUtil.sendMessage(sender, DMessages.CMD_DELETE_SUCCESS.getMessage(args[1]));
    }

}
