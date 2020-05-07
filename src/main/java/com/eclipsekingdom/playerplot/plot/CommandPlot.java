package com.eclipsekingdom.playerplot.plot;

import com.eclipsekingdom.playerplot.PlayerPlot;
import com.eclipsekingdom.playerplot.data.PlotCache;
import com.eclipsekingdom.playerplot.data.UserCache;
import com.eclipsekingdom.playerplot.data.UserData;
import com.eclipsekingdom.playerplot.plot.validation.NameValidation;
import com.eclipsekingdom.playerplot.plot.validation.RegionValidation;
import com.eclipsekingdom.playerplot.sys.PluginBase;
import com.eclipsekingdom.playerplot.sys.PluginHelp;
import com.eclipsekingdom.playerplot.sys.Version;
import com.eclipsekingdom.playerplot.sys.config.PluginConfig;
import com.eclipsekingdom.playerplot.util.*;
import com.eclipsekingdom.playerplot.util.scanner.PlotScanner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.eclipsekingdom.playerplot.sys.lang.Message.*;

public class CommandPlot implements CommandExecutor {

    private boolean usingDynmap = PluginBase.isDynmapDetected();
    private Dynmap dynmap = PluginBase.getDynmap();


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {


        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID playerID = player.getUniqueId();
            if (UserCache.hasData(playerID)) {
                if (args.length == 0) {
                    PluginHelp.showPlots(player);
                } else {
                    switch (args[0].toLowerCase()) {
                        case "scan":
                            processScan(player);
                            break;
                        case "info":
                            processInfo(player);
                            break;
                        case "claim":
                            processClaim(player, args);
                            break;
                        case "free":
                            processFree(player);
                            break;
                        case "list":
                            processList(player, args);
                            break;
                        case "flist":
                            processFList(player, args);
                            break;
                        case "trust":
                            processTrust(player, args);
                            break;
                        case "untrust":
                            processUntrust(player, args);
                            break;
                        case "upgrade":
                            processUpgrade(player);
                            break;
                        case "downgrade":
                            processDowngrade(player);
                            break;
                        case "setcenter":
                            processSetCenter(player);
                            break;
                        case "rename":
                            processRename(player, args);
                            break;
                        default:
                            PluginHelp.showPlots(player);
                            break;
                    }
                }
            } else {
                PlotUtil.fetchUnloadedData(player);
            }
        }

