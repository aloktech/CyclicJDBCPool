/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.log4j.Log4j2;

/**
 *
 * @author p
 * @param <T>
 */
@Log4j2
public class JDBCBuilder<T> {

    private String query;
    private boolean parameterized;
    private Consumer<ResultSet> consumer;
    private Function<ResultSet, T> mapper;
    private ExecutionType type;
    private Optional<T> result = Optional.empty();
    private List<T> resultList = new ArrayList<>();
    private Map<Integer, JDBCData> params = new HashMap<>();

    private final Random random = new Random();

    public JDBCBuilder<T> query(String query) {
        this.query = query;
        this.consumer = null;
        this.mapper = null;
        return this;
    }

    public JDBCBuilder<T> params(Map<Integer, JDBCData> params) {
        this.params = params;
        return this;
    }

    public JDBCBuilder<T> consumer(Consumer<ResultSet> consumer) {
        this.consumer = consumer;
        return this;
    }

    public JDBCBuilder<T> mapper(Function<ResultSet, T> mapper) {
        this.mapper = mapper;
        return this;
    }

    public JDBCBuilder<T> executionType(ExecutionType type) {
        this.type = type;
        return this;
    }

    public JDBCBuilder<T> parameterizedQuery(boolean parameterized) {
        this.parameterized = parameterized;
        return this;
    }

    public JDBCBuilder<T> execute() {
        if (query == null || query.isBlank() || type == null || !(mapper == null || consumer == null)) {
            log.warn("Failed to execute");
            return this;
        }
        CircularQueueNode<Connection> poolNode = CircularConnectionPool.INSTANCE.getConnection().get();
        Connection conn = poolNode.value;
        try {
            if (parameterized) {
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    switch (type) {
                        case NORMAL:
                            readData(statement, query, consumer, poolNode.index);
                            break;
                        case GET_DATA:
                            result = extractData(statement, params, mapper, poolNode.index);
                            break;
                        case GET_DATA_LIST:
                            resultList = extractDataAsList(statement, params, mapper, poolNode.index);
                    }
                }
            } else {
                try (Statement statement = conn.createStatement()) {
                    switch (type) {
                        case NORMAL:
                            readData(statement, query, consumer, poolNode.index);
                            break;
                        case GET_DATA:
                            result = extractData(statement, query, mapper, poolNode.index);
                            break;
                        case GET_DATA_LIST:
                            resultList = extractDataAsList(statement, query, mapper, poolNode.index);
                    }
                }
            }
            log.info("Successfuly execute the query: " + type);
        } catch (SQLException e) {
            log.error("Failed to execute the query");
        } finally {
            CircularConnectionPool.INSTANCE.releaseConnection(poolNode);
        }
        return this;
    }

    private void readData(final Statement statement, String query, Consumer<ResultSet> consumer, int index) {
        try (ResultSet rs = statement.executeQuery(query)) {
            delayed();
            while (rs.next()) {
                consumer.accept(rs);
            }
            log.info("Get Simple Data: {}", index);
        } catch (Exception e) {
        }
    }

    private <T> Optional<T> extractData(final PreparedStatement statement, Map<Integer, JDBCData> params, Function<ResultSet, T> mapper, int index) {
        setParameters(params, statement);
        try (ResultSet rs = statement.executeQuery()) {
            delayed();
            while (rs.next()) {
                log.info("Get Data: {}", index);
                return Optional.of(mapper.apply(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void setParameters(Map<Integer, JDBCData> params, final PreparedStatement statement) {
        for (Map.Entry<Integer, JDBCData> entry : params.entrySet()) {
            try {
                switch (entry.getValue().getType()) {
                    case STRING:
                        statement.setString(entry.getKey(), (String) entry.getValue().getData());
                        break;
                    case INTEGER:
                        statement.setInt(entry.getKey(), (Integer) entry.getValue().getData());
                        break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private <T> Optional<T> extractData(final Statement statement, String query, Function<ResultSet, T> mapper, int index) {
        try (ResultSet rs = statement.executeQuery(query)) {
            delayed();
            while (rs.next()) {
                log.info("Get Data: {}", index);
                return Optional.of(mapper.apply(rs));
            }
        } catch (Exception e) {
        }
        return Optional.empty();
    }

    private <T> List<T> extractDataAsList(final PreparedStatement statement, Map<Integer, JDBCData> params, Function<ResultSet, T> mapper, int index) {
        List<T> resultData = new ArrayList<>();
        setParameters(params, statement);
        try (ResultSet rs = statement.executeQuery()) {
            delayed();
            while (rs.next()) {
                resultData.add(mapper.apply(rs));
            }
        } catch (Exception e) {
        }
        log.info("Get List Data: {}", index);
        return resultData;
    }

    private <T> List<T> extractDataAsList(final Statement statement, String query, Function<ResultSet, T> mapper, int index) {
        List<T> resultData = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery(query)) {
            delayed();
            while (rs.next()) {
                resultData.add(mapper.apply(rs));
            }
        } catch (Exception e) {
        }
        log.info("Get List Data: {}", index);
        return resultData;
    }

    private void delayed() {
        long delay = random.nextInt(10_000);
        log.info("Delayed by {}", delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Logger.getLogger(JDBCBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Optional<T> getResult() {
        return result;
    }

    public List<T> getResultList() {
        return resultList;
    }
}
