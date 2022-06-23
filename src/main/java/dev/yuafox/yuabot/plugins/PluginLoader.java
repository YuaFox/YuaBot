package dev.yuafox.yuabot.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

public class PluginLoader {

    private Map<String, Plugin> plugins;

    public PluginLoader(){
        this.plugins = new HashMap<>();
    }

    public void loadPlugins(File folder) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.plugins.clear();

        for(File file : folder.listFiles()){
            JarFile jarFile = new JarFile(file);
            InputStream inputStream = jarFile.getInputStream(jarFile.getJarEntry("plugin.properties"));

            Properties properties = new Properties();
            properties.load(inputStream);
            String id = properties.getProperty("id");
            String mainClass = properties.getProperty("mainClass");

            URL[] urls = { new URL("jar:file:" + file.getAbsolutePath()+"!/") };
            URLClassLoader cl = URLClassLoader.newInstance(urls);
            Class<? extends Plugin> c = cl.loadClass(mainClass).asSubclass(Plugin.class);
            Plugin plugin = c.getDeclaredConstructor().newInstance();
            plugin.onLoad();
            this.plugins.put(id, plugin);
        }
    }
}
