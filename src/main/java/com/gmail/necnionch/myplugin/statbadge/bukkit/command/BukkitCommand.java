package com.gmail.necnionch.myplugin.statbadge.bukkit.command;

import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unused")
public class BukkitCommand implements TabExecutor {

    private static final String UNSUPPORTED_LIB_ERROR = "Please install the Paper 1.16.5 or later server or KyoriAdventureLib from https://github.com/Necnion8/KyoriAdventureLib";

    private final Command command;
    private final @NotNull Command.AudienceResolver audienceResolver;

    /**
     * {@link Command} を Bukkit コマンドに変換します
     * @param audienceResolver {@link Audience} の変換に使用するアダプタ
     */
    public BukkitCommand(Command command, @NotNull Command.AudienceResolver audienceResolver) {
        this.command = command;
        this.audienceResolver = audienceResolver;
    }

    public void register(JavaPlugin owner) {
        Objects.requireNonNull(owner.getCommand(command.getName()), "Undefined command: " + command.getName())
                .setExecutor(this);
    }

    public void unregister(JavaPlugin owner) {
        Optional.ofNullable(owner.getCommand(command.getName()))
                .ifPresent(cmd -> cmd.setExecutor(null));
    }

    /**
     * {@link Command} を Bukkit コマンドに変換します (for Paper 1.16.5 以降)<br>
     * Audienceをサポートしないサーバーで互換性を維持するには {@link BukkitCommand.Compat#register(Command)} を使用してください
     */
    @SuppressWarnings("UnusedReturnValue")
    public static BukkitCommand register(JavaPlugin owner, Command command) {
        BukkitCommand c = new BukkitCommand(command, createNativeAudience());
        c.register(owner);
        return c;
    }


    /**
     * {@link CommandSender}のコマンドLib用のクラスを作成します<br>
     * プレイヤーである場合は {@link BukkitCommand#createPlayerSender(Player)} を使用します
     */
    public Command.Sender createSender(CommandSender sender) {
        if (sender instanceof Player)
            return createPlayerSender((Player) sender);
        return new BukkitSender(sender, audienceResolver.getAudience(sender));
    }

    /**
     * {@link Player}のコマンドLib用のクラスを作成します
     */
    public Command.PlayerSender createPlayerSender(Player player) {
        return new BukkitPlayerSender(player, audienceResolver.getAudience(player));
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command c, @NotNull String label, @NotNull String[] args) {
        command.processCommand(createSender(sender), args, label);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command c, @NotNull String alias, @NotNull String[] args) {
        return command.processCommandComplete(createSender(sender), args, alias);
    }

    public static class BukkitSender implements Command.Sender {

        private final CommandSender sender;
        private final Audience audience;

        public BukkitSender(CommandSender sender, Audience audience) {
            this.sender = sender;
            this.audience = audience;
        }

        @Override
        public CommandSender getInstance() {
            return sender;
        }

        @Override
        public Audience getAudience() {
            return audience;
        }

        @Override
        public @Nullable Locale getLocale() {
            return null;
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }
    }

    public static class BukkitPlayerSender extends BukkitSender implements Command.PlayerSender {

        private final Player player;

        public BukkitPlayerSender(Player player, Audience audience) {
            super(player, audience);
            this.player = player;
        }

        @Override
        public Player getInstance() {
            return player;
        }
    }

    public static Compat compat(JavaPlugin plugin) {
        return new Compat(plugin);
    }

    /**
     * サーバーの Audience 互換性を解決してコマンドを登録するクラス
     */
    public static class Compat implements Command.AudienceResolver {

        private final JavaPlugin plugin;
        private final Map<String, BukkitCommand> commands = new HashMap<>();
        private @Nullable Command.AudienceResolver audienceResolver;
        private @Nullable Object bukkitAudiences;

        public Compat(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @SuppressWarnings("UnusedReturnValue")
        public BukkitCommand register(Command command) {
            BukkitCommand wrap = new BukkitCommand(command, this);
            wrap.register(plugin);
            commands.put(command.getName(), wrap);
            return wrap;
        }

        public Map<String, BukkitCommand> commands() {
            return commands;
        }

        public void init() {
            initAudiences();
        }

        public void close() {
            for (BukkitCommand command : commands.values()) {
                command.unregister(plugin);
            }
            commands.clear();
            closeAudiences();
        }

        protected void initAudiences() {
            if (audienceResolver != null)
                return;

            bukkitAudiences = null;
            try {
                // try native
                audienceResolver = createNativeAudience();
                return;
            } catch (RuntimeException ignored) {
            }

            // try bukkit audiences
            try {
                bukkitAudiences = createBukkitAudiences(plugin);
                audienceResolver = createBukkitAudiencesResolver(bukkitAudiences);

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(UNSUPPORTED_LIB_ERROR);
            }
        }

        protected void closeAudiences() {
            audienceResolver = null;
            if (bukkitAudiences != null) {
                try {
                    closeBukkitAudiences(bukkitAudiences);
                } catch (ReflectiveOperationException ignored) {
                } finally {
                    bukkitAudiences = null;
                }
            }
        }

        @Override
        public Audience getAudience(Object sender) {
            return Objects.requireNonNull(audienceResolver, "Audience resolver not initialized").getAudience(sender);
        }

    }

    /**
     * サーバーネイティブの Audience を使用して AudienceResolver を作成 (Paper 1.16.5 以降のみ)
     */
    public static Command.AudienceResolver createNativeAudience() {
        try {
            if (Audience.class.isAssignableFrom(CommandSender.class))
                return sender -> (Audience) sender;
        } catch (LinkageError ignored) {
        }
        throw new RuntimeException(UNSUPPORTED_LIB_ERROR);
    }

    /**
     * 外部ライブラリの BukkitAudience を使用して AudienceResolver を作成
     */
    public static Command.AudienceResolver createBukkitAudiencesResolver(Object bukkitAudiences) throws ReflectiveOperationException {
        Method senderMethod = Class.forName("net.kyori.adventure.platform.bukkit.BukkitAudiences").getMethod("sender", CommandSender.class);
        return sender -> {
            try {
                //noinspection JavaReflectionInvocation
                return (Audience) senderMethod.invoke(bukkitAudiences, sender);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to convert to audience", e);
            }
        };
    }

    public static Object createBukkitAudiences(Plugin plugin) throws ReflectiveOperationException {
        Class<?> bukkitAudiencesClass = Class.forName("net.kyori.adventure.platform.bukkit.BukkitAudiences");
        Method createMethod = bukkitAudiencesClass.getMethod("create", Plugin.class);
        return createMethod.invoke(null, plugin);
    }

    public static void closeBukkitAudiences(Object bukkitAudiences) throws ReflectiveOperationException {
        bukkitAudiences.getClass().getMethod("close").invoke(bukkitAudiences);
    }

}