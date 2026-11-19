package com.krumpy1337.badapple;

import com.krumpy1337.badapple.Global.ChatListener;
import com.krumpy1337.badapple.Global.EssentialsCommandBridge;
import com.krumpy1337.badapple.Local.Commands.BadAppleCommand;
import com.krumpy1337.badapple.Local.Commands.ApiCommand;
import com.krumpy1337.badapple.Local.Commands.BroadcastCommand;
import com.krumpy1337.badapple.Local.Commands.CosmeticsCommand;
import com.krumpy1337.badapple.Local.Commands.CompassCommand;
import com.krumpy1337.badapple.Local.Commands.FlyCommand;
import com.krumpy1337.badapple.Local.Cosmetics.CosmeticEffectManager;
import com.krumpy1337.badapple.Local.Commands.GamemodeCommand;
import com.krumpy1337.badapple.Local.Commands.GroupMenuCommand;
import com.krumpy1337.badapple.Local.Commands.HelpCommand;
import com.krumpy1337.badapple.Local.Commands.NicknameCommand;
import com.krumpy1337.badapple.Local.Commands.PunishMenuCommand;
import com.krumpy1337.badapple.Local.Commands.TpCommand;
import com.krumpy1337.badapple.Local.Commands.VanishCommand;
import com.krumpy1337.badapple.Local.Folders.SetupFolders;
import com.krumpy1337.badapple.Local.Management.ServerApi;
import com.krumpy1337.badapple.Local.Permissions.GroupConfig;
import com.krumpy1337.badapple.Local.Listeners.PlayerJoinListener;
import com.krumpy1337.badapple.Local.PreGeneration.ChunkMenuCommand;
import com.krumpy1337.badapple.Local.PreGeneration.ChunkPreGenerationManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class: registers commands, events, ensures folders/config and starts supporting services.
 */
public final class BadApple extends JavaPlugin {
    private SetupFolders setupFolders;
    private ChunkPreGenerationManager chunkPreGenerationManager;
    private ServerApi serverApi;
    private long serverStartedAt;
    private boolean velocityProxy = false;

    public long getServerStartedAt() {
        return serverStartedAt;
    }

    public boolean isVelocityProxy() {
        return velocityProxy;
    }

