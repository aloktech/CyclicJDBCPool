/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author p
 */
public class JDBCDataBuilder {

    private final static Map<Integer, JDBCData> PARAMS = new HashMap<>();

    public JDBCDataBuilder add(int index, DataType type, Object data) {
        PARAMS.put(index, new JDBCData(data, type));
        return this;
    }

    public Map<Integer, JDBCData> build() {
        return PARAMS;
    }

}
