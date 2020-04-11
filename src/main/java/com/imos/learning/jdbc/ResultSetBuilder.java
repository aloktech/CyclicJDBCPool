/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

import java.sql.ResultSet;
import lombok.extern.log4j.Log4j2;

/**
 *
 * @author p
 */
@Log4j2
public class ResultSetBuilder {

    private final ResultSet resultSet;

    public ResultSetBuilder(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public String getString(int columnIndex) {
        try {
            return resultSet.getString(columnIndex);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "";
    }

    public String getString(String columnName) {
        try {
            return resultSet.getString(columnName);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "";
    }
}
