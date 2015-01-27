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
package org.jmxtrans.core.query.embedded;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static javax.management.remote.JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import static org.jmxtrans.utils.Preconditions2.checkNotEmpty;

public class RemoteServer implements Server {

    @Nonnull private final JMXServiceURL url;
    @Nullable private final String username;
    @Nullable private final String password;
    @Nullable private final String protocolProviderPackages;
    @Nonnull @Getter
    private final Iterable<Query> queries;

    private RemoteServer(
            @Nonnull JMXServiceURL url,
            @Nullable String username,
            @Nullable String password,
            @Nullable String protocolProviderPackages,
            @Nonnull Iterable<Query> queries) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.protocolProviderPackages = protocolProviderPackages;
        this.queries = queries;
    }

    @Nullable
    @Override
    public String getHost() {
        return url.getHost();
    }

    @Nonnull
    private Map<String, ?> getEnvironment() {
        if (protocolProviderPackages != null && protocolProviderPackages.contains("weblogic")) {
            return getWebLogicEnvironment();
        }
        return getNonWebLogicEnvironment();
    }

    @Nonnull
    private Map<String, ?> getWebLogicEnvironment() {
        Map<String, String> environment = new HashMap<>();
        if ((username != null) && (password != null)) {
            environment.put(PROTOCOL_PROVIDER_PACKAGES, protocolProviderPackages);
            environment.put(SECURITY_PRINCIPAL, username);
            environment.put(SECURITY_CREDENTIALS, password);
        }
        return environment;
    }

    @Nonnull
    private Map<String, ?> getNonWebLogicEnvironment() {
        Map<String, String[]> environment = new HashMap<>();
        if ((username != null) && (password != null)) {
            String[] credentials = new String[] {
                    username,
                    password
            };
            environment.put(JMXConnector.CREDENTIALS, credentials);
        }
        return environment;
    }

    @Nonnull
    @Override
    public MBeanServerConnection getServerConnection() throws Exception {
        // FIXME: closing of JMXConnector is not done
        return JMXConnectorFactory.connect(url, this.getEnvironment()).getMBeanServerConnection();
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @Nullable private JMXServiceURL url;
        @Nullable private String host;
        @Nullable private Integer port;
        @Nullable private String username;
        @Nullable private String password;
        @Nullable private String protocolProviderPackages;
        @Nonnull private final Collection<Query> queries = new ArrayList<>();

        @Nonnull
        public Builder withUrl(@Nullable String url) throws MalformedURLException {
            if (url == null) {
                this.url = null;
            } else {
                this.url = new JMXServiceURL(url);
            }
            return this;
        }

        @Nonnull
        public Builder withHost(@Nullable String host) {
            this.host = host;
            return this;
        }

        @Nonnull
        public Builder withPort(@Nullable Integer port) {
            this.port = port;
            return this;
        }

        @Nonnull
        public Builder withUsername(@Nullable String username) {
            this.username = username;
            return this;
        }

        @Nonnull
        public Builder withPassword(@Nullable String password) {
            this.password = password;
            return this;
        }

        @Nonnull
        public Builder withProtocolProviderPackages(@Nullable String protocolProviderPackages) {
            this.protocolProviderPackages = protocolProviderPackages;
            return this;
        }

        @Nonnull
        public Builder withQueries(@Nonnull Collection<Query> queries) {
            this.queries.clear();
            this.queries.addAll(queries);
            return this;
        }

        @Nonnull
        public RemoteServer build() throws MalformedURLException {
            return new RemoteServer(
                    computeUrl(),
                    username,
                    password,
                    protocolProviderPackages,
                    queries
            );
        }

        @Nonnull
        private JMXServiceURL computeUrl() throws MalformedURLException {
            if (url != null) return url;
            return new JMXServiceURL(null, checkNotEmpty(host), requireNonNull(port, "port has to be specified"));
        }
    }
}
