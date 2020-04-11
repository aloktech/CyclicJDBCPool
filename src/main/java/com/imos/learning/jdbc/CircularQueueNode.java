/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.learning.jdbc;

/**
 *
 * @author p
 * @param <T>
 */
public class CircularQueueNode<T> {

    public final T value;
    public final int index;
    public boolean available;
    public CircularQueueNode next;
    public CircularQueueNode previous;

    public CircularQueueNode(T value, int index) {
        this.value = value;
        this.index = index;
        this.available = true;
    }

    @Override
    public String toString() {
        return "" + index;
    }
}
