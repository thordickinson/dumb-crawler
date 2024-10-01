package com.thordickinson.dumbcrawler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static com.thordickinson.dumbcrawler.util.JDBCUtil.*;

public class SQLiteConnection implements AutoCloseable {



    private record TableDef(String name, Map<String, String> columns, boolean withRowId, List<String> indices) {
        public String getSQL() {
            var columns = columns().entrySet().stream().map( (entry) -> "{0} {1}".formatted(entry.getKey(), entry.getValue()) );
            return "CREATE TABLE IF NOT EXISTS {0} ({1})".formatted(name, columns);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SQLiteConnection.class);
    private final Path directory;
    private Connection connection;
    private final String fileName;
    private final List<TableDef> tables = new LinkedList<>();

    public SQLiteConnection(Path directory, String fileName){
        this.directory = directory;
        this.fileName = fileName;
    }

    public SQLiteConnection(Path directory){
        this(directory, "db");
    }

    public void addTable(String name, Map<String, String> columns, boolean withRowId, List<String> indices){
        tables.add(new TableDef(name, columns, withRowId, indices));
    }

    public void addTable(String name, Map<String, String> columns, boolean withRowId){
        addTable(name, columns, withRowId, Collections.emptyList());
    }

    public void addTable(String name, Map<String, String> columns){
        addTable(name, columns, false);
    }


    public Connection getConnection(){
        if(connection == null){
            connection = createConnection();
            initializeTables();
        }
        return connection;
    }

    private void initializeTables(){
        logger.info("Initializing tables");
        for(var table : tables){
            logger.info("Creating table: {0}".formatted(table.name()));
            executeUpdate(connection, table.getSQL());
            for(var index : table.indices){
                executeUpdate(connection, index);
            }
        }
    }

    private Connection createConnection() {
        Path url = directory.resolve("%s.sqlite".formatted(fileName));
        url.getParent().toFile().mkdirs();
        try {
            var conn = DriverManager.getConnection("jdbc:sqlite:%s".formatted(url.toAbsolutePath()));
            logger.info("Connected to database at: %s".formatted(url));
            return conn;
        } catch (SQLException ex) {
            throw new RuntimeException("Error connecting to: %s".formatted(url), ex);
        }
    }

    public int update(String sql){
        return executeUpdate(getConnection(), sql);
    }
    public int update(String sql, List<?> params){
        return executeUpdate(getConnection(), sql, params);
    }

    public <T> Optional<T> singleResult(Class<T> expectedType, String query) {
        return JDBCUtil.singleResult(expectedType, getConnection(), query);
    }

    public List<List<Object>> query(String sql, List<?> params){
        return JDBCUtil.query(getConnection(), sql, params);
    }

    public List<List<Object>> query(String sql){
        return JDBCUtil.query(getConnection(), sql);
    }

    @Override
    public void close() throws Exception {
        if(connection != null){
            this.connection.close();
        }
    }
}
