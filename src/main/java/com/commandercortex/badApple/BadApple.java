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
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class: registers commands, events, ensures folders/config and starts supporting services.
 */
public final class BadApple extends JavaPlugin {
    SetupFolders setupFolders = new SetupFolders();
    private DeveloperDashboardServer developerDashboardServer;
    private long serverStartedAt;

    public DeveloperDashboardServer getDeveloperDashboardServer() {
        return developerDashboardServer;
    }

    public long getServerStartedAt() {
        return serverStartedAt;
    }

    @Override
    public void onEnable() {
        System.out.println("[BadApple] -> Starting plugin!");

        saveDefaultConfig();
        reloadConfig();
        serverStartedAt = System.currentTimeMillis();

        setupFolders.CreateMainFolder();
        setupFolders.CreateConfigFolder();
        setupFolders.CreatePlayersFolder();
        setupFolders.CreateGroupsFolder();
        setupFolders.CreateDefaultGroups();
        GroupConfig.ensureGroupsLoaded();
        GroupConfig.ensureTracksLoaded();

        developerDashboardServer = new DeveloperDashboardServer();
        developerDashboardServer.start();

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
