/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author p
 */
@Getter
@AllArgsConstructor
public class JDBCData {
    
    private final Object data;
    private final DataType type;
}
