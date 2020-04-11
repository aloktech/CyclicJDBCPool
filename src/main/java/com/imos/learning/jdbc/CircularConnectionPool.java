/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author p
 */
@Log4j2
public enum CircularConnectionPool {

    INSTANCE;

    private final String urlFormat = "%s:%s://%s:%s/%s";

    private int size;
    private String jdbcDriver;
    private String userName;
    private String password;
    private String url;
    private volatile CircularQueueNode<Connection> rootHeader;
    private volatile CircularQueueNode<Connection> tailHeader;
    private volatile CircularQueueNode<Connection> releaseHeader;
    private volatile CircularQueueNode<Connection> allocateHeader;
    private final Lock lock;
    private final Condition serviceAvaialble;

    private CircularConnectionPool() {
        releaseHeader = allocateHeader = tailHeader = rootHeader;
        lock = new ReentrantLock();
        serviceAvaialble = lock.newCondition();

        config();
    }

    public void config() {
        Logger LOGGER = LogManager.getLogger(CircularConnectionPool.class);

        Properties prop = new Properties();
        try {
            String defaultFile = System.getProperty("user.dir") + "/default.properties";
            File file = new File(defaultFile);
            if (file.exists()) {
                prop.load(Files.newInputStream(Paths.get(defaultFile)));
            } else {
                prop.load(JDBCBuilder.class.getClassLoader().getResourceAsStream("default.properties"));
            }
            size = Integer.parseInt(Optional.ofNullable(prop.getProperty("pool.size")).orElse("4"));
            userName = prop.getProperty("username");
            password = prop.getProperty("password");
            jdbcDriver = prop.getProperty("jdbc.driver");
            url = String.format(urlFormat,
                    prop.getProperty("protocol"),
                    prop.getProperty("sub.protocol"),
                    prop.getProperty("host"),
                    prop.getProperty("port"),
                    prop.getProperty("database"));
            LOGGER.info("Connection Pool is configured");
        } catch (IOException e) {
            LOGGER.error("Connection Pool failed to configure");
        }
    }

    public void generatePool() {
        try {
            Class.forName(jdbcDriver);
            for (int i = 0; i < size; i++) {
                try {
                    Connection conn = DriverManager.getConnection(url, userName, password);
                    CircularQueueNode<Connection> node = new CircularQueueNode<>(conn, i);
                    if (rootHeader == null) {
                        rootHeader = tailHeader = node;
                        rootHeader.next = tailHeader;
                        rootHeader.previous = tailHeader;
                        tailHeader.next = rootHeader;
                        tailHeader.previous = rootHeader;
                        allocateHeader = rootHeader;
                        releaseHeader = rootHeader;
                    } else {
                        if (rootHeader.index == tailHeader.index) {
                            tailHeader = node;
                            tailHeader.next = rootHeader;
                            tailHeader.previous = rootHeader;
                            rootHeader.next = tailHeader;
                            rootHeader.previous = tailHeader;
                        } else {
                            node.next = rootHeader;
                            node.previous = tailHeader;
                            CircularQueueNode temp = tailHeader;
                            temp.next = node;
                            tailHeader = node;
                        }
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }
            }
            log.info("Generate Connection Pool of size: {}", size);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
        }
    }

    public void closePool() {
        try {
            CircularQueueNode<Connection> temp = rootHeader.next;
            rootHeader.value.close();
            while (temp.index != rootHeader.index) {
                temp.value.close();
                temp = temp.next;
            }
            log.info("Release Connection Pool");
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public Optional<CircularQueueNode> getConnection() {
        lock.lock();
        CircularQueueNode startNode = allocateHeader;
        try {
            CircularQueueNode<Connection> tempNode = allocateHeader;
            if (tempNode.available) {
                tempNode.available = false;
            } else {
                while (!tempNode.available) {
                    tempNode = tempNode.next;
                    if (tempNode == startNode) {
                        log.info("Waiting for Connection in Pool");
                        serviceAvaialble.await();
                    }
                }
                if (tempNode.available) {
                    tempNode.available = false;
                }
            }
            allocateHeader = tempNode.next;
            log.info("Get Connection: {}", tempNode.index);
            return Optional.ofNullable(tempNode);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        } finally {
            lock.unlock();
        }
        log.info("Failed to get Connection");
        return Optional.empty();
    }

    public void releaseConnection(CircularQueueNode node) {
        if (node == null) {
            return;
        }
        lock.lock();
        try {
            boolean backward = releaseHeader.index > node.index && (releaseHeader.index - node.index) > 0;
            CircularQueueNode tempNode = releaseHeader;
            if (tempNode.index == node.index) {
                tempNode.available = true;
                releaseHeader = tempNode.next;
            } else {
                while (tempNode.index != node.index) {
                    tempNode = backward ? tempNode.previous : tempNode.next;
                }
                tempNode.available = true;
                releaseHeader = tempNode;
            }
            serviceAvaialble.signal();
            log.info("Release Connection: {}", tempNode.index);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
