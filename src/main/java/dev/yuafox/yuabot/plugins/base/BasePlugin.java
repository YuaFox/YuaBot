package dev.yuafox.yuabot.plugins.base;

import dev.yuafox.yuabot.YuaBot;
import dev.yuafox.yuabot.plugins.ActionHandler;
import dev.yuafox.yuabot.plugins.Plugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Deprecated
public class BasePlugin extends Plugin {

    @ActionHandler(action="install")
    public void install(){
        try {
            PreparedStatement statement = null;

            statement = YuaBot.dbConnection.prepareStatement("CREATE TABLE IF NOT EXISTS textPattern (" +
                    "id SERIAL NOT NULL PRIMARY KEY," +
                    "text TEXT NOT NULL" +
                    ");");
            statement.execute();

            statement = YuaBot.dbConnection.prepareStatement("CREATE TABLE IF NOT EXISTS sourceType (" +
                    "name TEXT NOT NULL PRIMARY KEY," +
                    "tableSource TEXT NOT NULL," +
                    "textPattern INTEGER NOT NULL REFERENCES textPattern (id)" +
                    ");");
            statement.execute();

            statement = YuaBot.dbConnection.prepareStatement("CREATE TABLE IF NOT EXISTS source (" +
                    "id SERIAL NOT NULL PRIMARY KEY," +
                    "authorName TEXT NOT NULL," +
                    "sourceType TEXT NOT NULL REFERENCES sourceType (name)" +
                    ");");
            statement.execute();

            statement = YuaBot.dbConnection.prepareStatement("CREATE TABLE IF NOT EXISTS media (" +
                    "url TEXT NOT NULL PRIMARY KEY," +
                    "lastSent INTEGER NOT NULL," +
                    "source INTEGER NOT NULL REFERENCES source (id)" +
                    ");");
            statement.execute();

            statement = YuaBot.dbConnection.prepareStatement("CREATE TABLE IF NOT EXISTS post (" +
                    "id SERIAL NOT NULL PRIMARY KEY," +
                    "media TEXT NOT NULL REFERENCES media (url)" +
                    ");");
            statement.execute();

        }catch (SQLException exception){
            exception.printStackTrace();
        }
    }
}