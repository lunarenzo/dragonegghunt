package com.lunatech.dragonegghunt.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.threadutil.Scheduler;
import io.github.milkdrinkers.wordweaver.Translation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.lunatech.dragonegghunt.constant.Permissions;

import java.util.UUID;

/**
 * Command for managing the Dragon Egg Tracker gameplay and configuration.
 */
public class EggHuntCommand extends Command {

    private final DragonEggHunt plugin;
    private final EggTrackerService eggTrackerService;

    public EggHuntCommand(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
    }

    @Override
    public CommandAPICommand command() {
        return new CommandAPICommand("egghunt")
            .withHelp("Egg Hunt controls", "Egg Hunt controls")
            .withSubcommands(
                new CommandAPICommand("info")
                    .withHelp("View current holder state", "View current holder state")
                    .withPermission(Permissions.COMMAND_INFO)
                    .executes(this::executorInfo),
                new CommandAPICommand("setholder")
                    .withHelp("Set a player as the egg holder", "Set a player as the egg holder")
                    .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                    .withPermission(Permissions.COMMAND_SETHOLDER)
                    .executes(this::executorSetHolder),
                new CommandAPICommand("override")
                    .withHelp("Toggle bypass regional protections.", "Toggle bypass regional protections.")
                    .withArguments(new BooleanArgument("value"))
                    .withPermission(Permissions.COMMAND_OVERRIDE)
                    .executes(this::executorOverride),
                new CommandAPICommand("reset")
                    .withHelp("Reset the Dragon Egg state to UNHELD.", "Reset the Dragon Egg state to UNHELD.")
                    .withPermission(Permissions.COMMAND_RESET)
                    .executes(this::executorReset),
                new CommandAPICommand("reload")
                    .withHelp("Reload the plugin configuration and data safely.", "Reload the plugin configuration and data safely.")
                    .withPermission(Permissions.COMMAND_RELOAD)
                    .executes(this::executorReload)
                    .withSubcommands(
                        new CommandAPICommand("all")
                            .withHelp("Reload all components (config, translations, database, messaging).", "Reload all components.")
                            .withPermission(Permissions.COMMAND_RELOAD)
                            .executes(this::executorReload),
                        new CommandAPICommand("config")
                            .withHelp("Reload only the config file.", "Reload only config.")
                            .withPermission(Permissions.COMMAND_RELOAD)
                            .executes(this::executorReloadConfig),
                        new CommandAPICommand("lang")
                            .withHelp("Reload only translation files.", "Reload only translation.")
                            .withPermission(Permissions.COMMAND_RELOAD)
                            .executes(this::executorReloadLang),
                        new CommandAPICommand("database")
                            .withHelp("Reload database connections and messaging.", "Reload database.")
                            .withPermission(Permissions.COMMAND_RELOAD)
                            .executes(this::executorReloadDatabase)
                    ),
                new CommandAPICommand("dashboard")
                    .withHelp("Open the administrative dashboard GUI", "Open the administrative dashboard GUI")
                    .withPermission(Permissions.COMMAND_DASHBOARD)
                    .executesPlayer(this::executorDashboard)
            )
            .executes(this::executorInfo);
    }

