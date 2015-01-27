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

import java.io.Serializable;
import java.util.Comparator;

import static java.lang.String.format;

public class QueryResultValueComparator implements Comparator<QueryResult>, Serializable {

    private static final long serialVersionUID = 0L;

    @Override
    @SuppressWarnings("unchecked")
    public int compare(QueryResult result1, QueryResult result2) {
        Object value1 = result1.getValue();
        Object value2 = result2.getValue();

        if (value1 == value2) {
            return 0;
        }

        if (value1 == null) {
            return -1;
        }

        if (value2 == null) {
            return 1;
        }

        if (value1 instanceof Comparable<?> && value2 instanceof Comparable<?>) {
            return ((Comparable) value1).compareTo(value2);
        }
        throw new ClassCastException(format("Query results are not comparable [%s], [%s]", result1, result2));
    }
}
