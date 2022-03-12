package me.xpyex.plugin.flywithfood.sponge;

import java.util.concurrent.TimeUnit;
import me.xpyex.plugin.flywithfood.common.config.ConfigUtil;
import me.xpyex.plugin.flywithfood.common.implementations.flyenergy.energys.FoodEnergy;
import me.xpyex.plugin.flywithfood.common.types.FWFMsgType;
import me.xpyex.plugin.flywithfood.common.utils.NetWorkUtil;
import me.xpyex.plugin.flywithfood.sponge.commands.FlyCmd;
import me.xpyex.plugin.flywithfood.sponge.config.HandleConfig;
import me.xpyex.plugin.flywithfood.sponge.implementations.SpongeUser;
import me.xpyex.plugin.flywithfood.sponge.implementations.energys.SpongeExpLevel;
import me.xpyex.plugin.flywithfood.sponge.implementations.energys.SpongeExpPoint;
import me.xpyex.plugin.flywithfood.sponge.implementations.energys.SpongeFood;
import me.xpyex.plugin.flywithfood.sponge.implementations.energys.SpongeMoney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;

@Plugin(
        id = "flywithfood-sponge",
        name = "FlyWithFood",
        description = "FlyWithFood",
        authors = {
                "XPYEX"
        },
        version = "1.3.7"
)
public class FlyWithFood {
    public static FlyWithFood INSTANCE;
    public static Logger LOGGER;
    public static final EconomyService ECONOMY_SERVICE = Sponge.getServiceManager().provide(EconomyService.class).get();
    public static final Task.Builder SCHEDULER = Task.builder();

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        INSTANCE = this;
        LOGGER = LoggerFactory.getLogger("FlyWithFood");
        FlyCmd.registerCmd();

        {
            new SpongeExpPoint().register();
            new SpongeExpLevel().register();
            new SpongeFood().register();
            new SpongeMoney().register();
        }

        if (!HandleConfig.loadConfig()) {
            LOGGER.error("载入配置文件出错!插件加载已终止,请检查配置文件，如无法解决请查看后台报错并报告开发者. QQ:1723275529");
            LOGGER.error("若确认是由配置文件错误导致加载出错，可在修改完毕后使用 /fly reload 重载以恢复");
            return;
        }
        if (ConfigUtil.needUpdate()) {
            LOGGER.info("本次插件更新修改了配置文件格式，正在备份原文件并转化新文件");
            HandleConfig.updateConfigFile();
            if (!HandleConfig.reloadConfig()) {
                LOGGER.error("载入配置文件出错!插件加载已终止,请检查配置文件，如无法解决请查看后台报错并报告开发者. QQ:1723275529");
                LOGGER.error("若确认是由配置文件错误导致加载出错，可在修改完毕后使用 /fly reload 重载以恢复");
                return;
            }
        }

        HandleConfig.functionWL = HandleConfig.config.functionWL.get("Enable").getAsBoolean();
        HandleConfig.noCostWL = HandleConfig.config.noCostWL.get("Enable").getAsBoolean();
        NetWorkUtil.PLUGIN_VERSION = Sponge.getPluginManager().getPlugin("flywithfood-sponge").get().getVersion().get();

        startCheck();

        SCHEDULER.execute(() -> {
                    NetWorkUtil.newVer = NetWorkUtil.checkUpdate();
                    if (NetWorkUtil.newVer != null) {
                        LOGGER.info("你当前运行的版本为 v" + Sponge.getPluginManager().getPlugin("flywithfood-sponge").get().getVersion().get());
                        LOGGER.info("找到一个更新的版本: " + NetWorkUtil.newVer);
                        LOGGER.info("前往 https://gitee.com/xpyex/FlyWithFood/releases 下载");
                        LOGGER.info(" ");
                        LOGGER.info("You are running FlyWithFood v" + NetWorkUtil.PLUGIN_VERSION);
                        LOGGER.info("There is a newer version: " + NetWorkUtil.newVer);
                        LOGGER.info("Download it at: https://github.com/0XPYEX0/FlyWithFood/releases");
                    } else {
                        LOGGER.info("当前已是最新版本");
                        LOGGER.info("You are running the newest FlyWithFood");
                    }
                })
                .async()
                .submit(INSTANCE)
        ;

        LOGGER.info("成功加载配置文件");
        LOGGER.info("已成功加载!");
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        cancelTasks();
    }

    public static void cancelTasks() {
        for (Task task : Sponge.getScheduler().getScheduledTasks(INSTANCE)) {
            task.cancel();
        }
    }

    public static void startCheck() {
        SCHEDULER.execute(() -> {
                    for (Player player : Sponge.getServer().getOnlinePlayers()) {
                        SpongeUser user = new SpongeUser(player);
                        if (!user.needCheck()) {
                            continue;
                        }
                        if (user.getInfo().getEnergy() instanceof FoodEnergy) {
                            if (user.hasSaturationEff()) {
                                user.disableFly();
                                user.sendFWFMsg(FWFMsgType.HasEffect);
                                continue;
                            }
                        }
                        double cost = user.getInfo().getCost(); //每秒消耗的饱食度,20为满,奇数即半格
                        double disable = user.getInfo().getDisable(); //饱食度消耗至多少关闭飞行
                        double now = user.getNow().doubleValue();
                        user.cost((now - cost));
                        if ((now - cost) < disable) {
                            user.disableFly();
                            user.sendFWFMsg(FWFMsgType.CanNotFly);
                            user.protectFromFall();
                        }
                    }
                })
                .interval(1, TimeUnit.SECONDS)
                .delay(0, TimeUnit.SECONDS)
                .submit(INSTANCE);
    }
}

