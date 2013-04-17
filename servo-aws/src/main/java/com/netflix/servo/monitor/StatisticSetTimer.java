/*
 * StaisticSetMonitor.java            1.00  2013/04/17
 *
 * Copyright (c) 2013 Lama Lab, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Lama Lab, Inc. ("Confidential Information").
 */

package com.netflix.servo.monitor;

import com.amazonaws.services.cloudwatch.model.StatisticSet;

import java.util.concurrent.TimeUnit;

/**
 * @author fehrenbacher
 */
public interface StatisticSetTimer extends ResettableMonitor<StatisticSet> {

    /**
     * Returns a stopwatch that has been started and will automatically
     * record its result to this timer when stopped.
     */
    Stopwatch start();

    /**
     * The time unit reported by this timer.
     */
    TimeUnit getTimeUnit();

    /**
     * Record a new value for this timer.
     */
    void record(long duration);

    /**
     * Record a new value that was collected with the given TimeUnit.
     */
    void record(long duration, TimeUnit timeUnit);

}
