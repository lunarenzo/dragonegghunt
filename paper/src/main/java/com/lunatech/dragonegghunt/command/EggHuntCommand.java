package com.lunatech.dragonegghunt.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static com.lunatech.dragonegghunt.command.CommandHandler.BASE_PERM;

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
            .withPermission(BASE_PERM)
            .withSubcommands(
                new CommandAPICommand("info")
                    .withHelp("View current holder state", "View current holder state")
                    .executes(this::executorInfo),
                new CommandAPICommand("setholder")
                    .withHelp("Set a player as the egg holder", "Set a player as the egg holder")
                    .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                    .executes(this::executorSetHolder),
                new CommandAPICommand("override")
                    .withHelp("Toggle bypass regional protections.", "Toggle bypass regional protections.")
                    .withArguments(new BooleanArgument("value"))
                    .executes(this::executorOverride),
                new CommandAPICommand("reset")
                    .withHelp("Reset the Dragon Egg state to UNHELD.", "Reset the Dragon Egg state to UNHELD.")
                    .executes(this::executorReset)
            )
            .executes(this::executorInfo);
    }

    private void executorInfo(CommandSender sender, CommandArguments args) {
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Held held) {
            String name = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
            sender.sendMessage(ColorParser.of("<gold>Dragon Egg is currently held by: <yellow><player>")
                .with("player", name != null ? name : "Unknown")
                .build());
        } else if (state instanceof EggState.Placed placed) {
            sender.sendMessage(ColorParser.of("<gold>Dragon Egg is placed at: <yellow><x>, <y>, <z> <gray>in <white><world>")
                .with("x", String.valueOf((int) placed.x()))
                .with("y", String.valueOf((int) placed.y()))
                .with("z", String.valueOf((int) placed.z()))
                .with("world", placed.worldName())
                .build());
        } else {
            sender.sendMessage(ColorParser.of("<red>The Dragon Egg is currently unheld.").build());
        }
    }

    private void executorSetHolder(CommandSender sender, CommandArguments args) {
        Player target = (Player) args.get("player");
        if (target == null) {
            sender.sendMessage(Translation.as("commands.egghunt.setholder.invalid-player"));
            return;
        }

        // Add tagged Alpha Egg to inventory
        ItemStack egg = new ItemStack(Material.DRAGON_EGG);
        org.bukkit.inventory.meta.ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            egg.setItemMeta(meta);
        }
        target.getInventory().addItem(egg);
        eggTrackerService.updateState(new EggState.Held(target.getUniqueId(), System.currentTimeMillis()));

        sender.sendMessage(
            ColorParser.of(Translation.of("commands.egghunt.setholder.success"))
                .with("player", target.getName())
                .build()
        );
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
}
