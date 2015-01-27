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
package org.jmxtrans.core.query;

import lombok.Getter;
import org.jmxtrans.core.log.Logger;
import org.jmxtrans.core.log.LoggerFactory;
import org.jmxtrans.core.results.QueryResult;
import org.jmxtrans.utils.collections.ArrayUtils;
import org.jmxtrans.utils.collections.Iterables2;
import org.jmxtrans.utils.time.Clock;
import org.jmxtrans.utils.time.SystemClock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * @deprecated use @link{org.jmxtrans.core.query.embedded.Query} instead.
 */
@Immutable
@ThreadSafe
@Deprecated
public class Query {

    @Nonnull
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Nonnull
    private final ResultNameStrategy resultNameStrategy;

    @Nonnull @Getter
    private final ObjectName objectName;
    @Nonnull @Getter
    private final String resultAlias;
    /**
     * The attribute to retrieve ({@link javax.management.MBeanServer#getAttribute(javax.management.ObjectName, String)})
     */
    @Nonnull @Getter
    private final String attribute;
    /**
     * If the MBean attribute value is a {@link javax.management.openmbean.CompositeData}, the key to lookup.
     */
    @Nullable @Getter
    private final String key;
    /**
     * If the returned value is a {@link java.util.Collection}or an array, the position of the entry to lookup.
     */
    @Nullable @Getter
    private final Integer position;
    /**
     * Attribute type like '{@code gauge}' or '{@code counter}'. Used by monitoring systems like Librato who require this information.
     */
    @Nullable @Getter
    private final String type;

    @Nonnull
    private final Clock clock = new SystemClock();

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, null, null, attribute, resultNameStrategy);
    }

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute, @Nullable Integer position, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, position, null, attribute, resultNameStrategy);
    }

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute, @Nonnull String resultAlias, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, null, null, resultAlias, resultNameStrategy);
    }

    /**
     * @param objectName         The {@link javax.management.ObjectName} to search for
     *                           ({@link javax.management.MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)}),
     *                           can contain wildcards and return several entries.
     * @param attribute          The attribute to retrieve ({@link javax.management.MBeanServer#getAttribute(javax.management.ObjectName, String)})
     * @param key                if the MBean attribute value is a {@link javax.management.openmbean.CompositeData}, the key to lookup.
     * @param position           if the returned value is a {@link java.util.Collection} or an array, the position of the entry to lookup.
     * @param type               type of the metric ('counter', 'gauge', ...)
     * @param resultAlias
     * @param resultNameStrategy the {@link ResultNameStrategy} used during the
     *                           {@link #collectMetrics(javax.management.MBeanServer, java.util.Queue)} phase.
     */
    public Query(
            @Nonnull String objectName,
            @Nonnull String attribute,
            @Nullable String key,
            @Nullable Integer position,
            @Nullable String type,
            @Nonnull String resultAlias,
            @Nonnull ResultNameStrategy resultNameStrategy) {
        try {
            this.objectName = new ObjectName(Objects.requireNonNull(objectName));
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid objectName '" + objectName + "'", e);
        }
        this.attribute = Objects.requireNonNull(attribute);
        this.key = key;
        this.resultAlias = Objects.requireNonNull(resultAlias);
        this.position = position;
        this.type = type;
        this.resultNameStrategy = Objects.requireNonNull(resultNameStrategy, "resultNameStrategy");
    }

    public void collectMetrics(@Nonnull MBeanServer mbeanServer, @Nonnull Queue<QueryResult> resultQueue) {
        Set<ObjectName> objectNames = mbeanServer.queryNames(objectName, null);

        for (ObjectName on : objectNames) {

            try {
                Object attributeValue = mbeanServer.getAttribute(on, attribute);

                processAttributeValues(on, attributeValue, resultQueue);
            } catch (AttributeNotFoundException | MBeanException | ReflectionException | InstanceNotFoundException e) {
                logCollectingException(on, e);
            }
        }
    }

    private void processAttributeValues(
            @Nonnull ObjectName on,
            @Nullable Object attributeValue,
            @Nonnull Collection<QueryResult> resultQueue) {
        if (attributeValue == null) {
            // skip null values
            return;
        }

        if (attributeValue instanceof CompositeData && key == null) {
            logger.warn("Ignore compositeData without key specified for '" + on + "'#" + attribute + ": " + attributeValue);
            return;
        }

        if (!(attributeValue instanceof CompositeData) && key != null) {
            logger.warn("Ignore NON compositeData for specified key for '" + on + "'#" + attribute + "#" + key + ": " + attributeValue);
            return;
        }

        Object value;
        if (attributeValue instanceof CompositeData) {
            value = ((CompositeData) attributeValue).get(key);
        } else {
            value = attributeValue;
        }

        value = ArrayUtils.transformToListIfIsArray(value);

        String resultName = resultNameStrategy.getResultName(this, on, key);
        if (value instanceof Iterable) {
            Iterable iterable = (Iterable) value;
            if (position == null) {
                int idx = 0;
                for (Object entry : iterable) {
                    addResult(new QueryResult(resultName + "_" + idx++, type, entry, clock.currentTimeMillis()), resultQueue);
                }
            } else {
                value = Iterables2.get(iterable, position);
                addResult(new QueryResult(resultName, type, value, clock.currentTimeMillis()), resultQueue);
            }
        } else {
            addResult(new QueryResult(resultName, type, value, clock.currentTimeMillis()), resultQueue);
        }
    }

    private void addResult(QueryResult queryResult, Collection<QueryResult> resultQueue) {
        try {
            resultQueue.add(queryResult);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Result queue is full, could not add query " + queryResult
                    + " further results for this query will be ignored.");
        }
    }

    private void logCollectingException(ObjectName on, Exception e) {
        logger.warn("Exception collecting " + on + "#" + attribute + (key == null ? "" : "#" + key), e);
    }

    @Override
    public String toString() {
        return "Query{" +
                "objectName=" + objectName +
                ", resultAlias='" + resultAlias + '\'' +
                ", attribute='" + attribute + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Query query = (Query) o;

        return attribute.equals(query.attribute)
                && !(key != null ? !key.equals(query.key) : query.key != null)
                && objectName.equals(query.objectName)
                && !(position != null ? !position.equals(query.position) : query.position != null)
                && resultAlias.equals(query.resultAlias)
                && resultNameStrategy.equals(query.resultNameStrategy)
                && !(type != null ? !type.equals(query.type) : query.type != null);

    }

    @Override
    public int hashCode() {
        int result = resultNameStrategy.hashCode();
        result = 31 * result + objectName.hashCode();
        result = 31 * result + resultAlias.hashCode();
        result = 31 * result + attribute.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
