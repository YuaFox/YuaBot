package dev.yuafox.yuabot.plugins;

import java.io.File;

public abstract class Plugin {

    public void onLoad(){}

    public File getBaseFolder(){
        return new File("data/" + this.getClass().getName());
    }

    @ActionHandler(action="help")
    public void showHelp() {
        System.out.println("Help menu");
    }
}
