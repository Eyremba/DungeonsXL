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
package io.github.dre2n.dungeonsxl.listener;

import io.github.dre2n.commons.util.messageutil.MessageUtil;
import io.github.dre2n.dungeonsxl.DungeonsXL;
import io.github.dre2n.dungeonsxl.config.DMessages;
import io.github.dre2n.dungeonsxl.config.MainConfig;
import io.github.dre2n.dungeonsxl.event.dgroup.DGroupCreateEvent;
import io.github.dre2n.dungeonsxl.game.Game;
import io.github.dre2n.dungeonsxl.global.DPortal;
import io.github.dre2n.dungeonsxl.global.GameSign;
import io.github.dre2n.dungeonsxl.global.GlobalProtection;
import io.github.dre2n.dungeonsxl.global.GroupSign;
import io.github.dre2n.dungeonsxl.global.LeaveSign;
import io.github.dre2n.dungeonsxl.player.DEditPlayer;
import io.github.dre2n.dungeonsxl.player.DGamePlayer;
import io.github.dre2n.dungeonsxl.player.DGlobalPlayer;
import io.github.dre2n.dungeonsxl.player.DGroup;
import io.github.dre2n.dungeonsxl.player.DInstancePlayer;
import io.github.dre2n.dungeonsxl.player.DPermissions;
import io.github.dre2n.dungeonsxl.player.DPlayers;
import io.github.dre2n.dungeonsxl.reward.DLootInventory;
import io.github.dre2n.dungeonsxl.task.RespawnTask;
import io.github.dre2n.dungeonsxl.trigger.InteractTrigger;
import io.github.dre2n.dungeonsxl.trigger.UseItemTrigger;
import io.github.dre2n.dungeonsxl.world.DEditWorld;
import io.github.dre2n.dungeonsxl.world.DGameWorld;
import io.github.dre2n.dungeonsxl.world.block.LockedDoor;
import io.github.dre2n.dungeonsxl.world.block.RewardChest;
import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * @author Frank Baumann, Milan Albrecht, Daniel Saukel
 */
public class PlayerListener implements Listener {