    private void executorInfo(CommandSender sender, CommandArguments args) {
        if (!sender.hasPermission(Permissions.COMMAND_INFO)) {
            sender.sendMessage(ColorParser.of("<red>I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.").build());
            return;
        }
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Held held) {
            String name = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
            sender.sendMessage(ColorParser.of(Translation.of("commands.egghunt.info.held"))
                .with("player", name != null ? name : "Unknown")
                .build());
        } else if (state instanceof EggState.Placed placed) {
            sender.sendMessage(ColorParser.of(Translation.of("commands.egghunt.info.placed"))
                .with("x", String.valueOf((int) placed.x()))
                .with("y", String.valueOf((int) placed.y()))
                .with("z", String.valueOf((int) placed.z()))
                .with("world", com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, placed.worldName()))
                .build());
        } else {
            sender.sendMessage(Translation.as("commands.egghunt.info.unheld"));
        }
    }

    private void executorSetHolder(CommandSender sender, CommandArguments args) {
        Player target = (Player) args.get("player");
        if (target == null) {
            sender.sendMessage(Translation.as("commands.egghunt.setholder.invalid-player"));
            return;
        }

        if (hasAlphaEgg(target)) {
            sender.sendMessage(Translation.as("commands.egghunt.setholder.already-holder"));
            return;
        }

        // Add tagged Alpha Egg to inventory
        ItemStack egg = com.lunatech.dragonegghunt.utility.EggItemFactory.createAlphaEgg(plugin);
        target.getInventory().addItem(egg);
        eggTrackerService.updateState(new EggState.Held(target.getUniqueId(), System.currentTimeMillis()));

        sender.sendMessage(
            ColorParser.of(Translation.of("commands.egghunt.setholder.success"))
                .with("player", target.getName())
                .build()
        );
    }

    private boolean isAlphaEgg(ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER);
    }

    private boolean hasAlphaEgg(Player player) {
        // Check main inventory storage contents (slots 0-35)
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isAlphaEgg(item)) {
                return true;
            }
        }
        // Check offhand slot
        if (isAlphaEgg(player.getInventory().getItemInOffHand())) {
            return true;
        }
        // Check cursor item
        org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
        if (openInv != null && isAlphaEgg(openInv.getCursor())) {
            return true;
        }
        return false;
    }

    private void executorOverride(CommandSender sender, CommandArguments args) {
        Boolean value = (Boolean) args.get("value");
        if (value == null) {
            return;
        }

        eggTrackerService.setOverrideRegionProtection(value);
        sender.sendMessage(
            ColorParser.of(Translation.of("commands.egghunt.override.success"))
                .with("value", String.valueOf(value))
                .build()
        );
    }

    private void executorReset(CommandSender sender, CommandArguments args) {
        eggTrackerService.updateState(new EggState.Unheld());
        sender.sendMessage(Translation.as("commands.egghunt.reset.success"));
    }

    private void executorReload(CommandSender sender, CommandArguments args) {
        Scheduler.async(() -> {
            try {
                plugin.onReload();
                return true;
            } catch (Exception e) {
                plugin.getComponentLogger().error("Failed to reload DragonEggHunt:", e);
                return false;
            }
        }).sync(success -> {
            if (success) {
                sender.sendMessage(Translation.as("commands.egghunt.reload.success"));
            } else {
                sender.sendMessage(Translation.as("commands.egghunt.reload.failure"));
            }
        }).execute();
    }

    private void executorReloadConfig(CommandSender sender, CommandArguments args) {
        Scheduler.async(() -> {
            try {
                plugin.reloadConfigOnly();
                return true;
            } catch (Exception e) {
                plugin.getComponentLogger().error("Failed to reload DragonEggHunt config:", e);
                return false;
            }
        }).sync(success -> {
            if (success) {
                sender.sendMessage(Translation.as("commands.egghunt.reload.config-success"));
            } else {
                sender.sendMessage(Translation.as("commands.egghunt.reload.failure"));
            }
        }).execute();
    }

    private void executorReloadLang(CommandSender sender, CommandArguments args) {
        Scheduler.async(() -> {
            try {
                plugin.reloadLangOnly();
                return true;
            } catch (Exception e) {
                plugin.getComponentLogger().error("Failed to reload DragonEggHunt translations:", e);
                return false;
            }
        }).sync(success -> {
            if (success) {
                sender.sendMessage(Translation.as("commands.egghunt.reload.lang-success"));
            } else {
                sender.sendMessage(Translation.as("commands.egghunt.reload.failure"));
            }
        }).execute();
    }

    private void executorReloadDatabase(CommandSender sender, CommandArguments args) {
        Scheduler.async(() -> {
            try {
                plugin.reloadDatabaseOnly();
                return true;
            } catch (Exception e) {
                plugin.getComponentLogger().error("Failed to reload DragonEggHunt database/messaging:", e);
                return false;
            }
        }).sync(success -> {
            if (success) {
                sender.sendMessage(Translation.as("commands.egghunt.reload.database-success"));
            } else {
                sender.sendMessage(Translation.as("commands.egghunt.reload.failure"));
            }
        }).execute();
    }

    private void executorDashboard(Player player, CommandArguments args) {
        new com.lunatech.dragonegghunt.gui.AdminDashboard(plugin).open(player);
    }
}
