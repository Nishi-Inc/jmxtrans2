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
package org.jmxtrans.core.scheduler;

import org.jmxtrans.core.log.Logger;
import org.jmxtrans.core.log.LoggerFactory;
import org.jmxtrans.core.output.OutputWriter;
import org.jmxtrans.core.query.embedded.Query;
import org.jmxtrans.core.query.embedded.ResultNameStrategy;
import org.jmxtrans.core.query.embedded.Server;
import org.jmxtrans.core.results.QueryResult;
import org.jmxtrans.utils.time.Clock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class QueryProcessor {

    @Nonnull private final Clock clock;
    @Nonnull private final Iterable<OutputWriter> outputWriters;
    @Nonnull private final Executor queryExecutor;
    @Nonnull private final ResultProcessor resultProcessor;
    @Nonnull private final ResultNameStrategy resultNameStrategy;

    public QueryProcessor(
            @Nonnull Clock clock,
            @Nonnull Iterable<OutputWriter> outputWriters,
            @Nonnull Executor queryExecutor,
            @Nonnull ResultProcessor resultProcessor,
            @Nonnull ResultNameStrategy resultNameStrategy) {
        this.clock = clock;
        this.outputWriters = outputWriters;
        this.queryExecutor = queryExecutor;
        this.resultProcessor = resultProcessor;
        this.resultNameStrategy = resultNameStrategy;
    }

    public void process(long deadline, @Nonnull Server server, @Nonnull Query query) {
        queryExecutor.execute(new Processor(clock, deadline, server, query, outputWriters, resultProcessor, resultNameStrategy));
    }

    @ThreadSafe
    private static class Processor extends DeadlineRunnable {
        @Nonnull private final Logger logger = LoggerFactory.getLogger(getClass().getName());
        @Nonnull private final Query query;
        @Nonnull private final Server server;
        @Nonnull private final Iterable<OutputWriter> outputWriters;
        @Nonnull private final ResultProcessor resultProcessor;
        @Nonnull private final ResultNameStrategy resultNameStrategy;

        public Processor(
                @Nonnull Clock clock,
                long deadline,
                @Nonnull Server server,
                @Nonnull Query query,
                @Nonnull Iterable<OutputWriter> outputWriters,
                @Nonnull ResultProcessor resultProcessor,
                @Nonnull ResultNameStrategy resultNameStrategy) {
            super(clock, deadline);
            this.query = query;
            this.server = server;
            this.outputWriters = outputWriters;
            this.resultProcessor = resultProcessor;
            this.resultNameStrategy = resultNameStrategy;
        }

        @Override
        protected void doRun() {
            try {
                logger.debug("Collecting metrics for " + query);
                Iterable<QueryResult> results = query.collectMetrics(server.getServerConnection(), resultNameStrategy);
                for (OutputWriter outputWriter : outputWriters) {
                    for (QueryResult result : results) {
                        try {
                            resultProcessor.writeResult(getDeadline(), result, outputWriter);
                        } catch (RejectedExecutionException e) {
                            logger.warn("Could not enqueue result to writers.", e);
                        }
                    }
                }
            } catch (Exception e) {
                // TODO
            }
        }
    }
}