    @Override
    public void onEnable() {
        getLogger().info("Starting BadApple...");

        saveDefaultConfig();
        reloadConfig();
        serverStartedAt = System.currentTimeMillis();

        // Detect velocity proxy usage: explicit config override or heuristics
        String cfgProxy = getConfig().getString("proxy.type", "");
        velocityProxy = "velocity".equalsIgnoreCase(cfgProxy) ||
                (Bukkit.getPluginManager().getPlugin("Velocity") != null) ||
                (Bukkit.getPluginCommand("server") != null);
        if (velocityProxy) {
            getLogger().info("Proxy integration: velocity detected/enabled.");
        } else {
            getLogger().info("Proxy integration: velocity not detected. Server selector will be disabled unless you set proxy.type=velocity in config.");
        }

        setupFolders = new SetupFolders(this);
        setupFolders.CreateMainFolder();
        setupFolders.CreateConfigFolder();
        setupFolders.CreatePlayersFolder();
        setupFolders.CreateGroupsFolder();
        setupFolders.CreateDefaultGroups();
        chunkPreGenerationManager = new ChunkPreGenerationManager(this);
        chunkPreGenerationManager.resumePersistedTask();
        serverApi = new ServerApi(this);
        serverApi.start();
        GroupConfig.ensureGroupsLoaded();
        GroupConfig.ensureTracksLoaded();

        // Register BadApple permissions so they appear under the BadApple plugin folder in permission menus
        registerBadApplePermissions();

        if (getCommand("group") != null) {
            getCommand("group").setExecutor(new BadAppleCommand());
            getCommand("group").setTabCompleter(new BadAppleCommand());
        }
        if (getCommand("groupmenu") != null) {
            GroupMenuCommand groupMenuCommand = new GroupMenuCommand();
            getCommand("groupmenu").setExecutor(groupMenuCommand);
            getCommand("groupmenu").setTabCompleter(groupMenuCommand);
            Bukkit.getPluginManager().registerEvents(groupMenuCommand, this);
        }
        if (getCommand("punishmenu") != null) {
            PunishMenuCommand punishMenuCommand = new PunishMenuCommand();
            getCommand("punishmenu").setExecutor(punishMenuCommand);
            getCommand("punishmenu").setTabCompleter(punishMenuCommand);
            Bukkit.getPluginManager().registerEvents(punishMenuCommand, this);
        }
        if (getCommand("vanish") != null) {
            VanishCommand vanishCommand = new VanishCommand();
            getCommand("vanish").setExecutor(vanishCommand);
            getCommand("vanish").setTabCompleter(vanishCommand);
        }
        if (getCommand("broadcast") != null) {
            getCommand("broadcast").setExecutor(new BroadcastCommand());
            getCommand("broadcast").setTabCompleter(new BroadcastCommand());
        }
        if (getCommand("fly") != null) {
            getCommand("fly").setExecutor(new FlyCommand());
            getCommand("fly").setTabCompleter(new FlyCommand());
        }
        if (getCommand("gamemode") != null) {
            getCommand("gamemode").setExecutor(new GamemodeCommand());
            getCommand("gamemode").setTabCompleter(new GamemodeCommand());
        }
        if (getCommand("nickname") != null) {
            getCommand("nickname").setExecutor(new NicknameCommand());
            getCommand("nickname").setTabCompleter(new NicknameCommand());
        }
        if (getCommand("badapplehelp") != null) {
            getCommand("badapplehelp").setExecutor(new HelpCommand());
            getCommand("badapplehelp").setTabCompleter(new HelpCommand());
        }
        if (getCommand("chunkmenu") != null || getCommand("chunkpregeneration") != null) {
            ChunkMenuCommand chunkMenuCommand = new ChunkMenuCommand(this, chunkPreGenerationManager);
            if (getCommand("chunkmenu") != null) {
                getCommand("chunkmenu").setExecutor(chunkMenuCommand);
                getCommand("chunkmenu").setTabCompleter(chunkMenuCommand);
            }
            if (getCommand("chunkpregeneration") != null) {
                getCommand("chunkpregeneration").setExecutor(chunkMenuCommand);
                getCommand("chunkpregeneration").setTabCompleter(chunkMenuCommand);
            }
            Bukkit.getPluginManager().registerEvents(chunkMenuCommand, this);
        }
        if (getCommand("api") != null) {
            ApiCommand apiCommand = new ApiCommand(serverApi);
            getCommand("api").setExecutor(apiCommand);
            getCommand("api").setTabCompleter(apiCommand);
        }
        if (getCommand("cosmetics") != null) {
            CosmeticsCommand cosmeticsCommand = new CosmeticsCommand();
            getCommand("cosmetics").setExecutor(cosmeticsCommand);
            getCommand("cosmetics").setTabCompleter(cosmeticsCommand);
            Bukkit.getPluginManager().registerEvents(cosmeticsCommand, this);
        }
        if (velocityProxy) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            if (getCommand("compass") != null) {
                CompassCommand compassCommand = new CompassCommand();
                getCommand("compass").setExecutor(compassCommand);
                getCommand("compass").setTabCompleter(compassCommand);
            }
        } else {
            getLogger().info("/compass command not registered because velocity proxy is not detected.");
        }
        if (getCommand("tp") != null) {
            TpCommand tpCommand = new TpCommand();
            getCommand("tp").setExecutor(tpCommand);
            getCommand("tp").setTabCompleter(tpCommand);
            Bukkit.getPluginManager().registerEvents(tpCommand, this);
        }
        if (getCommand("tpa") != null) {
            TpCommand tpCommand = new TpCommand();
            getCommand("tpa").setExecutor(tpCommand);
            getCommand("tpa").setTabCompleter(tpCommand);
        }
        if (getCommand("tpaccept") != null) {
            TpCommand tpCommand = new TpCommand();
            getCommand("tpaccept").setExecutor(tpCommand);
            getCommand("tpaccept").setTabCompleter(tpCommand);
        }
        if (getCommand("tpdeny") != null) {
            TpCommand tpCommand = new TpCommand();
            getCommand("tpdeny").setExecutor(tpCommand);
            getCommand("tpdeny").setTabCompleter(tpCommand);
        }

        new CosmeticEffectManager(this).start();

        registerEvents();
    }

    @Override
    public void onDisable() {
        if (chunkPreGenerationManager != null) {
            chunkPreGenerationManager.shutdown();
        }
        if (serverApi != null) {
            serverApi.stop();
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new EssentialsCommandBridge(), this);
        Bukkit.getPluginManager().registerEvents(new BedrockLoginListener(this), this);
    }

    private void registerBadApplePermissions() {
        String[] perms = new String[]{
                "badapple.group",
                "badapple.punish",
                "badapple.vanish",
                "badapple.broadcast",
                "badapple.fly",
                "badapple.fly.other",
                "badapple.gamemode",
                "badapple.gamemode.other",
                "badapple.nickname",
                "badapple.nickname.other",
                "badapple.cosmetics",
                "badapple.tp",
                "badapple.punish",
                "badapple.bypass",
                "badapple.*",
                "badapple.chunkpregeneration",
                "badapple.api"
        };

        for (String p : perms) {
            if (p == null || p.trim().isEmpty()) continue;
            if (Bukkit.getPluginManager().getPermission(p) == null) {
                org.bukkit.permissions.Permission perm = new org.bukkit.permissions.Permission(p, org.bukkit.permissions.PermissionDefault.OP);
                Bukkit.getPluginManager().addPermission(perm);
            }
        }
    }
}
