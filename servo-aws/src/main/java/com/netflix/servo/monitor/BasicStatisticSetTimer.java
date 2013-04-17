/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.amazonaws.services.cloudwatch.model.StatisticSet;
import com.google.common.base.Objects;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.TimeUnit;

/**
 * Analogous to BasicTimer, but reports one metric in the form of a StatisticSet, reportable directly to CloudWatch
 */
public class BasicStatisticSetTimer extends AbstractMonitor<StatisticSet> implements StatisticSetTimer {

    private static final String STATISTIC = "statistic";
    private static final String UNIT = "unit";

    private static final Tag STAT_TOTAL = Tags.newTag(STATISTIC, "totalTime");
    private static final Tag STAT_COUNT = Tags.newTag(STATISTIC, "count");
    private static final Tag STAT_MIN = Tags.newTag(STATISTIC, "min");
    private static final Tag STAT_MAX = Tags.newTag(STATISTIC, "max");

    private final TimeUnit timeUnit;

    private final ResettableCounter totalTime;
    private final ResettableCounter count;
    private final MinGauge min;
    private final MaxGauge max;

    private final Timer timer;

    /**
     * Creates a new instance of the timer with a unit of milliseconds.
     */
    public BasicStatisticSetTimer(MonitorConfig config) {
        this(config, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new instance of the timer.
     */
    public BasicStatisticSetTimer(MonitorConfig config, TimeUnit unit) {
        super(config);

        final Tag unitTag = Tags.newTag(UNIT, unit.name());
        final MonitorConfig unitConfig = config.withAdditionalTag(unitTag);
        timeUnit = unit;

        totalTime = new ResettableCounter(unitConfig.withAdditionalTag(STAT_TOTAL));
        count = new ResettableCounter(unitConfig.withAdditionalTag(STAT_COUNT));
        min = new MinGauge(unitConfig.withAdditionalTag(STAT_MIN));
        max = new MaxGauge(unitConfig.withAdditionalTag(STAT_MAX));
        timer = new Timer() {
            @Override
            public TimeUnit getTimeUnit() {
                return BasicStatisticSetTimer.this.getTimeUnit();
            }

            @Override
            public void record(long duration) {
                BasicStatisticSetTimer.this.record(duration);
            }

            @Override
            public void record(long duration, TimeUnit timeUnit) {
                BasicStatisticSetTimer.this.record(duration, timeUnit);
            }

            @Override public Stopwatch start() { throw new NotImplementedException(); }

            @Override public Long getValue() { throw new NotImplementedException(); }

            @Override public MonitorConfig getConfig() { throw new NotImplementedException(); }
        };
    }

    public Stopwatch start() {
        Stopwatch s = new BasicStopwatch(timer);
        s.start();
        return s;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public synchronized void record(long duration) {
        totalTime.increment(duration);
        count.increment();
        min.update(duration);
        max.update(duration);
    }

    public synchronized void record(long duration, TimeUnit unit) {
        record(this.timeUnit.convert(duration, unit));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized StatisticSet getValue() {
        return new StatisticSet().withSum(Double.valueOf(totalTime.getValue()))
                .withSampleCount(Double.valueOf(count.getValue()))
                .withMinimum(Double.valueOf(min.getValue()))
                .withMaximum(Double.valueOf(max.getValue()));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized StatisticSet getAndResetValue() {
        return new StatisticSet().withSum(Double.valueOf(totalTime.getAndResetValue()))
                .withSampleCount(Double.valueOf(count.getAndResetValue()))
                .withMinimum(Double.valueOf(min.getAndResetValue()))
                .withMaximum(Double.valueOf(max.getAndResetValue()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BasicStatisticSetTimer)) {
            return false;
        }
        BasicStatisticSetTimer m = (BasicStatisticSetTimer) obj;
        return config.equals(m.getConfig())
                && totalTime.equals(m.totalTime)
                && count.equals(m.count)
                && min.equals(m.min)
                && max.equals(m.max);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, totalTime, count, min, max);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("totalTime", totalTime)
                .add("count", count)
                .add("min", min)
                .add("max", max)
                .toString();
    }

}
