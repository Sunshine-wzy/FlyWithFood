package me.xpyex.plugin.flywithfood.bukkit.energies;

import me.xpyex.plugin.flywithfood.bukkit.FlyWithFoodBukkit;
import me.xpyex.plugin.flywithfood.common.flyenergy.energies.FoodEnergy;
import me.xpyex.plugin.flywithfood.common.implementation.FWFUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitFood extends FoodEnergy {
    @Override
    public void cost(@NotNull FWFUser user, @NotNull Number value) {
        if (value.intValue() == 0) {
            return;
        }
        Player target = user.getPlayer();
        Bukkit.getScheduler().runTask(FlyWithFoodBukkit.INSTANCE, () ->
                target.setFoodLevel(Math.max(target.getFoodLevel() - value.intValue(), 0))
        );
    }

    @Override
    public @NotNull Number getNow(@NotNull FWFUser user) {
        return user.<Player>getPlayer().getFoodLevel();
        //
    }
}
