package com.gmail.necnionch.myplugin.statbadge.bukkit.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"UnusedReturnValue", "ClassCanBeRecord", "unused"})
public class Command {

    private final String name;
    private final @Nullable String permissionBaseName;
    private boolean defaultCommand;
    private boolean playerOnly;
    private final List<Argument.Registered<?>> arguments = new ArrayList<>();
    private @Nullable ComponentLike messagePrefix;
    private PrefixFormatter messagePrefixFormatter = PrefixFormatter.ALL_LINES;

    protected final List<Command> childrenCommands = new ArrayList<>();
    protected final Map<String, Command> childrenCommandsKey = new HashMap<>();

    public Command(String name, @Nullable String permissionBaseName) {
        this.name = name;
        this.permissionBaseName = permissionBaseName;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getPermissionBaseName() {
        return permissionBaseName;
    }

    public String getExecuteName(Context context) {
        return Optional.ofNullable(context.getExecuteName()).orElse(name);
    }

    public List<Argument.Registered<?>> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public List<Command> getChildrenCommands() {
        return Collections.unmodifiableList(childrenCommands);
    }

    public Optional<Command> getChildCommand(String name) {
        return Optional.ofNullable(childrenCommandsKey.get(name));
    }

    public Optional<Command> getDefaultCommand(Context context) {
        return childrenCommands.stream()
                .filter(Command::isDefault)
                .filter(c -> !c.isPlayerOnly() || context.getSender() instanceof PlayerSender)
                .filter(context::testPermissionWith)
                .findFirst();
    }

    public void processCommand(Context context) {
        context.commands().add(this);

        if (!context.testPermission()) {
            context.send(getLang(context, LangKey.NO_PERMISSION));
            return;
        }

        if (isPlayerOnly() && !(context.getSender() instanceof PlayerSender)) {
            context.send(getLang(context, LangKey.PLAYER_ONLY));
            return;
        }

        if (processArguments(context)) {
            execute(context);
        }
    }

    public Context processCommand(Sender sender, String[] args, @Nullable String executeName) {
        Context context = new Context(sender, args, executeName, true);
        processCommand(context);
        return context;
    }

    protected boolean processArguments(Context context) {
        for (Argument.Registered<?> argument : arguments) {
            if (context.argsList().isEmpty()) {
                if (!childrenCommands.isEmpty()) {
                    showHelp(context);
                    return false;
                }
                break;
            }

            String input = context.nextArg();
            context.arguments().add(argument);
            try {
                context.processArgument(argument, input);
            } catch (InvalidArgumentError.InExecuting e) {
                throw new InvalidArgumentError(argument.getKey(), argument.getArgument(), e);
            }
        }
        return true;
    }

    protected void processChildCommand(String commandName, Context context) {
        Command command = getChildCommand(commandName).orElse(null);
        if (command != null) {
            command.processCommand(context);
        } else {
            processUnknownCommand(context, commandName);
        }
    }

    protected boolean processUnknownCommand(Context context, String commandName) {
        int pos = context.commands().indexOf(this) - 1;

        if (0 <= pos) {
            return context.commands().get(pos).processUnknownCommand(context, commandName);
        } else {
            throw new UnknownCommandError(commandName);
        }
    }

    protected boolean processError(Context context, Throwable error) {
        int pos = context.commands().indexOf(this) - 1;

        if (0 <= pos) {
            return context.commands().get(pos).processError(context, error);
        } else if (!showError(context, error)) {
            throw new RuntimeException("Unhandled command exception", error);
        }
        return true;
    }

    public final boolean showError(Context context, Throwable error) {
        if (error instanceof UnknownCommandError) {
            if (!context.isExecuted())
                return true;

            TextComponent.Builder b = Component.text()
                    .append(getLang(context, LangKey.COMMAND_UNKNOWN));

            context.send(b.append(context.getCommandLine())
                    .append(Component.text(" "))
                    .append(Component.text(((UnknownCommandError) error).getInputName(), NamedTextColor.RED, TextDecoration.UNDERLINED)));
            return true;

        } else if (error instanceof RequiredArgumentError) {
            if (!context.isExecuted())
                return true;

            TextComponent.Builder b = Component.text()
                    .append(getLang(context, LangKey.ARGUMENT_REQUIRED));

            RequiredArgumentError err = (RequiredArgumentError) error;
            Component commandLine = context.getCommandLine((ctx, c, t) -> t, (ctx, a, t) -> {
                if (Objects.equals(a.getKey(), err.getArgumentKey()) && a.getArgument().getClass().equals(err.getArgumentClass()))
                    t = t.color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED);
                return t;
            });

            context.send(b.append(commandLine).build());
            return true;

        } else if (error instanceof InvalidArgumentError) {
            if (!context.isExecuted())
                return true;

            InvalidArgumentError err = (InvalidArgumentError) error;
            ComponentLike errorMessage = getInvalidArgumentErrorMessage(context, err);

            if (errorMessage != null) {
                context.send(errorMessage);
            } else {
                TextComponent.Builder b = Component.text();
                b.append(getLang(context, LangKey.ARGUMENT_INVALID));
                b.append(context.getCommandLine((ctx, c, t) -> t, (ctx, a, t) -> {
                    if (Objects.equals(a.getKey(), err.getArgumentKey()) && a.getArgument().equals(err.getArgument()))
                        t = t.color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED);
                    return t;
                }));
                context.send(b);
            }
            return true;
        }

