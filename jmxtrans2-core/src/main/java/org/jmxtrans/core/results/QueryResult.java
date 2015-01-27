/**
 * The MIT License
 * Copyright (c) 2014 JMXTrans Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jmxtrans.core.results;

import lombok.Getter;
import org.jmxtrans.utils.Preconditions2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.hash;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
@Immutable
@ThreadSafe
public class QueryResult {
    @Nonnull @Getter
    private final String name;
    private final long epochInMillis;
    @Nullable @Getter
    private final Object value;
    @Nullable @Getter
    private final String type;

    /**
     * @param name          plain name of the metric (variables (e.g. <code>%my-jmx-attr%</code>) must have been resolved).
     * @param value         value of the collected metric
     * @param epochInMillis collect time in millis (see {@link System#currentTimeMillis()})
     */
    public QueryResult(@Nonnull String name, @Nullable Object value, long epochInMillis) {
        this(name, null, value, epochInMillis);
    }

    /**
     * @param name          plain name of the metric (variables (e.g. <code>%my-jmx-attr%</code>) must have been resolved).
     * @param type          type of the metric (e.g. "{@code counter}", "{@code gauge}", ...)
     * @param value         value of the collected metric
     * @param epochInMillis collect time in millis (see {@link System#currentTimeMillis()})
     */
    public QueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value, long epochInMillis) {
        this.name = Preconditions2.checkNotEmpty(name);
        this.value = value;
        this.epochInMillis = epochInMillis;
        this.type = type;
    }

    public long getEpoch(TimeUnit timeUnit) {
        return timeUnit.convert(epochInMillis, MILLISECONDS);
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "name='" + name + '\'' +
                ", epoch=" + new Timestamp(epochInMillis) +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (getClass() != o.getClass()) return false;

        QueryResult that = (QueryResult) o;
        return Objects.equals(name, that.name)
                && Objects.equals(epochInMillis, that.epochInMillis)
                && Objects.equals(value, that.value)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return hash(name, epochInMillis, value, type);
    }
}
