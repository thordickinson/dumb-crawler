package com.thordickinson.dumbcrawler.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

class ListResultHandler<T> implements ResultSetHandler<List<T>> {

    private final ResultSetHandler<T> delegate;

    public ListResultHandler(ResultSetHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<T> handle(ResultSet rs) throws SQLException {
        LinkedList<T> result = new LinkedList<>();
        while (rs.next()) {
            result.add(delegate.handle(rs));
        }
        return result;
    }
}

class RowResultSetHandler implements ResultSetHandler<List<Object>> {

    @Override
    public List<Object> handle(ResultSet rs) throws SQLException {
        LinkedList<Object> row = new LinkedList<>();
        for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
            row.add(rs.getObject(i + 1));
        }
        return row;
    }

}

public class JDBCUtil {

    public static <T> Optional<T> singleResult(Class<T> type, Connection connection,
            String sql) {
        return singleResult(type, connection, sql, Collections.emptyList());
    }

    public static <T> Optional<T> singleResult(Class<T> type, Connection connection,
            String sql, List<?> params) {

        var result = query(connection, sql, params);
        return result.isEmpty() ? Optional.empty() : Optional.of(type.cast(result.get(0).get(0)));
    }

    public static List<List<Object>> query(Connection connection, String sql) {
        return query(connection, sql, Collections.emptyList());
    }

    public static List<List<Object>> query(Connection connection, String sql, List<?> params) {
        QueryRunner runner = new QueryRunner();
        try {
            return runner.query(connection, sql, new ListResultHandler<List<Object>>(new RowResultSetHandler()),
                    params.toArray());
        } catch (SQLException ex) {
            throw new RuntimeException("Error running query: %s".formatted(sql), ex);
        }
    }

    public static int executeUpdate(Connection connection, String sql) {
        return executeUpdate(connection, sql, Collections.emptyList());
    }

    public static int executeUpdate(Connection connection, String sql, List<?> params) {
        try {
            var statement = connection.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Error executing update: %s".formatted(sql), ex);
        }
    }

    public static String generateParams(int paramsPerRow, int rowCount){
        String row = generateParams(paramsPerRow);
        StringBuilder rows = new StringBuilder();
        for(int i = 0; i < rowCount; i++){
            rows.append( i ==0 ? row : ", " + row);
        }
        return rows.toString();
    }

    public static String generateParams(int qty){
        StringBuilder row = new StringBuilder("( ");
        for(int i = 0; i < qty; i++){
            row.append( i == 0? "?" : ", ?");
        }
        row.append(" )");
        return row.toString();
    }

}