    DungeonsXL plugin = DungeonsXL.getInstance();
    DPlayers dPlayers = plugin.getDPlayers();

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DGamePlayer dPlayer = DGamePlayer.getByPlayer(player);
        if (dPlayer == null) {
            return;
        }
        dPlayer.onDeath(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        DGlobalPlayer dGlobalPlayer = dPlayers.getByPlayer(player);
        Block clickedBlock = event.getClickedBlock();
        DGameWorld dGameWorld = DGameWorld.getByWorld(player.getWorld());

        if (dGlobalPlayer.isInBreakMode()) {
            return;
        }

        if (clickedBlock != null) {
            // Block Enderchests
            if (dGameWorld != null || DEditWorld.getByWorld(player.getWorld()) != null) {
                if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
                    if (clickedBlock.getType() == Material.ENDER_CHEST) {
                        if (!DPermissions.hasPermission(player, DPermissions.BYPASS)) {
                            MessageUtil.sendMessage(player, DMessages.ERROR_ENDERCHEST.getMessage());
                            event.setCancelled(true);
                        }

                    } else if (clickedBlock.getType() == Material.BED_BLOCK) {
                        if (!DPermissions.hasPermission(player, DPermissions.BYPASS)) {
                            MessageUtil.sendMessage(player, DMessages.ERROR_BED.getMessage());
                            event.setCancelled(true);
                        }
                    }
                }
            }

            // Block Dispensers
            if (dGameWorld != null) {
                if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
                    if (clickedBlock.getType() == Material.DISPENSER) {
                        if (!DPermissions.hasPermission(player, DPermissions.BYPASS)) {
                            MessageUtil.sendMessage(player, DMessages.ERROR_DISPENSER.getMessage());
                            event.setCancelled(true);
                        }
                    }
                }

                for (LockedDoor door : dGameWorld.getLockedDoors()) {
                    if (clickedBlock.equals(door.getBlock()) || clickedBlock.equals(door.getAttachedBlock())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // Check Portals
        if (event.getItem() != null) {
            ItemStack item = event.getItem();

            if (dGlobalPlayer.isCreatingPortal()) {
                if (item.getType() == Material.WOOD_SWORD) {
                    if (clickedBlock != null) {
                        for (GlobalProtection protection : plugin.getGlobalProtections().getProtections(DPortal.class)) {
                            DPortal dPortal = (DPortal) protection;
                            if (!dPortal.isActive()) {
                                if (dPortal == dGlobalPlayer.getPortal()) {
                                    if (dPortal.getBlock1() == null) {
                                        dPortal.setBlock1(event.getClickedBlock());
                                        MessageUtil.sendMessage(player, DMessages.PLAYER_PORTAL_PROGRESS.getMessage());

                                    } else if (dPortal.getBlock2() == null) {
                                        dPortal.setBlock2(event.getClickedBlock());
                                        dPortal.setActive(true);
                                        dPortal.create(dGlobalPlayer);
                                        MessageUtil.sendMessage(player, DMessages.PLAYER_PORTAL_CREATED.getMessage());
                                    }
                                    event.setCancelled(true);
                                }
                            }
                        }
                    }
                }
            }

            // Copy/Paste a Sign and Block-info
            if (DEditWorld.getByWorld(player.getWorld()) != null) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (item.getType() == Material.STICK) {
                        DEditPlayer dPlayer = DEditPlayer.getByPlayer(player);
                        if (dPlayer != null) {
                            dPlayer.poke(clickedBlock);
                            event.setCancelled(true);
                        }
                    }
                }
            }

            // Trigger UseItem Signs
            DGameWorld gameWorld = DGameWorld.getByWorld(player.getWorld());
            if (gameWorld != null) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                    String name = null;
                    if (item.hasItemMeta()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            name = item.getItemMeta().getDisplayName();

                        } else if (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.BOOK_AND_QUILL) {
                            if (item.getItemMeta() instanceof BookMeta) {
                                BookMeta meta = (BookMeta) item.getItemMeta();
                                if (meta.hasTitle()) {
                                    name = meta.getTitle();
                                }
                            }
                        }
                    }
                    if (name == null) {
                        name = item.getType().toString();
                    }
                    UseItemTrigger trigger = UseItemTrigger.getByName(name, gameWorld);
                    if (trigger != null) {
                        trigger.onTrigger(player);
                    }
                }
            }
        }

        // Check Signs
        if (clickedBlock != null) {

            if (clickedBlock.getType() == Material.WALL_SIGN || clickedBlock.getType() == Material.SIGN_POST) {
                // Check Group Signs
                if (GroupSign.playerInteract(event.getClickedBlock(), player)) {
                    event.setCancelled(true);
                }

                // Check Game Signs
                if (GameSign.playerInteract(event.getClickedBlock(), player)) {
                    event.setCancelled(true);
                }

                LeaveSign leaveSign = LeaveSign.getByBlock(clickedBlock);
                if (leaveSign != null) {
                    leaveSign.onPlayerInteract(player);
                    event.setCancelled(true);
                }

                DGamePlayer dPlayer = DGamePlayer.getByPlayer(player);
                if (dPlayer != null) {

                    // Check DGameWorld Signs
                    DGameWorld gameWorld = DGameWorld.getByWorld(player.getWorld());
                    if (gameWorld != null) {

                        // Trigger InteractTrigger
                        InteractTrigger trigger = InteractTrigger.getByBlock(clickedBlock, gameWorld);
                        if (trigger != null) {
                            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                                trigger.onTrigger(player);
                            }
                        }

                        // Class Signs
                        for (Sign classSign : gameWorld.getClassesSigns()) {
                            if (classSign != null) {
                                if (classSign.getLocation().distance(clickedBlock.getLocation()) < 1) {
                                    if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                                        dPlayer.setDClass(ChatColor.stripColor(classSign.getLine(1)));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        DGlobalPlayer dPlayer = dPlayers.getByPlayer(player);
        if (dPlayer == null) {
            return;
        }

        if (dPlayer instanceof DEditPlayer && !plugin.getMainConfig().getDropItems() && !DPermissions.hasPermission(player, DPermissions.INSECURE)) {
            event.setCancelled(true);
        }

        if (!(dPlayer instanceof DGamePlayer)) {
            return;
        }

        DGamePlayer gamePlayer = (DGamePlayer) dPlayer;

        DGroup dGroup = DGroup.getByPlayer(player);
        if (dGroup == null) {
            return;
        }

        if (!dGroup.isPlaying()) {
            event.setCancelled(true);
            return;
        }

        if (!gamePlayer.isReady()) {
            event.setCancelled(true);
            return;
        }

        Game game = Game.getByWorld(gamePlayer.getWorld());

        for (ItemStack item : game.getRules().getSecureObjects()) {
            if (event.getItemDrop().getItemStack().isSimilar(item)) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, DMessages.ERROR_DROP.getMessage());
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getDPlayers().getByPlayer(player).applyRespawnInventory();

        DGlobalPlayer dPlayer = DGamePlayer.getByPlayer(player);
        if (dPlayer == null) {
            return;
        }

        if (dPlayer instanceof DEditPlayer) {
            DEditWorld editWorld = DEditWorld.getByWorld(((DEditPlayer) dPlayer).getWorld());
            if (editWorld == null) {
                return;
            }

            if (editWorld.getLobbyLocation() == null) {
                event.setRespawnLocation(editWorld.getWorld().getSpawnLocation());

            } else {
                event.setRespawnLocation(editWorld.getLobbyLocation());
            }

        } else if (dPlayer instanceof DGamePlayer) {
            DGamePlayer gamePlayer = (DGamePlayer) dPlayer;

            DGameWorld gameWorld = DGameWorld.getByWorld(gamePlayer.getWorld());

            if (gameWorld == null) {
                return;
            }

            DGroup dGroup = DGroup.getByPlayer(dPlayer.getPlayer());

            Location respawn = gamePlayer.getCheckpoint();

            if (respawn == null) {
                respawn = dGroup.getGameWorld().getStartLocation(dGroup);
            }

            // Because some plugins set another respawn point, DXL teleports a few ticks later.
            new RespawnTask(player, respawn).runTaskLater(plugin, 10);

            // Don't forget Doge!
            if (gamePlayer.getServant() != null) {
                gamePlayer.getServant().teleport(respawn);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPortal(PlayerPortalEvent event) {
        Block block1 = event.getFrom().getBlock();
        Block block2 = block1.getRelative(BlockFace.WEST);
        Block block3 = block1.getRelative(BlockFace.NORTH);
        Block block4 = block1.getRelative(BlockFace.EAST);
        Block block5 = block1.getRelative(BlockFace.SOUTH);
        if (DPortal.getByBlock(block1) != null || DPortal.getByBlock(block2) != null || DPortal.getByBlock(block3) != null || DPortal.getByBlock(block4) != null || DPortal.getByBlock(block5) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        DGamePlayer dPlayer = DGamePlayer.getByPlayer(player);

        if (dPlayer == null) {
            return;
        }

        if (dPlayer.getWorld() == event.getTo().getWorld()) {
            return;
        }

        if (!DPermissions.hasPermission(player, DPermissions.BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        DGamePlayer dPlayer = DGamePlayer.getByPlayer(player);
        if (dPlayer == null) {
            return;
        }

        if (dPlayer.isInDungeonChat()) {
            dPlayer.sendMessage(player.getDisplayName() + ": " + event.getMessage());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        plugin.debug.start("PlayerListener#onQuit");
        Player player = event.getPlayer();
        DGlobalPlayer dPlayer = dPlayers.getByPlayer(player);
        DGroup dGroup = DGroup.getByPlayer(player);
        Game game = Game.getByWorld(player.getWorld());

        if (!(dPlayer instanceof DInstancePlayer)) {
            dPlayers.removePlayer(dPlayer);
            if (dGroup != null) {
                dGroup.removePlayer(player);
            }

        } else if (game != null) {
            int timeUntilKickOfflinePlayer = game.getRules().getTimeUntilKickOfflinePlayer();

            if (timeUntilKickOfflinePlayer == 0) {
                ((DGamePlayer) dPlayer).leave();

            } else if (timeUntilKickOfflinePlayer > 0) {
                dGroup.sendMessage(DMessages.PLAYER_OFFLINE.getMessage(dPlayer.getName(), String.valueOf(timeUntilKickOfflinePlayer)), player);
                ((DGamePlayer) dPlayer).setOfflineTime(System.currentTimeMillis() + timeUntilKickOfflinePlayer * 1000);

            } else {
                dGroup.sendMessage(DMessages.PLAYER_OFFLINE_NEVER.getMessage(dPlayer.getName()), player);
            }

        } else if (dPlayer instanceof DEditPlayer) {
            ((DEditPlayer) dPlayer).leave();
        }
        plugin.debug.end("PlayerListener#onQuit", true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        plugin.debug.start("PlayerListener#onJoin");
        Player player = event.getPlayer();

        new DGlobalPlayer(player);

        // Check dPlayers
        DGamePlayer dPlayer = DGamePlayer.getByName(player.getName());
        if (dPlayer != null) {
            DGroup dGroup = DGroup.getByPlayer(dPlayer.getPlayer());
            if (dGroup != null) {
                dGroup.removePlayer(dPlayer.getPlayer());
                dGroup.addPlayer(player);
            }
            dPlayer.setPlayer(player);

            // Check offlineTime
            dPlayer.setOfflineTime(0);
        }

        // Tutorial Mode
        if (!plugin.getMainConfig().isTutorialActivated()) {
            plugin.debug.end("PlayerListener#onJoin", true);
            return;
        }

        if (DGamePlayer.getByPlayer(player) != null) {
            plugin.debug.end("PlayerListener#onJoin", true);
            return;
        }

        if (plugin.getPermissionProvider() == null || !plugin.getPermissionProvider().hasGroupSupport()) {
            plugin.debug.end("PlayerListener#onJoin", true);
            return;
        }

        if ((plugin.getMainConfig().getTutorialDungeon() == null || plugin.getMainConfig().getTutorialStartGroup() == null || plugin.getMainConfig().getTutorialEndGroup() == null)) {
            plugin.debug.end("PlayerListener#onJoin", true);
            return;
        }

        for (String group : plugin.getPermissionProvider().getPlayerGroups(player)) {
            if (!plugin.getMainConfig().getTutorialStartGroup().equalsIgnoreCase(group)) {
                continue;
            }

            DGroup dGroup = new DGroup(player, plugin.getMainConfig().getTutorialDungeon(), false);

            DGroupCreateEvent createEvent = new DGroupCreateEvent(dGroup, player, DGroupCreateEvent.Cause.GROUP_SIGN);
            plugin.getServer().getPluginManager().callEvent(createEvent);

            if (createEvent.isCancelled()) {
                dGroup = null;
            }

            if (dGroup == null) {
                continue;
            }

            if (dGroup.getGameWorld() == null) {
                dGroup.setGameWorld(plugin.getDWorlds().getResourceByName(DGroup.getByPlayer(player).getMapName()).instantiateAsGameWorld());
                dGroup.getGameWorld().setTutorial(true);
            }

            if (dGroup.getGameWorld() == null) {
                MessageUtil.sendMessage(player, DMessages.ERROR_TUTORIAL_NOT_EXIST.getMessage());
                continue;
            }

            DGamePlayer.create(player, dGroup.getGameWorld());
            plugin.debug.end("PlayerListener#onJoin", true);
            return;
        }
        plugin.debug.end("PlayerListener#onJoin", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        MainConfig config = plugin.getMainConfig();

        if (!config.isTutorialActivated()) {
            return;
        }

        if (DGamePlayer.getByPlayer(player) != null) {
            return;
        }

        if (plugin.getPermissionProvider() == null || !plugin.getPermissionProvider().hasGroupSupport()) {
            return;
        }

        if ((config.getTutorialDungeon() == null || config.getTutorialStartGroup() == null || config.getTutorialEndGroup() == null)) {
            return;
        }

        for (String group : plugin.getPermissionProvider().getPlayerGroups(player)) {
            if (!config.getTutorialStartGroup().equalsIgnoreCase(group)) {
                continue;
            }

            if (plugin.getDWorlds().getGameWorlds().size() >= config.getMaxInstances()) {
                event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                event.setKickMessage(DMessages.ERROR_TOO_MANY_TUTORIALS.getMessage());
            }

            return;
        }
    }

    // Deny Player Cmds
    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (DPermissions.hasPermission(event.getPlayer(), DPermissions.BYPASS)) {
            return;
        }

        if (!(dPlayers.getByPlayer(event.getPlayer()) instanceof DInstancePlayer)) {
            return;
        }
        DInstancePlayer dPlayer = (DInstancePlayer) dPlayers.getByPlayer(event.getPlayer());

        String command = event.getMessage().toLowerCase();
        ArrayList<String> commandWhitelist = new ArrayList<>();

        Game game = Game.getByWorld(dPlayer.getWorld());

        if (dPlayer instanceof DEditPlayer) {
            if (DPermissions.hasPermission(event.getPlayer(), DPermissions.CMD_EDIT)) {
                return;

            } else {
                commandWhitelist.addAll(plugin.getMainConfig().getEditCommandWhitelist());
            }

        } else if (game != null) {
            if (game.getRules() != null) {
                commandWhitelist.addAll(game.getRules().getGameCommandWhitelist());
            }
        }

        commandWhitelist.add("dungeonsxl");
        commandWhitelist.add("dungeon");
        commandWhitelist.add("dxl");

        event.setCancelled(true);

        for (String whitelistEntry : commandWhitelist) {
            if (command.equals('/' + whitelistEntry.toLowerCase()) || command.startsWith('/' + whitelistEntry.toLowerCase() + ' ')) {
                event.setCancelled(false);
            }
        }

        if (event.isCancelled()) {
            MessageUtil.sendMessage(event.getPlayer(), DMessages.ERROR_CMD.getMessage());
        }
    }

    // Inventory Events
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        InventoryView inventory = event.getView();

        DGameWorld gameWorld = DGameWorld.getByWorld(event.getPlayer().getWorld());

        if (gameWorld == null) {
            return;
        }

        if (!(inventory.getTopInventory().getHolder() instanceof Chest)) {
            return;
        }

        Chest chest = (Chest) inventory.getTopInventory().getHolder();

        for (RewardChest rewardChest : gameWorld.getRewardChests()) {
            if (!rewardChest.getChest().equals(chest)) {
                continue;
            }

            rewardChest.onOpen((Player) event.getPlayer());
            event.setCancelled(true);
        }

        if (!plugin.getMainConfig().getOpenInventories() && !DPermissions.hasPermission(event.getPlayer(), DPermissions.INSECURE)) {
            World world = event.getPlayer().getWorld();
            if (event.getInventory().getType() != InventoryType.CREATIVE && DEditWorld.getByWorld(world) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        for (DLootInventory inventory : plugin.getDLootInventories()) {
            if (event.getView() != inventory.getInventoryView()) {
                continue;
            }

            if (System.currentTimeMillis() - inventory.getTime() <= 500) {
                continue;
            }

            for (ItemStack istack : inventory.getInventory().getContents()) {
                if (istack != null) {
                    player.getWorld().dropItem(player.getLocation(), istack);
                }
            }

            plugin.getDLootInventories().remove(inventory);
        }
    }

    // Player move
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DGameWorld gameWorld = DGameWorld.getByWorld(player.getWorld());
        DGamePlayer gamePlayer = DGamePlayer.getByPlayer(player);
        if (gameWorld != null && gamePlayer != null && gamePlayer.isStealing()) {
            DGroup group = gamePlayer.getDGroup();
            Location startLocation = gameWorld.getStartLocation(group);

            if (startLocation.distance(player.getLocation()) < 3) {
                gamePlayer.captureFlag();
            }
        }

        DLootInventory inventory = DLootInventory.getByPlayer(player);
        if (inventory != null && player.getLocation().getBlock().getRelative(0, 1, 0).getType() != Material.PORTAL && player.getLocation().getBlock().getRelative(0, -1, 0).getType() != Material.PORTAL
                && player.getLocation().getBlock().getRelative(1, 0, 0).getType() != Material.PORTAL && player.getLocation().getBlock().getRelative(-1, 0, 0).getType() != Material.PORTAL
                && player.getLocation().getBlock().getRelative(0, 0, 1).getType() != Material.PORTAL && player.getLocation().getBlock().getRelative(0, 0, -1).getType() != Material.PORTAL) {
            inventory.setInventoryView(player.openInventory(inventory.getInventory()));
            inventory.setTime(System.currentTimeMillis());
        }

        DPortal dPortal = DPortal.getByLocation(player.getEyeLocation());
        if (dPortal == null) {
            return;
        }

        Block blockFrom = event.getFrom().getBlock();
        Block blockTo = event.getTo().getBlock();
        if (blockFrom.equals(blockTo)) {
            return;
        }

        dPortal.teleport(player);
    }

}
