package cad.cli;

import cad.core.Command;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CommandRegistry {
    private static final Map<String, CliHandler> handlers = new HashMap<>();
    private static final Map<String, String> aliases = new HashMap<>();

    public static void register(String name, CliHandler handler) {
        handlers.put(name.toLowerCase(), handler);
    }

    public static void registerAlias(String alias, String target) {
        aliases.put(alias.toLowerCase(), target.toLowerCase());
    }

    public static CliHandler getHandler(String name) {
        String lowerName = name.toLowerCase();
        if (handlers.containsKey(lowerName)) {
            return handlers.get(lowerName);
        }
        if (aliases.containsKey(lowerName)) {
            return handlers.get(aliases.get(lowerName));
        }
        return null;
    }

    public static Map<String, String> getHelpMap() {
        Map<String, String> help = new TreeMap<>();
        for (Map.Entry<String, CliHandler> entry : handlers.entrySet()) {
            help.put(entry.getKey(), entry.getValue().getUsage());
        }
        return help;
    }
}
