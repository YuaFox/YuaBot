package dev.yuafox.yuabot;

import dev.yuafox.yuabot.plugins.base.BasePlugin;
import dev.yuafox.yuabot.utils.Https;

import java.util.*;

public class Main {

    public static void main(String[] args){
        YuaBot.LOGGER.info(Arrays.toString(args));
        Thread.setDefaultUncaughtExceptionHandler(((t, e) -> YuaBot.LOGGER.error("Unhandled error", e)));

        try {
            YuaBot.params = getParams(args);
            YuaBot.init();
            new BasePlugin().install();
            YuaBot.fireAction(args[0], args[1]);
        }catch (Exception e){
            YuaBot.LOGGER.error("Unhandled error", e);
        }
    }


    public static Map<String, List<String>> getParams(String[] args){
        final Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        for (final String a : args) {
            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return null;
                }
                options = new ArrayList<>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            }
        }

        return params;
    }
}