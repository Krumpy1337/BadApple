package com.commandercortex.badApple;

import com.commandercortex.badApple.Global.onChatEvent;
import com.commandercortex.badApple.Local.Commands.BadAppleCommand;
import com.commandercortex.badApple.Local.Commands.BroadcastCommand;
import com.commandercortex.badApple.Local.Commands.FlyCommand;
import com.commandercortex.badApple.Local.Commands.GamemodeCommand;
import com.commandercortex.badApple.Local.Commands.GroupMenuCommand;
import com.commandercortex.badApple.Local.Commands.HelpCommand;
import com.commandercortex.badApple.Local.Commands.NicknameCommand;
import com.commandercortex.badApple.Local.Commands.PunishMenuCommand;
import com.commandercortex.badApple.Local.Commands.VanishCommand;
import com.commandercortex.badApple.Local.Folders.SetupFolders;
import com.commandercortex.badApple.Local.Permissions.GroupConfig;
import com.commandercortex.badApple.Local.Players.Events.onPlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BadApple extends JavaPlugin {
    SetupFolders setupFolders = new SetupFolders();

    @Override
    public void onEnable() {
        System.out.println("[BadApple] -> Starting plugin!");

        saveDefaultConfig();
        reloadConfig();

        setupFolders.CreateMainFolder();
        setupFolders.CreatePlayersFolder();
        setupFolders.CreateGroupsFolder();
        setupFolders.CreateDefaultGroups();
        GroupConfig.ensureGroupsLoaded();

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

        Events();
    }

    @Override
    public void onDisable() {
    }

    public void Events(){
        Bukkit.getPluginManager().registerEvents(new onPlayerJoinEvent(), this);
        Bukkit.getPluginManager().registerEvents(new onChatEvent(), this);
    }
}
