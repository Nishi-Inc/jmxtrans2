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
package org.jmxtrans.output.writers;

import org.jmxtrans.core.log.Logger;
import org.jmxtrans.core.log.LoggerFactory;
import org.jmxtrans.core.output.OutputWriter;
import org.jmxtrans.core.output.OutputWriterFactory;
import org.jmxtrans.core.results.QueryResult;
import org.jmxtrans.utils.time.Clock;
import org.jmxtrans.utils.time.SystemClock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jmxtrans.utils.ConfigurationUtils.getInt;
import static org.jmxtrans.utils.ConfigurationUtils.getString;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
@NotThreadSafe
public class GraphitePlainTextTcpOutputWriter implements OutputWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Nonnull private final static Charset UTF_8 = Charset.forName("UTF-8");
    protected String metricPathPrefix;
    private Socket socket;
    private Writer writer;
    private final int socketConnectTimeoutInMillis;
    @Nonnull private final InetSocketAddress serverAddress;
    @Nonnull private final Clock clock = new SystemClock();

    protected GraphitePlainTextTcpOutputWriter(
            String metricPathPrefix,
            int socketConnectTimeoutInMillis,
            @Nonnull InetSocketAddress serverAddress) {
        this.metricPathPrefix = metricPathPrefix;
        this.socketConnectTimeoutInMillis = socketConnectTimeoutInMillis;
        this.serverAddress = serverAddress;

        logger.info("GraphitePlainTextTcpOutputWriter is configured with " + serverAddress + ", metricPathPrefix=" + metricPathPrefix +
                ", socketConnectTimeoutInMillis=" + socketConnectTimeoutInMillis);
    }

    /**
     * {@link java.net.InetAddress#getLocalHost()} may not be known at JVM startup when the process is launched as a Linux service.
     *
     * @return
     */
    @Nonnull
    protected String buildMetricPathPrefix() {
        if (metricPathPrefix != null) {
            return metricPathPrefix;
        }
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName().replaceAll("\\.", "_");
        } catch (UnknownHostException e) {
            hostname = "#unknown#";
        }
        metricPathPrefix = "servers." + hostname + ".";
        return metricPathPrefix;
    }

    @Override
    public void write(@Nonnull QueryResult result) throws IOException {
        String msg = buildMetricPathPrefix() + result.getName() + " " + result.getValue() + " " + SECONDS.convert(clock.currentTimeMillis(), MILLISECONDS);
        try {
            ensureGraphiteConnection();
            logger.debug("Send '" + msg + "' to " + serverAddress);
            writer.write(msg + "\n");
        } catch (IOException e) {
            logger.warn("Exception sending '" + msg + "' to " + serverAddress, e);
            releaseGraphiteConnection();
            throw e;
        }
    }

    private void releaseGraphiteConnection() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                logger.warn("Exception closing writer for socket " + socket, e);
            }
            writer = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Exception closing socket " + socket, e);
            }
            socket = null;
        }
    }

    private void ensureGraphiteConnection() throws IOException {
        boolean socketIsValid;
        try {
            socketIsValid = socket != null &&
                    socket.isConnected()
                    && socket.isBound()
                    && !socket.isClosed()
                    && !socket.isInputShutdown()
                    && !socket.isOutputShutdown();
        } catch (Exception e) {
            socketIsValid = false;
        }
        if (!socketIsValid) {
            writer = null;
            try {
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.connect(
                        serverAddress,
                        socketConnectTimeoutInMillis);
            } catch (IOException e) {
                ConnectException ce = new ConnectException("Exception connecting to " + serverAddress);
                ce.initCause(e);
                throw ce;
            }
        }
        if (writer == null) {
            writer = new OutputStreamWriter(socket.getOutputStream(), UTF_8);
        }
    }

    @Override
    @Nonnull
    public String toString() {
        return "GraphitePlainTextTcpOutputWriter{" +
                ", " + serverAddress +
                ", metricPathPrefix='" + metricPathPrefix + '\'' +
                '}';
    }

    public static final class Factory implements OutputWriterFactory<GraphitePlainTextTcpOutputWriter> {
        public final static String SETTING_HOST = "host";
        public final static String SETTING_PORT = "port";
        public static final int SETTING_PORT_DEFAULT_VALUE = 2003;
        public final static String SETTING_NAME_PREFIX = "namePrefix";
        public final static String SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS = "socket.connectTimeoutInMillis";
        public final static int SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE = 500;

        @Nonnull
        @Override
        public GraphitePlainTextTcpOutputWriter create(@Nonnull Map<String, String> settings) {
            String metricPathPrefix = getString(settings, SETTING_NAME_PREFIX, null);
            int socketConnectTimeoutInMillis = getInt(settings,
                    SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS,
                    SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE);
            InetSocketAddress graphiteAddress = new InetSocketAddress(
                    getString(settings, SETTING_HOST),
                    getInt(settings, SETTING_PORT, SETTING_PORT_DEFAULT_VALUE));

            return new GraphitePlainTextTcpOutputWriter(
                    metricPathPrefix,
                    socketConnectTimeoutInMillis,
                    graphiteAddress);
        }
    }

}
