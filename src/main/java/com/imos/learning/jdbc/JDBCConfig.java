/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author p
 */
@Getter
@Builder
public class JDBCConfig {

    private final String jdbcDriver;
    private final String userName;
    private final String password;
    private final String protocol;
    private final String subProtocol;
    private final String host;
    private final String database;
    private final String parameters = "";
    private final int port;
}
