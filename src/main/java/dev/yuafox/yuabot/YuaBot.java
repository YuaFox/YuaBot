package dev.yuafox.yuabot;

import dev.yuafox.yuabot.data.Data;
import dev.yuafox.yuabot.data.Media;
import dev.yuafox.yuabot.data.Source;
import dev.yuafox.yuabot.plugins.ActionHandler;
import dev.yuafox.yuabot.plugins.DataController;
import dev.yuafox.yuabot.plugins.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class YuaBot {

    public static File pluginFolder = new File("plugins");
    public static File dataFolder = new File("data");

    public static Map<String, List<String>> params;
    public static Connection dbConnection;

    private static final PluginLoader pluginLoader = new PluginLoader();
    private static final Map<String, Object> actionHandlerList = new HashMap<>();

    public static final Logger LOGGER = LoggerFactory.getLogger(YuaBot.class);

    public static void init() throws IOException, SQLException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        boolean setup = true;
        if(!YuaBot.pluginFolder.exists())
            setup = YuaBot.pluginFolder.mkdir();
        if(!YuaBot.dataFolder.exists())
            setup &= YuaBot.dataFolder.mkdir();
        File propertiesFile = new File(dataFolder, "bot.properties");
        Properties properties = new Properties();
        if(!propertiesFile.exists()){
            setup &= propertiesFile.createNewFile();
            properties.setProperty("database", "jdbc:postgresql://localhost/DATABASE?user=USER&password=PASSWORD");
            properties.store(new FileOutputStream(propertiesFile), null);
            if(setup)
                YuaBot.LOGGER.info("Check data/bot.properties file to finish setup.");
            else
                YuaBot.LOGGER.error("Error creating initial configuration.");
            return;
        }
        properties.load(new FileInputStream(propertiesFile));
        YuaBot.dbConnection = DriverManager.getConnection(properties.getProperty("database", ""));
        YuaBot.loadPlugins();
    }

    private static void loadPlugins() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        pluginLoader.loadPlugins(pluginFolder);
    }

    public static void registerActionHandler(String id, Object object){
        actionHandlerList.put(id, object);
    }

    public static void fireAction(String actionHandlerId, String actionId) throws InvocationTargetException, IllegalAccessException {
        if(!actionHandlerList.containsKey(actionHandlerId)){
            YuaBot.LOGGER.error("Action handler {} does not exist.", actionHandlerId);
            return;
        }
        Object actionHandler = actionHandlerList.get(actionHandlerId);
        for (Method m : actionHandler.getClass().getMethods()) {
            ActionHandler annotation = m.getAnnotation(ActionHandler.class);
            if(annotation != null && annotation.action().equals(actionId)){
                m.invoke(actionHandler);
            }
        }
    }

    public static void installMediaSource(DataController controller, Class<? extends Source> mediaClazz) throws SQLException {
        String tablename = controller.getSourceName()+"source";

        StringBuilder tableStatement = new StringBuilder("CREATE TABLE "+tablename+" (");
        tableStatement.append("id INTEGER NOT NULL PRIMARY KEY REFERENCES source (id),");
        for(Field field : mediaClazz.getFields()){
            Data dataField = field.getAnnotation(Data.class);
            if(dataField != null) {
                tableStatement.append(dataField.id());
                tableStatement.append(" TEXT NOT NULL ");
                if (dataField.unique())
                    tableStatement.append(" UNIQUE ");
                tableStatement.append(",");
            }
        }
        tableStatement.setLength(tableStatement.length() - 1);
        tableStatement.append(");");

        PreparedStatement statement = null;
        statement = YuaBot.dbConnection.prepareStatement(tableStatement.toString());
        statement.execute();

        statement = YuaBot.dbConnection.prepareStatement("INSERT INTO textPattern (text) VALUES ('') RETURNING id;");
        ResultSet result = statement.executeQuery();
        result.next();
        int id = result.getInt("id");

        statement = YuaBot.dbConnection.prepareStatement("INSERT INTO sourceType (name, tableSource, textPattern) VALUES (?, ?, ?);");
        statement.setString(1, controller.getSourceName());
        statement.setString(2, tablename);
        statement.setInt(3, id);
        statement.execute();
    }

    public static int createSource(DataController controller, Source source) throws SQLException, IllegalAccessException {
        String tablename = controller.getSourceName() + "source";

        PreparedStatement statement = null;
        statement = YuaBot.dbConnection.prepareStatement("INSERT INTO source (authorName, sourceType) VALUES (?, ?) RETURNING id;");
        statement.setString(1, source.author);
        statement.setString(2, controller.getSourceName());
        ResultSet result = statement.executeQuery();
        result.next();
        int id = result.getInt("id");

        StringBuilder sourceStatement = new StringBuilder("INSERT INTO "+tablename+" (");
        sourceStatement.append("id ,");
        for(Field field : source.getClass().getFields()){
            Data dataField = field.getAnnotation(Data.class);
            if(dataField != null) {
                sourceStatement.append(dataField.id());
                sourceStatement.append(",");
            }
        }
        sourceStatement.setLength(sourceStatement.length() - 1);
        sourceStatement.append(") VALUES (?,");

        for(Field field : source.getClass().getFields()){
            Data dataField = field.getAnnotation(Data.class);
            if(dataField != null) {
                sourceStatement.append("?,");
            }
        }
        sourceStatement.setLength(sourceStatement.length() - 1);
        sourceStatement.append(");");

        statement = YuaBot.dbConnection.prepareStatement(sourceStatement.toString());
        statement.setInt(1, id);
        int index = 2;
        for(Field field : source.getClass().getFields()){
            Data dataField = field.getAnnotation(Data.class);
            if(dataField != null) {
                String v = (String) field.get(source);
                if(v == null) v = "";
                statement.setString(index, v);
                sourceStatement.append("?,");
                index++;
            }
        }
        statement.execute();

        return id;
    }

    public static boolean createMedia(int sourceId, Media media) throws SQLException {
        PreparedStatement statement = YuaBot.dbConnection.prepareStatement("INSERT INTO media (url, lastSent, source) VALUES (?, 0, ?);");
        statement.setString(1, media.media.getPath());
        statement.setInt(2, sourceId);
        statement.execute();
        return true;
    }

    public static Media getRandomMedia() throws SQLException {
        PreparedStatement statement = YuaBot.dbConnection.prepareStatement(
                "SELECT * FROM (SELECT * FROM media WHERE lastSent<=? OR lastSent=0 ORDER BY RANDOM() LIMIT 1) media, source, sourceType, textPattern WHERE media.source = source.id AND source.sourceType = sourceType.name AND sourceType.textPattern = textPattern.id;"
        );
        statement.setLong(1, 1000L);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        String text = "";
        String textPattern = resultSet.getString("text");
        String url = resultSet.getString("url");
        String tableSource = resultSet.getString("tableSource");
        File media = new File(url);

        try {
            text = YuaBot.processData(tableSource, url, textPattern);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Media mediaData = new Media();
        mediaData.text = text;
        mediaData.media = media;

        LOGGER.info("Media URL selected: "+url);

        return mediaData;
    }

    public static String processData(String tableSource, String mediaUrl, String textPattern) throws ClassNotFoundException, SQLException {
        PreparedStatement statement = YuaBot.dbConnection.prepareStatement(
                "SELECT * FROM (SELECT * FROM media WHERE media.url=?) media, source, "+tableSource+" WHERE media.source = source.id AND source.id = "+tableSource+".id;"
        );
        statement.setString(1, mediaUrl);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();

        Map<String, String> data = new HashMap<>();
        for(int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++){
            String name = resultSet.getMetaData().getColumnName(i);
            String value = resultSet.getObject(i).toString();
            System.out.println(name + " "+value);
            data.put(name, value);
        }

        for(Map.Entry<String, String> entry : data.entrySet()){
            textPattern = textPattern.replace("{"+entry.getKey()+"}", entry.getValue());
        }

        return textPattern;
    }
}