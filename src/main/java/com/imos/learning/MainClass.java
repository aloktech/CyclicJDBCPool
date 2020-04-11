/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning;

import com.imos.learning.jdbc.ExecutionType;
import com.imos.learning.jdbc.JDBCDataBuilder;
import com.imos.learning.jdbc.CircularConnectionPool;
import com.imos.learning.jdbc.DataType;
import com.imos.learning.jdbc.JDBCBuilder;
import com.imos.learning.jdbc.JDBCData;
import com.imos.learning.jdbc.ResultSetBuilder;
import java.sql.ResultSet;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author p
 */
public class MainClass {

    private static final Logger LOGGER = LogManager.getLogger(MainClass.class);

    public static void main(String[] args) throws InterruptedException {
        CircularConnectionPool.INSTANCE.generatePool();

        new Thread(() -> {
            JDBCBuilder<String> builder = new JDBCBuilder<>();
            builder.query("select * from temp")
                    .executionType(ExecutionType.NORMAL)
                    .consumer((ResultSet rs) -> {
                        ResultSetBuilder rsBuilder = new ResultSetBuilder(rs);
                        LOGGER.info(rsBuilder.getString(1));
                    })
                    .execute()
                    .getResult()
                    .ifPresent(result -> {
                        LOGGER.info(result);
                    });
        }).start();

        new Thread(() -> {
            JDBCBuilder<String> builder = new JDBCBuilder<>();
            builder.query("select * from temp")
                    .executionType(ExecutionType.GET_DATA_LIST)
                    .mapper((ResultSet rs) -> {
                        ResultSetBuilder rsBuilder = new ResultSetBuilder(rs);
                        return rsBuilder.getString(1);
                    })
                    .execute()
                    .getResultList()
                    .forEach(System.out::println);
        }).start();

        for (int i = 0; i < 30; i++) {
            executeGetData();
        }

        Thread.sleep(60_000);
        CircularConnectionPool.INSTANCE.closePool();
    }

    private static void executeGetData() {
        new Thread(() -> {
            Map<Integer, JDBCData> params = new JDBCDataBuilder()
                    .add(1, DataType.INTEGER, 12)
                    .build();
            JDBCBuilder<String> builder = new JDBCBuilder<>();
            builder.query("select * from temp where id = ?")
                    .executionType(ExecutionType.GET_DATA)
                    .parameterizedQuery(true)
                    .params(params)
                    .mapper((ResultSet rs) -> {
                        ResultSetBuilder rsBuilder = new ResultSetBuilder(rs);
                        return rsBuilder.getString(1);
                    })
                    .execute()
                    .getResult()
                    .ifPresent(result -> {
                        LOGGER.info(result);
                    });
        }).start();
    }
}