        return false;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    protected ComponentLike getInvalidArgumentErrorMessage(Context context, InvalidArgumentError error) {
        if (error.getCause() instanceof BoolArg.Invalid && error.getArgument() instanceof BoolArg) {
            BoolArg boolArg = (BoolArg) error.getArgument();
            return getLang(context, LangKey.ARGUMENT_BOOL_INVALID)
                    .asComponent()
                    .replaceText(b -> b.matchLiteral("{true}").replacement(boolArg.getAccepts().get(0)))
                    .replaceText(b -> b.matchLiteral("{false}").replacement(boolArg.getRejects().get(0)));
        } else if (error.getCause() instanceof IntArg.NotIntError) {
            TextComponent.Builder b = Component.text();
            b.append(getLang(context, LangKey.ARGUMENT_INT_INVALID));
            b.append(context.getCommandLine((ctx, c, t) -> t, (ctx, a, t) -> {
                if (Objects.equals(a.getKey(), error.getArgumentKey()) && a.getArgument().equals(error.getArgument()))
                    t = t.color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED);
                return t;
            }));
            return b;
        } else if (error.getCause() instanceof IntArg.OutOfRangeError && error.getArgument() instanceof IntArg) {
            IntArg intArg = (IntArg) error.getArgument();
            LangKey key = intArg.getMin() != null ? LangKey.ARGUMENT_INT_OUT_OF_RANGE_MIN : LangKey.ARGUMENT_INT_INVALID;
            if (intArg.getMax() != null)
                key = intArg.getMin() != null ? LangKey.ARGUMENT_INT_OUT_OF_RANGE_MIN_MAX : LangKey.ARGUMENT_INT_OUT_OF_RANGE_MAX;

            return getLang(context, key)
                    .asComponent()
                    .replaceText(b -> b.matchLiteral("{min}").replacement(intArg.getMin() != null ? String.valueOf(intArg.getMin()) : "?"))
                    .replaceText(b -> b.matchLiteral("{max}").replacement(intArg.getMax() != null ? String.valueOf(intArg.getMax()) : "?"));
        }
        return getLang(context, LangKey.ARGUMENT_INVALID);
    }

    protected boolean processHelp(Context context) {
        int pos = context.commands().indexOf(this) - 1;

        if (0 <= pos) {
            return context.commands().get(pos).processHelp(context);
        } else if (!showHelp(context)) {
            throw new RuntimeException("Unhandled help exception");
        }
        return true;
    }

    public final boolean showHelp(Context context) {
        TextComponent.Builder b = Component.text();

        boolean commandLineSuggestCompleted = false;
        List<String> commandLineSuggest = new ArrayList<>();

        for (int i = 0; i < context.commands().size(); i++) {
            Command command = context.commands().get(i);
            String name;
            if (i == 0) {
                name = command.getExecuteName(context);
                b.append(Component.text("/", NamedTextColor.GRAY));
            } else {
                name = command.getName();
                b.append(Component.text(" "));
            }
            b.append(Component.text(name, NamedTextColor.GRAY));
            commandLineSuggest.add(name);

            for (Argument.Registered<?> argument : command.arguments) {
                String input = context.getArgumentInputValue(argument);
                TextComponent text = Component.text(
                        input != null ? input : (argument.getKey() != null ? "(" + argument.getKey() + ")" : "(?)")
                );
                b.append(Component.text(" ")).append(text.color(NamedTextColor.WHITE));

                if (!commandLineSuggestCompleted && input != null) {
                    commandLineSuggest.add(input);
                } else {
                    commandLineSuggest.add("");
                    commandLineSuggestCompleted = true;
                }
            }
        }
        TextComponent prefix = b.build();

        b = Component.text().append(getLang(context, LangKey.COMMAND_LIST_HEADER));

        @SuppressWarnings("SimplifyStreamApiCallChains")
        List<Command> commands = context.getCurrentCommand().childrenCommands.stream()
                .filter(c -> !c.isPlayerOnly() || context.getSender() instanceof PlayerSender)
                .filter(context::testPermissionWith)
                .collect(Collectors.toList());

        if (commands.isEmpty()) {
            b.appendNewline();
            b.append(getLang(context, LangKey.COMMAND_LIST_EMPTY_COMMANDS));

        } else {
            for (Command command : commands) {
                TextComponent.Builder bCommand = Component.text()
                        .append(prefix)
                        .append(Component.text(" "));

                bCommand.append(CommandFormatter.formatDefault(context, command)).color(NamedTextColor.YELLOW);
                StringBuilder commandLineSuggestCommand = new StringBuilder(" ").append(command.getName());

                for (Argument.Registered<?> argument : command.arguments) {
                    bCommand.append(Component.text(" ")).append(ArgumentFormatter.formatDefault(context, argument));
                }

                if (!command.childrenCommands.isEmpty()) {
                    bCommand.append(Component.text(" [ ..]", NamedTextColor.GRAY));
                }

                if (!command.arguments.isEmpty() || !command.childrenCommands.isEmpty())
                    commandLineSuggestCommand.append(" ");

                b.appendNewline();
                String suggestCommand = commandLineSuggestCompleted ? String.join(" ", commandLineSuggest) : String.join(" ", commandLineSuggest) + commandLineSuggestCommand;
                b.append(bCommand.clickEvent(ClickEvent.suggestCommand("/" + suggestCommand)));
            }
        }

        context.send(b.build());
        return true;
    }


    protected void execute(Context context) {
        try {
            if (context.argsList().isEmpty()) {
                Command command = getDefaultCommand(context).orElse(null);
                if (command != null) {
                    command.processCommand(context);
                } else {
                    processHelp(context);
                }
            } else {
                String commandName = context.nextArgLowerCase();
                processChildCommand(commandName, context);
            }
        } catch (Throwable error) {
            processError(context, error);
        }
    }

    protected List<String> complete(Context context) {
        try {
            List<String> args = context.argsList();
            if (args.isEmpty())
                return Collections.emptyList();

            String input = context.nextArg();
            for (Argument.Registered<?> argument : arguments) {
                context.arguments().add(argument);
                if (args.isEmpty()) {
                    return argument.complete(context, input);
                } else {
                    try {
                        context.processArgument(argument, input);
                    } catch (CommandError | InvalidArgumentError.InExecuting e) {
                        return Collections.emptyList();
                    }
                }
                input = context.nextArg();
            }

            input = input.toLowerCase(Locale.ENGLISH);
            if (args.isEmpty())
                return childrenCommands
                        .stream()
                        .filter(c -> !c.isPlayerOnly() || context.getSender() instanceof PlayerSender)
                        .map(Command::getName)
                        .filter(startsLowerWith(input))
                        .collect(Collectors.toList());

            return getChildCommand(input)
                    .map(c -> c.processCommandComplete(context))
                    .orElseGet(Collections::emptyList);

        } catch (Throwable error) {
            processError(context, error);
        }
        return Collections.emptyList();
    }

    public List<String> processCommandComplete(Context context) {
        context.commands().add(this);

        if (!context.testPermission()) {
            return Collections.emptyList();
        }

        if (isPlayerOnly() && !(context.getSender() instanceof PlayerSender)) {
            return Collections.emptyList();
        }
        return complete(context);
    }

    public List<String> processCommandComplete(Sender sender, String[] args, @Nullable String executeName) {
        Context context = new Context(sender, args, executeName, false);
        return processCommandComplete(context);
    }


    public void addChild(Command command) {
        childrenCommands.add(command);
        childrenCommandsKey.put(command.getName().toLowerCase(Locale.ENGLISH), command);
    }

    public Command addChild(String name, Executor executor, Completer completer) {
        Command command = new Command(name, null) {
            @Override
            public void execute(Context context) {
                executor.execute(context);
            }

            @Override
            public List<String> complete(Context context) {
                if (completer != null)
                    return completer.complete(context, context.argsList());
                return super.complete(context);
            }
        };
        addChild(command);
        return command;
    }

    public Command addChild(String name, Executor executor) {
        return addChild(name, executor, null);
    }

    public Command addChild(String name, SenderExecutor<Sender> executor) {
        return addChild(name, (Executor) executor);
    }

    public Command addChildPlayer(String name, SenderExecutor<PlayerSender> executor) {
        return addChild(name, executor).playerOnly();
    }

    public Command addChild(String name, SimpleExecutor<Sender> command) {
        return addChild(name, (Executor) command);
    }

    public Command addChildPlayer(String name, SimpleExecutor<PlayerSender> executor) {
        return addChild(name, executor).playerOnly();
    }

    public void addChildApply(String name, Consumer<Command> apply) {
        Command command = new Command(name, null);
        apply.accept(command);
        addChild(command);
    }

    public Command addHelpCommand(String name) {
        Command command = new Command(name, null) {
            @Override
            protected void execute(Context context) {
                context.commands().remove(this);
                showHelp(context);
            }
        }.defaultCommand(true);
        addChild(command);
        return command;
    }

    public Command addHelpCommand() {
        return addHelpCommand("help");
    }


    public <T> Command argument(@Nullable String key, Argument<T> argument) {
        arguments.add(new Argument.Registered<>(argument, key));
        return this;
    }

    public <T> Command argument(Argument<T> argument) {
        arguments.add(new Argument.Registered<>(argument, null));
        return this;
    }

    public Command argumentBool(@Nullable String key, String accept, String reject) {
        return argument(key, new BoolArg(accept, reject));
    }

    public Command argumentBool(@Nullable String key) {
        return argumentBool(key, "true", "false");
    }

    public Command argumentBool(String accept, String reject) {
        return argumentBool(null, accept, reject);
    }

    public Command argumentBool() {
        return argumentBool(null);
    }

    public Command argumentInt(@Nullable String key, @Nullable Integer min, @Nullable Integer max) {
        return argument(key, new IntArg(min, max));
    }

    public Command argumentInt(@Nullable String key) {
        return argumentInt(key, null, null);
    }

    public Command argumentInt(@Nullable Integer min, @Nullable Integer max) {
        return argumentInt(null, min, max);
    }

    public Command argumentInt() {
        return argumentInt(null, null, null);
    }

    public Command argumentString(@Nullable String key) {
        return argument(key, new StringArg());
    }

    public Command argumentString() {
        return argumentString(null);
    }

    public Command defaultCommand() {
        defaultCommand = true;
        return this;
    }

    public Command defaultCommand(boolean defaults) {
        defaultCommand = defaults;
        return this;
    }

    public boolean isDefault() {
        return defaultCommand;
    }

    public Command playerOnly() {
        playerOnly = true;
        return this;
    }

    public Command playerOnly(boolean only) {
        playerOnly = only;
        return this;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public Command messagePrefix(@Nullable ComponentLike prefix, PrefixFormatter formatter) {
        this.messagePrefix = prefix;
        this.messagePrefixFormatter = formatter;
        return this;
    }

    public Command messagePrefix(@Nullable ComponentLike prefix, boolean allLines) {
        this.messagePrefix = prefix;
        this.messagePrefixFormatter = allLines ? PrefixFormatter.ALL_LINES : PrefixFormatter.FIRST_LINE;
        return this;
    }

    public Command messagePrefix(@Nullable ComponentLike prefix) {
        this.messagePrefix = prefix;
        return this;
    }

    public @Nullable ComponentLike messagePrefix() {
        return messagePrefix;
    }

    public PrefixFormatter messagePrefixFormatter() {
        return messagePrefixFormatter;
    }

    public ComponentLike getLang(Context context, LangKey key) {
        return key.getDefaultMessage();
    }

    // context

    public static class Context {

        private final Sender sender;
        private final List<Command> commands = new ArrayList<>();
        private final List<Argument.Registered<?>> arguments = new ArrayList<>();
        private final Map<Argument.Registered<?>, Object> argumentValues = new HashMap<>();
        private final Map<Argument.Registered<?>, String> argumentInputValues = new HashMap<>();
        private final String[] args;
        private final List<String> argsList;
        private final boolean executed;
        private final @Nullable String executeName;

        public Context(Sender sender, String[] args, @Nullable String executeName, boolean executed) {
            this.sender = sender;
            this.args = args;
            this.argsList = new ArrayList<>(Arrays.asList(args));
            this.executeName = executeName;
            this.executed = executed;
        }

        public Sender getSender() {
            return sender;
        }

        public @Nullable String getExecuteName() {
            return executeName;
        }

        public String nextArg() throws NoSuchElementException {
            if (argsList.isEmpty())
                throw new NoSuchElementException();
            return argsList.remove(0);
        }

        public String nextArgLowerCase() throws NoSuchElementException {
            return nextArg().toLowerCase(Locale.ENGLISH);
        }

        public String getArg(int index) throws IndexOutOfBoundsException {
            return argsList.get(index);
        }

        public String getArgLowerCase(int index) throws IndexOutOfBoundsException {
            return argsList.get(index).toLowerCase(Locale.ENGLISH);
        }

        public int getArgCount() {
            return argsList.size();
        }

        public String[] args() {
            return args;
        }

        public List<String> argsList() {
            return argsList;
        }

        public boolean isExecuted() {
            return executed;
        }

        public List<Command> commands() {
            return commands;
        }

        public Command getCurrentCommand() {
            return commands.get(commands.size() - 1);
        }

        public Optional<String> getCurrentPermission() {
            return Optional.ofNullable(commands.get(0).getPermissionBaseName())
                    .map(p -> "." + commands.stream().map(Command::getName).collect(Collectors.joining(".")));
        }

        public Optional<String> getCurrentPermissionWith(Command command) {
            return getCurrentPermission().map(p -> "." + command.getName());
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean testPermission() {
            return getCurrentPermission().map(sender::hasPermission).orElse(true);
        }

        public boolean testPermissionWith(Command command) {
            return getCurrentPermissionWith(command)
                    .map(sender::hasPermission)
                    .orElse(true);
        }

        public List<Argument.Registered<?>> arguments() {
            return arguments;
        }

        public <A> A processArgument(Argument.Registered<A> argument, String input) {
            argumentInputValues.put(argument, input);
            A value = argument.execute(this, input);
            argumentValues.put(argument, value);
            return value;
        }

        public @Nullable String getArgumentInputValue(Argument.Registered<?> argument) {
            return argumentInputValues.get(argument);
        }


        public void send(ComponentLike message) {
            for (int i = 0; i < commands.size(); i++) {
                Command command = commands.get(commands.size() - i - 1);
                ComponentLike prefix = command.messagePrefix();
                if (prefix == null)
                    continue;

                sender.send(command.messagePrefixFormatter().format(command.messagePrefix(), message));
                return;
            }

            sender.send(message);
        }

        public Object getSenderObject() {
            return sender.getInstance();
        }

        public Component getCommandLine(CommandFormatter commandFormatter, ArgumentFormatter argumentFormatter) {
            ComponentLike format;
            TextComponent.Builder b = Component.text();

            b.append(Component.text("/", NamedTextColor.GRAY));
            for (int i = 0; i < commands.size(); i++) {
                Command command = commands.get(i);
                format = CommandFormatter.formatDefault(this, command);
                format = commandFormatter.format(this, command, format.asComponent());

                if (format != null) {
                    if (i != 0)
                        b.append(Component.text(" "));
                    b.append(format);
                }

                for (Argument.Registered<?> argument : command.getArguments()) {
                    format = ArgumentFormatter.formatDefault(this, argument);
                    format = argumentFormatter.format(this, argument, format.asComponent());
                    if (format != null)
                        b.append(Component.text(" ").append(format));
                }
            }
            return b.build();
        }

        public Component getCommandLine() {
            return getCommandLine((ctx, c, t) -> t, (ctx, a, t) -> t);
        }


        public <A extends Argument<T>, T> Optional<Argument.Registered<A>> getArgument(@Nullable String key, A argument) {
            return arguments.stream()
                    .filter(a -> key == null || key.equals(a.getKey()))
                    .filter(a -> a.getArgument().equals(argument))
                    .findFirst()
                    .map(a -> {
                        //noinspection unchecked
                        return (Argument.Registered<A>) a;
                    });
        }

        public <A extends Argument<T>, T> Optional<Argument.Registered<A>> getArgument(@Nullable String key, Class<A> clazz) {
            return arguments.stream()
                    .filter(a -> key == null || key.equals(a.getKey()))
                    .filter(a -> a.getArgument().getClass().equals(clazz))
                    .findFirst()
                    .map(a -> {
                        //noinspection unchecked
                        return (Argument.Registered<A>) a;
                    });
        }

        public boolean isPresentValue(@Nullable String key, Argument<?> argument) {
            return argumentValues.entrySet().stream()
                    .filter(e -> key == null || key.equals(e.getKey().getKey()))
                    .anyMatch(e -> e.getKey().getArgument().equals(argument));
        }

        public <A extends Argument<T>, T> boolean isPresentValue(@Nullable String key, Class<A> clazz) {
            return argumentValues.entrySet().stream()
                    .filter(e -> key == null || key.equals(e.getKey().getKey()))
                    .anyMatch(e -> e.getKey().getArgument().getClass().equals(clazz));
        }

        public boolean isPresentOrNull(@Nullable String key, Argument<?> argument) {
            return getArgument(key, argument).isPresent();
        }

        public <A extends Argument<T>, T> boolean isPresentOrNull(@Nullable String key, Class<A> clazz) {
            return getArgument(key, clazz).isPresent();
        }

        public <A extends Argument<T>, T> Optional<T> getOptional(@Nullable String key, A argument) {
            return getArgument(key, argument)
                    .flatMap(a -> Optional.ofNullable(argumentValues.get(a)))
                    .map(v -> {
                        //noinspection unchecked
                        return (T) v;
                    });
        }

        public <A extends Argument<T>, T> T get(@Nullable String key, A argument) {
            Object v = argumentValues.entrySet().stream()
                    .filter(e -> key == null || key.equals(e.getKey().getKey()))
                    .filter(e -> e.getKey().getArgument().equals(argument))
                    .map(Map.Entry::getValue)
                    .findAny()
                    .orElseThrow(() -> new RequiredArgumentError(key, argument.getClass()));
            //noinspection unchecked
            T obj = (T) v;
            if (obj == null)
                throw new InvalidArgumentError(key, argument, null);
            return obj;
        }

        public <A extends Argument<T>, T> Optional<T> getOptional(@Nullable String key, Class<A> clazz) {
            return getArgument(key, clazz)
                    .flatMap(a -> Optional.ofNullable(argumentValues.get(a)))
                    .map(v -> {
                        //noinspection unchecked
                        return (T) v;
                    });
        }

        public <A extends Argument<T>, T> T get(@Nullable String key, Class<A> clazz) {
            Map.Entry<Argument.Registered<?>, Object> entry = argumentValues.entrySet().stream()
                    .filter(e -> key == null || key.equals(e.getKey().getKey()))
                    .filter(e -> e.getKey().getArgument().getClass().equals(clazz))
                    .findAny()
                    .orElseThrow(() -> new RequiredArgumentError(key, clazz));
            Object v = entry.getValue();
            //noinspection unchecked
            T obj = (T) v;
            if (obj == null)
                throw new InvalidArgumentError(key, entry.getKey().getArgument(), null);
            return obj;
        }

        public boolean isPresentValue(Argument<?> argument) {
            return getArgument(null, argument).isPresent();
        }

        public <A extends Argument<T>, T> boolean isPresentValue(Class<A> clazz) {
            return getArgument(null, clazz).isPresent();
        }

        public boolean isPresentOrNull(Argument<?> argument) {
            return isPresentOrNull(null, argument);
        }

        public <A extends Argument<T>, T> boolean isPresentOrNull(Class<A> clazz) {
            return isPresentOrNull(null, clazz);
        }

        public <A extends Argument<T>, T> Optional<T> getOptional(A argument) {
            return getOptional(null, argument);
        }

        public <A extends Argument<T>, T> Optional<T> getOptional(Class<A> clazz) {
            return getOptional(null, clazz);
        }

        public <A extends Argument<T>, T> T get(A argument) {
            return get(null, argument);
        }

        public <A extends Argument<T>, T> T get(Class<A> clazz) {
            return get(null, clazz);
        }
    }

    // sender

    public interface Sender {
        Object getInstance();
        Audience getAudience();
        default void send(ComponentLike message) {
            getAudience().sendMessage(message);
        }
        @Nullable Locale getLocale();

        boolean hasPermission(String permission);
    }

    public interface PlayerSender extends Sender {
    }

    // executor

    public interface Executor {
        void execute(Context context);
    }

    public interface SenderExecutor<S extends Sender> extends Executor {
        @Override
        default void execute(Context context) {
            //noinspection unchecked
            execute((S) context.getSender(), context);
        }

        void execute(S sender, Context context);
    }

    public interface SimpleExecutor<S extends Sender> extends Executor {
        @Override
        default void execute(Context context) {
            //noinspection unchecked
            execute((S) context.getSender(), context.argsList().toArray(new String[0]));
        }

        void execute(S sender, String[] args);
    }

    // completer

    public interface Completer {
        List<String> complete(Context context, List<String> args);
    }

    public interface SenderCompleter<S extends Sender> extends Completer {
        @Override
        default List<String> complete(Context context, List<String> args) {
            //noinspection unchecked
            return execute((S) context.getSender(), context);
        }

        List<String> execute(S sender, Context context);
    }

    public interface SimpleCompleter<S extends Sender> extends Completer {
        @Override
        default List<String> complete(Context context, List<String> args) {
            //noinspection unchecked
            return complete((S) context.getSender(), args);
        }

        List<String> complete(S sender, List<String> args);
    }

    // argument

    public static abstract class Argument<T> {
        public abstract T execute(Context context, String input);

        public Stream<String> completeEntries(Context context, String input) {
            return Stream.empty();
        }

        public List<String> complete(Context context, String input) {
            return completeEntries(context, input)
                    .filter(startsLowerWith(input.toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }


        public static class Registered<T> {

            private final Argument<T> argument;
            private final @Nullable String key;

            public Registered(Argument<T> argument, @Nullable String key) {
                this.argument = argument;
                this.key = key;
            }

            public Argument<T> getArgument() {
                return argument;
            }

            public @Nullable String getKey() {
                return key;
            }

            public T execute(Context context, String input) {
                return argument.execute(context, input);
            }

            public List<String> complete(Context context, String input) {
                return argument.complete(context, input);
            }
        }
    }

    public static class BoolArg extends Argument<Boolean> {

        private final List<String> accept;
        private final List<String> reject;
        private final Set<String> entries;

        public BoolArg(List<String> accept, List<String> reject) {
            this.accept = accept;
            this.reject = reject;
            this.entries = new HashSet<>(accept);
            entries.addAll(reject);
        }

        public BoolArg(String accept, String reject) {
            this(Collections.singletonList(accept), Collections.singletonList(reject));
        }

        public List<String> getAccepts() {
            return Collections.unmodifiableList(accept);
        }

        public List<String> getRejects() {
            return Collections.unmodifiableList(reject);
        }

        @Override
        public Boolean execute(Context context, String input) {
            input = input.toLowerCase(Locale.ENGLISH);
            if (!entries.contains(input))
                throw new Invalid(input);
            return accept.contains(input);
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            return entries.stream();
        }

        public static class Invalid extends InvalidArgumentError.InExecuting {

            private final @Nullable String input;

            public Invalid(@Nullable String input) {
                this.input = input;
            }

            public @Nullable String getInput() {
                return input;
            }
        }

    }

    public static class IntArg extends Argument<Integer> {

        private final @Nullable Integer min;
        private final @Nullable Integer max;

        public IntArg(@Nullable Integer min, @Nullable Integer max) {
            this.min = min;
            this.max = max;
        }

        public @Nullable Integer getMin() {
            return min;
        }

        public @Nullable Integer getMax() {
            return max;
        }

        @Override
        public Integer execute(Context context, String input) {
            int value;
            try {
                value = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new NotIntError();
            }
            if (min != null && value < min)
                throw new OutOfRangeError();
            if (max != null && max < value)
                throw new OutOfRangeError();
            return value;
        }

        public static class NotIntError extends InvalidArgumentError.InExecuting {}

        public static class OutOfRangeError extends InvalidArgumentError.InExecuting {}

    }

    public static class StringArg extends Argument<String> {

        @Override
        public String execute(Context context, String input) {
            return input;
        }

    }

    public static class EntriesArg<T> extends Argument<T> {

        private final Map<String, T> entries;
        private final Function<String, String> keyFormatter;

        public EntriesArg(Map<String, T> entries, Function<String, String> keyFormatter) {
            this.entries = entries.entrySet().stream()
                    .collect(Collectors.toMap(e -> keyFormatter.apply(e.getKey()), Map.Entry::getValue));
            this.keyFormatter = keyFormatter;
        }

        public EntriesArg(Map<String, T> entries) {
            this.entries = new HashMap<>(entries);
            this.keyFormatter = s -> s;
        }

        @Override
        public T execute(Context context, String input) {
            T entry = entries.get(keyFormatter.apply(input));
            if (entry == null)
                throw new InvalidArgumentError.InExecuting();
            return entry;
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            return entries.keySet().stream();
        }
    }

    public static <T> EntriesArg<T> createEntriesArgument(Map<String, T> entries, Function<String, String> keyFormatter) {
        return new EntriesArg<>(entries, keyFormatter);
    }

    public static <T> EntriesArg<T> createEntriesArgument(Collection<T> entries, Function<T, String> formatter) {
        return new EntriesArg<>(
                entries.stream().collect(Collectors.toMap(formatter, e -> e)),
                s -> s.toLowerCase(Locale.ENGLISH)
        );
    }

    public static <T extends Enum<T>> EntriesArg<T> createEntriesArgument(Class<T> enumType) {
        return createEntriesArgument(
                Arrays.asList(enumType.getEnumConstants()),
                s -> s.name().toLowerCase(Locale.ENGLISH)
        );
    }

    // error

    public static class CommandError extends Error {
        public CommandError(Throwable cause) { super(cause); }
    }

    public static class UnknownCommandError extends CommandError {

        private final String inputName;

        public UnknownCommandError(String inputName, @Nullable Throwable cause) {
            super(cause);
            this.inputName = inputName;
        }

        public UnknownCommandError(String inputName) {
            this(inputName, null);
        }

        public String getInputName() {
            return inputName;
        }
    }

    public static class RequiredArgumentError extends CommandError {
        private final @Nullable String argumentKey;
        private final Class<?> argumentClass;

        public RequiredArgumentError(@Nullable String argumentKey, Class<?> argumentClass) {
            super(null);
            this.argumentKey = argumentKey;
            this.argumentClass = argumentClass;
        }

        public @Nullable String getArgumentKey() {
            return argumentKey;
        }

        public Class<?> getArgumentClass() {
            return argumentClass;
        }
    }

    public static class InvalidArgumentError extends CommandError {
        private final @Nullable String argumentKey;
        private final Argument<?> argument;
        private final @Nullable InExecuting cause;

        public InvalidArgumentError(@Nullable String argumentKey, Argument<?> argument, @Nullable InExecuting cause) {
            super(cause);
            this.argumentKey = argumentKey;
            this.argument = argument;
            this.cause = cause;
        }

        public @Nullable String getArgumentKey() {
            return argumentKey;
        }

        public Argument<?> getArgument() {
            return argument;
        }

        @Override
        public @Nullable InExecuting getCause() {
            return cause;
        }

        public static class InExecuting extends Error {}
    }

    // util

    public static Predicate<String> startsLowerWith(String lowerInput) {
        return s -> s.toLowerCase(Locale.ENGLISH).startsWith(lowerInput);
    }

    public interface PrefixFormatter {
        ComponentLike format(ComponentLike prefix, ComponentLike content);

        PrefixFormatter ALL_LINES = (prefix, content) ->
                prefix.asComponent().append(content.asComponent().replaceText(b -> b.match("\n").replacement(Component.newline().append(prefix))));

        PrefixFormatter FIRST_LINE = (p, c) -> p.asComponent().append(c);
    }

    public interface CommandFormatter {
        @Nullable ComponentLike format(Context context, Command command, Component content);

        static Component formatDefault(Context context, Command command) {
            if (context.commands().get(0).equals(command)) {
                return Component.text(command.getExecuteName(context), NamedTextColor.GRAY);
            } else {
                return Component.text(command.getName(), NamedTextColor.GRAY);
            }
        }
    }

    public interface ArgumentFormatter {
        @Nullable ComponentLike format(Context context, Argument.Registered<?> argument, Component content);

        static Component formatDefault(Context context, Argument.Registered<?> argument) {
            String input = context.getArgumentInputValue(argument);
            if (input != null) {
                return Component.text(input, NamedTextColor.WHITE);
            } else if (argument.getKey() != null) {
                return Component.text("(" + argument.getKey() + ")", NamedTextColor.WHITE);
            } else {
                return Component.text("(?)", NamedTextColor.WHITE);
            }
        }
    }

    public interface AudienceResolver {
        Audience getAudience(Object sender);
    }

    public enum LangKey {
        NO_PERMISSION(Component.text("このコマンドを実行する権限がありません", NamedTextColor.RED)),
        PLAYER_ONLY(Component.text("このコマンドはプレイヤー専用です", NamedTextColor.RED)),
        COMMAND_UNKNOWN(Component.text("不明なコマンドです: ", NamedTextColor.RED)),
        COMMAND_LIST_HEADER(Component.text("コマンド一覧:", NamedTextColor.DARK_AQUA)),
        COMMAND_LIST_EMPTY_COMMANDS(Component.text("利用可能なサブコマンドはありません。:(", NamedTextColor.GRAY, TextDecoration.ITALIC)),
        ARGUMENT_REQUIRED(Component.text("引数が必要です: ", NamedTextColor.RED)),
        ARGUMENT_INVALID(Component.text("引数の値が無効です: ", NamedTextColor.RED)),
        ARGUMENT_BOOL_INVALID(Component.text()
                .append(Component.text("{true}").decorate(TextDecoration.ITALIC))
                .append(Component.text(" または "))
                .append(Component.text("{false}").decorate(TextDecoration.ITALIC))
                .append(Component.text(" の値が必要です"))
                .build().color(NamedTextColor.RED)),
        ARGUMENT_INT_INVALID(Component.text("数値が必要です: ", NamedTextColor.RED)),
        ARGUMENT_INT_OUT_OF_RANGE_MIN(Component.text()
                .append(Component.text("{min}").decorate(TextDecoration.ITALIC))
                .append(Component.text(" 以上の数値が必要です"))
                .build().color(NamedTextColor.RED)),
        ARGUMENT_INT_OUT_OF_RANGE_MAX(Component.text()
                .append(Component.text("{max}").decorate(TextDecoration.ITALIC))
                .append(Component.text(" までの数値が必要です"))
                .build().color(NamedTextColor.RED)),
        ARGUMENT_INT_OUT_OF_RANGE_MIN_MAX(Component.text()
                .append(Component.text("{min}").decorate(TextDecoration.ITALIC))
                .append(Component.text(" から "))
                .append(Component.text("{max}").decorate(TextDecoration.ITALIC))
                .append(Component.text(" までの数値が必要です"))
                .build().color(NamedTextColor.RED));

        private final Component defaultMessage;

        LangKey(Component defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public Component getDefaultMessage() {
            return defaultMessage;
        }
    }

}