        return false;
    }

    private void processScan(Player player) {
        Plot plot = PlotCache.getPlot(player.getLocation());
        if (plot != null) {
            if (Version.current.canPlayBorderSound()) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.77f);
            }
            player.sendMessage(ChatColor.LIGHT_PURPLE + "[PlayerPlot] " + ChatColor.DARK_PURPLE + SCANNER_PLOT_OVERVIEW.fromPlayerAndPlot(plot.getOwnerName(), plot.getName()));
            PlotScanner.showPlot(player, plot, 5);
        } else {
            player.sendMessage(ChatColor.DARK_PURPLE + "[PlayerPlot] " + ChatColor.RED + SCANNER_NO_PLOTS);
        }
    }

    private void processInfo(Player player) {
        new StandingAction(player, () -> {

            Plot plot = PlotCache.getPlot(player.getLocation());
            player.sendMessage(ChatColor.DARK_PURPLE + "- - - " + plot.getName() + " - - -");
            PlotPoint minCorner = plot.getMinCorner();
            PlotPoint maxCorner = plot.getMaxCorner();
            int length = maxCorner.getX() - minCorner.getX() + 1;
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_AREA.toString() + ": " + ChatColor.RESET + length + " x " + length + "");
            PlotPoint center = plot.getCenter();
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_CENTER.toString() + ": " + ChatColor.RESET + "x:" + center.getX() + " z:" + center.getZ());
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_MIN_CORNER.toString() + ": " + ChatColor.RESET + "x:" + minCorner.getX() + " z:" + minCorner.getZ());
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_MAX_CORNER.toString() + ": " + ChatColor.RESET + "x:" + maxCorner.getX() + " z:" + maxCorner.getZ());
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_WORLD.toString() + ": " + ChatColor.RESET + plot.getWorld().getName());
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_COMPONENTS.toString() + ": " + ChatColor.RESET + plot.getComponents());
            player.sendMessage(ChatColor.LIGHT_PURPLE + LABEL_FRIENDS.toString() + ":");
            player.sendMessage(PlotUtil.getFriendsAsString(plot));

        }).run();
    }

    private void processClaim(Player player, String[] args) {
        UUID playerID = player.getUniqueId();
        UserData userData = UserCache.getData(playerID);
        PermInfo permInfo = UserCache.getPerms(playerID);
        if (PlotCache.getPlayerPlotsUsed(playerID) < PluginConfig.getStartingPlotNum() + userData.getUnlockedPlots() + permInfo.getPlotBonus()) {
            int unitSideLength = PluginConfig.getPlotUnitSideLength();
            RegionValidation.Status regionStatus = RegionValidation.canPlotBeRegisteredAt(player.getLocation(), unitSideLength, null);
            if (regionStatus == RegionValidation.Status.VALID) {
                String plotName = args.length > 1 ? args[1] : PlotUtil.getDefaultName(playerID);
                NameValidation.Status nameStatus = NameValidation.clean(plotName, playerID);
                if (nameStatus == NameValidation.Status.VALID) {
                    Plot plot = new Plot(player, player.getLocation(), plotName, unitSideLength);
                    PlotCache.registerPlot(plot);
                    player.sendMessage(SUCCESS_PLOT_CLAIM.coloredFromPlot(plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                    PlotScanner.showPlot(player, plot, 7);
                    if (usingDynmap) dynmap.registerPlot(plot);
                    if (Version.current.canPlayBorderSound()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + nameStatus.getMessage());
                }
            } else {
                player.sendMessage(ChatColor.RED + regionStatus.getMessage());
            }
        } else {
            player.sendMessage(ChatColor.RED + WARN_PLOT_LIMIT.toString());
        }
    }

    private void processFree(Player player) {
        new StandingAction(player, () -> {
            Plot plot = PlotCache.getPlot(player.getLocation());
            PlotScanner.showPlot(player, plot, 1);
            PlotCache.removePlot(plot);
            if (usingDynmap) dynmap.deletePlot(plot);
            player.sendMessage(SUCCESS_PLOT_FREE.coloredFromPlot(plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
            if (Version.current.canPlayBorderSound()) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
            }

        }).run();
    }

    public void processList(Player player, String[] args) {
        List<String> items = new ArrayList<>();
        for (Plot plot : PlotCache.getPlayerPlots(player.getUniqueId())) {
            items.add(PlotUtil.getListString(plot));
        }
        UUID playerID = player.getUniqueId();
        UserData userData = UserCache.getData(playerID);
        PermInfo permInfo = UserCache.getPerms(playerID);
        int used = PlotCache.getPlayerPlotsUsed(playerID);
        int capacity = PluginConfig.getStartingPlotNum() + userData.getUnlockedPlots() + permInfo.getPlotBonus();
        String title = ChatColor.LIGHT_PURPLE + LABEL_PLOTS.toString() + " (" + used + "/" + capacity + ")";
        InfoList infoList = new InfoList(title, items, 7);
        int page = args.length > 0 ? Amount.parse(args[0]) : 1;
        infoList.displayTo(player, page);
    }

    public void processFList(Player player, String[] args) {
        List<String> items = new ArrayList<>();
        for (Plot plot : PlotCache.getFriendPlots(player)) {
            items.add(PlotUtil.getFListString(plot));
        }
        String title = ChatColor.LIGHT_PURPLE + LABEL_FRIEND_PLOTS.toString();
        InfoList infoList = new InfoList(title, items, 7);
        int page = args.length > 0 ? Amount.parse(args[0]) : 1;
        infoList.displayTo(player, page);
    }

    private void processTrust(Player player, String[] args) {
        new StandingAction(player, () -> {

            Plot plot = PlotCache.getPlot(player.getLocation());
            if (args.length > 1) {
                String targetName = args[1];
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    Friend friend = new Friend(target);
                    if (!plot.getFriends().contains(friend)) {
                        if (!plot.getOwnerID().equals(friend.getUuid())) {
                            plot.addFriend(friend);
                            PlotCache.registerFriendAdd(friend, plot);
                            PlotCache.reportPlotModification(plot);
                            player.sendMessage(SUCCESS_PLOT_TRUST.coloredFromPlayerAndPlot(target.getName(), plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                            target.sendMessage(SUCCESS_INVITED.coloredFromPlayerAndPlot(player.getName(), plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                        } else {
                            player.sendMessage(ChatColor.RED + WARN_ADD_SELF.toString());
                        }
                    } else {
                        player.sendMessage(WARN_ALREADY_FRIEND.coloredFromPlayerAndPlot(target.getName(), plot.getName(), ChatColor.RED, ChatColor.DARK_PURPLE));
                    }
                } else {
                    player.sendMessage(WARN_PLAYER_OFFLINE.coloredFromPlayer(targetName, ChatColor.RED, ChatColor.DARK_PURPLE));
                }
            } else {
                player.sendMessage(ChatColor.RED + MISC_FORMAT.fromFormat("/plot trust [" + ARG_PLAYER + "]"));
            }

        }).run();
    }

    private void processUntrust(Player player, String[] args) {
        new StandingAction(player, () -> {

            Plot plot = PlotCache.getPlot(player.getLocation());
            if (args.length > 1) {
                String friendName = args[1];
                if (plot.isFriend(friendName)) {
                    Friend friend = plot.getFriend(friendName);
                    plot.removeFriend(friendName);
                    PlotCache.reportPlotModification(plot);
                    PlotCache.registerFriendRemove(friend, plot);
                    player.sendMessage(SUCCESS_PLOT_UNTRUST.coloredFromPlayerAndPlot(friendName, plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                } else {
                    player.sendMessage(WARN_NOT_FRIEND.coloredFromPlayerAndPlot(friendName, plot.getName(), ChatColor.RED, ChatColor.DARK_PURPLE));
                }
            } else {
                player.sendMessage(ChatColor.RED + MISC_FORMAT.fromFormat("/plot untrust [" + ARG_PLAYER + "]"));
            }

        }).run();
    }

    private void processUpgrade(Player player) {
        new StandingAction(player, () -> {
            Plot plot = PlotCache.getPlot(player.getLocation());
            UUID playerID = player.getUniqueId();
            UserData userData = UserCache.getData(playerID);
            PermInfo permInfo = UserCache.getPerms(playerID);
            if (PlotCache.getPlayerPlotsUsed(playerID) < PluginConfig.getStartingPlotNum() + userData.getUnlockedPlots() + permInfo.getPlotBonus()) {
                int newSideLength = PlotUtil.getUpgradeLength(plot.getComponents());
                PlotPoint center = plot.getCenter();
                RegionValidation.Status regionStatus = RegionValidation.canPlotBeUpgradedAt(plot.getWorld(), center, newSideLength, plot.getID());
                if (regionStatus == RegionValidation.Status.VALID) {
                    PlotScanner.showPlot(player, plot, 1);
                    if (Version.current.canPlayBorderSound()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
                    }
                    PlotPoint newMin = center.getMinCorner(newSideLength);
                    PlotPoint newMax = center.getMaxCorner(newSideLength);
                    plot.setRegion(newMin, newMax);
                    plot.incrementComponents();
                    PlotCache.reportPlotModification(plot);

                    if (usingDynmap) dynmap.updateMarker(plot);

                    Bukkit.getScheduler().runTaskLater(PlayerPlot.getPlugin(), () -> {
                        PlotScanner.showPlot(player, plot, 7);
                        player.sendMessage(SUCCESS_PLOT_UPGRADE.coloredFromPlot(plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                    }, 23);

                } else {
                    player.sendMessage(ChatColor.RED + regionStatus.getMessage());
                }
            } else {
                player.sendMessage(ChatColor.RED + WARN_PLOT_LIMIT.toString());
            }

        }).run();
    }


    private void processDowngrade(Player player) {
        new StandingAction(player, () -> {

            Plot plot = PlotCache.getPlot(player.getLocation());
            PlotPoint center = plot.getCenter();
            int newSideLength = PlotUtil.getDowngradeLength(plot.getComponents());
            if (newSideLength >= PluginConfig.getPlotUnitSideLength()) {

                PlotScanner.showPlot(player, plot, 1);
                if (Version.current.canPlayBorderSound()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                }

                PlotPoint newMin = center.getMinCorner(newSideLength);
                PlotPoint newMax = center.getMaxCorner(newSideLength);
                plot.setRegion(newMin, newMax);
                plot.decrementComponents();
                PlotCache.reportPlotModification(plot);

                if (usingDynmap) dynmap.updateMarker(plot);

                Bukkit.getScheduler().scheduleSyncDelayedTask(PlayerPlot.getPlugin(), () -> {
                    PlotScanner.showPlot(player, plot, 7);
                    player.sendMessage(SUCCESS_PLOT_DOWNGRADE.coloredFromPlot(plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                }, 23);

            } else {
                player.sendMessage(WARN_NOT_DOWNGRADEABLE.coloredFromPlot(plot.getName(), ChatColor.RED, ChatColor.DARK_PURPLE));
            }

        }).run();
    }

    private void processSetCenter(Player player) {
        new StandingAction(player, () -> {
            Location location = player.getLocation();
            Plot plot = PlotCache.getPlot(location);
            RegionValidation.Status regionStatus = RegionValidation.canPlotBeRegisteredAt(player.getLocation(), plot.getSideLength(), plot.getID());
            if (regionStatus == RegionValidation.Status.VALID) {
                plot.setCenter(player.getLocation());
                PlotCache.reportPlotModification(plot);
                player.sendMessage(SUCCESS_PLOT_CENTER.coloredFromPlot(plot.getName(), ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                PlotScanner.showPlot(player, plot, 7);
                if (usingDynmap) dynmap.updatePlot(plot);
                if (Version.current.canPlayBorderSound())
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            } else {
                player.sendMessage(ChatColor.RED + regionStatus.getMessage());
            }
        }).run();
    }

    private void processRename(Player player, String[] args) {
        new StandingAction(player, () -> {

            if (args.length > 1) {
                String newName = args[1];
                NameValidation.Status nameStatus = NameValidation.clean(newName, player.getUniqueId());
                if (nameStatus == NameValidation.Status.VALID) {
                    Plot plot = PlotCache.getPlot(player.getLocation());
                    plot.setName(newName);
                    PlotCache.reportPlotModification(plot);
                    if (usingDynmap) dynmap.updatePlot(plot);
                    player.sendMessage(SUCCESS_PLOT_RENAME.coloredFromPlot(newName, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE));
                } else {
                    player.sendMessage(ChatColor.RED + nameStatus.getMessage());
                }
            } else {
                player.sendMessage(ChatColor.RED + MISC_FORMAT.fromFormat("/plot rename [" + ARG_NAME + "]"));
            }

        }).run();
    }

}
