package com.commandercortex.badApple;

import com.commandercortex.badApple.Global.onChatEvent;
import com.commandercortex.badApple.Global.EssentialsCommandBridge;
import com.commandercortex.badApple.Local.Commands.BadAppleCommand;
import com.commandercortex.badApple.Local.Commands.BroadcastCommand;
import com.commandercortex.badApple.Local.Commands.CosmeticsCommand;
import com.commandercortex.badApple.Local.Commands.FlyCommand;
import com.commandercortex.badApple.Local.Cosmetics.CosmeticEffectManager;
import com.commandercortex.badApple.Local.Commands.GamemodeCommand;
import com.commandercortex.badApple.Local.Commands.GroupMenuCommand;
import com.commandercortex.badApple.Local.Commands.HelpCommand;
import com.commandercortex.badApple.Local.Commands.ManageCommand;
import com.commandercortex.badApple.Local.Commands.NicknameCommand;
import com.commandercortex.badApple.Local.Commands.PunishMenuCommand;
import com.commandercortex.badApple.Local.Commands.TpCommand;
import com.commandercortex.badApple.Local.Commands.VanishCommand;
import com.commandercortex.badApple.Local.Folders.SetupFolders;
import com.commandercortex.badApple.Local.Management.DeveloperDashboardServer;
import com.commandercortex.badApple.Local.Permissions.GroupConfig;
import com.commandercortex.badApple.Local.Players.Events.onPlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class: registers commands, events, ensures folders/config and starts supporting services.
 */
public final class BadApple extends JavaPlugin {
    SetupFolders setupFolders = new SetupFolders();
    private DeveloperDashboardServer developerDashboardServer;
    private long serverStartedAt;
    private boolean velocityProxy = false;

    public DeveloperDashboardServer getDeveloperDashboardServer() {
        return developerDashboardServer;
    }

    public long getServerStartedAt() {
        return serverStartedAt;
    }

    public boolean isVelocityProxy() {
        return velocityProxy;
    }

    @Override
    public void onEnable() {
        System.out.println("[BadApple] -> Starting plugin!");

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

        setupFolders.CreateMainFolder();
        setupFolders.CreateConfigFolder();
        setupFolders.CreatePlayersFolder();
        setupFolders.CreateGroupsFolder();
        setupFolders.CreateDefaultGroups();
        GroupConfig.ensureGroupsLoaded();
        GroupConfig.ensureTracksLoaded();

        // Developer dashboard disabled entirely due to compatibility issues with Paper on lobby server
        developerDashboardServer = null;
        getLogger().info("DeveloperDashboardServer disabled (entirely)");

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
        if (getCommand("manage") != null) {
            getCommand("manage").setExecutor(new ManageCommand());
            getCommand("manage").setTabCompleter(new ManageCommand());
        }
        if (getCommand("cosmetics") != null) {
            CosmeticsCommand cosmeticsCommand = new CosmeticsCommand();
            getCommand("cosmetics").setExecutor(cosmeticsCommand);
            getCommand("cosmetics").setTabCompleter(cosmeticsCommand);
            Bukkit.getPluginManager().registerEvents(cosmeticsCommand, this);
        }
        // Compass command opens the Server Selector GUI from any server (only if velocity detected)
        if (velocityProxy) {
            // Register BungeeCord outgoing channel to allow plugin-messaging server switches
            try {
                getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            } catch (Exception ignored) {}

            if (getCommand("compass") != null) {
                getCommand("compass").setExecutor(new com.commandercortex.badApple.Local.Commands.CompassCommand());
                getCommand("compass").setTabCompleter(new com.commandercortex.badApple.Local.Commands.CompassCommand());
            } else {
                // Attempt dynamic registration of /compass if not declared in plugin.yml
                try {
                    PluginCommand cmd = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class).newInstance("compass", this);
                    com.commandercortex.badApple.Local.Commands.CompassCommand executor = new com.commandercortex.badApple.Local.Commands.CompassCommand();
                    cmd.setExecutor(executor);
                    cmd.setTabCompleter(executor);

                    Object server = Bukkit.getServer();
                    java.lang.reflect.Method m = server.getClass().getMethod("getCommandMap");
                    Object commandMapObj = m.invoke(server);
                    if (commandMapObj instanceof CommandMap) {
                        CommandMap commandMap = (CommandMap) commandMapObj;
                        commandMap.register(getDescription().getName(), cmd);
                        getLogger().info("Dynamically registered /compass command.");
                    } else {
                        getLogger().warning("CommandMap not available; /compass not registered.");
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to dynamically register /compass: " + e.getMessage());
                }
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

        Events();
    }

    @Override
    public void onDisable() {
        if (developerDashboardServer != null) {
            developerDashboardServer.stop();
        }
    }

    public void Events(){
        Bukkit.getPluginManager().registerEvents(new onPlayerJoinEvent(), this);
        Bukkit.getPluginManager().registerEvents(new onChatEvent(), this);
        Bukkit.getPluginManager().registerEvents(new EssentialsCommandBridge(), this);
        Bukkit.getPluginManager().registerEvents(new BedrockLoginListener(this), this);
    }

    private void registerBadApplePermissions() {
        String[] perms = new String[]{
                "badapple.group",
                "badapple.manage",
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
                "badapple.*"
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
