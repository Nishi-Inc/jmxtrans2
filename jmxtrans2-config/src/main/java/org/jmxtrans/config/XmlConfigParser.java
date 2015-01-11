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
package org.jmxtrans.config;

import org.jmxtrans.config.jaxb.InvocationType;
import org.jmxtrans.config.jaxb.Jmxtrans;
import org.jmxtrans.config.jaxb.OutputWriterType;
import org.jmxtrans.config.jaxb.QueryType;
import org.jmxtrans.output.OutputWriter;
import org.jmxtrans.output.OutputWriterFactory;
import org.jmxtrans.query.Invocation;
import org.jmxtrans.query.embedded.Query;
import org.jmxtrans.query.embedded.QueryAttribute;
import org.jmxtrans.utils.circuitbreaker.CircuitBreakerProxy;
import org.jmxtrans.utils.io.Resource;
import org.jmxtrans.utils.time.Clock;
import org.jmxtrans.utils.time.Interval;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

@ThreadSafe
public class XmlConfigParser implements ConfigParser {

    public static final String JMXTRANS_XSD_PATH = "classpath:jmxtrans.xsd";

    public static final int MAX_FAILURES = 5;
    public static final int DISABLE_DURATION_MILLIS = 60 * 1000;

    private final Clock clock;
    private final PropertyPlaceholderResolverXmlPreprocessor preprocessor;
    private final StandardConfiguration configuration = new StandardConfiguration(DefaultConfiguration.getInstance());
    private final DocumentBuilder documentBuilder;
    private final Unmarshaller unmarshaller;
    private Resource source;

    private XmlConfigParser(
            @Nonnull DocumentBuilder documentBuilder,
            @Nonnull Unmarshaller unmarshaller,
            @Nonnull PropertyPlaceholderResolverXmlPreprocessor preprocessor,
            @Nonnull Clock clock) {
        this.documentBuilder = documentBuilder;
        this.unmarshaller = unmarshaller;
        this.preprocessor = preprocessor;
        this.clock = clock;
    }

    @Override
    public synchronized void setSource(@Nonnull Resource source) throws IOException, SAXException, JAXBException {
        this.source = source;
    }

    private void parse(@Nonnull Jmxtrans jmxtrans, @Nonnull TemporaryConfiguration configuration) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        if (jmxtrans.getQueries() != null) {
            parse(jmxtrans.getQueries(), configuration);
        }
        if (jmxtrans.getInvocations() != null) {
            parse(jmxtrans.getInvocations(), configuration);
        }
        if (jmxtrans.getOutputWriters() != null) {
            parse(jmxtrans.getOutputWriters(), configuration);
        }
    }

    private void parse(@Nonnull Jmxtrans.Queries queries, @Nonnull TemporaryConfiguration configuration) {
        if (queries.getCollectIntervalInSeconds() != null) {
            configuration.queryPeriod = new Interval(queries.getCollectIntervalInSeconds(), SECONDS);
        }
        for (QueryType query : queries.getQuery()) {
            Query.Builder queryBuilder = Query.builder()
                    .withObjectName(query.getObjectName())
                    .withResultAlias(query.getResultAlias());
            for (QueryType.QueryAttribute attribute : query.getQueryAttribute()) {
                QueryAttribute.Builder attributeBuilder = QueryAttribute
                        .builder(attribute.getName())
                        .withResultAlias(attribute.getResultAlias())
                        .withType(attribute.getType());
                for (String key : attribute.getKey()) {
                    attributeBuilder.addKey(key);
                }
                queryBuilder.addAttribute(attributeBuilder.build());
            }
            configuration.queries.add(queryBuilder.build());
        }
    }

    private void parse(@Nonnull Jmxtrans.Invocations invocations, @Nonnull TemporaryConfiguration configuration) {
        if (invocations.getCollectIntervalInSeconds() != null) {
            configuration.invocationPeriod = new Interval(invocations.getCollectIntervalInSeconds(), SECONDS);
        }
        for (InvocationType invocation : invocations.getInvocation()) {
            List<String> params = new ArrayList<>();
            List<String> signature = new ArrayList<>();
            for (InvocationType.Parameter parameter : invocation.getParameter()) {
                params.add(parameter.getValue());
                signature.add(parameter.getType());
            }
            configuration.invocations.add(
                    new Invocation(
                            invocation.getObjectName(),
                            invocation.getOperationName(),
                            params.toArray(),
                            signature.toArray(new String[0]),
                            invocation.getResultAlias()));
        }
    }

    private void parse(@Nonnull Jmxtrans.OutputWriters outputWriters, @Nonnull TemporaryConfiguration configuration) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        for (OutputWriterType outputWriter : outputWriters.getOutputWriter()) {
            Map<String, String> settings = new HashMap<>();
            for (Map.Entry<QName, String> attribute : outputWriter.getOtherAttributes().entrySet()) {
                settings.put(attribute.getKey().getLocalPart(), attribute.getValue());
            }
            configuration.outputWriters.add(instantiateOutputWriter(outputWriter.getClazz(), settings));
        }
    }

    @Nonnull
    private OutputWriter instantiateOutputWriter(@Nonnull String outputWriterClass, @Nonnull Map<String, String> settings)
            throws InstantiationException, IllegalAccessException {
        try {
            Class<OutputWriterFactory<?>> builderClass = (Class<OutputWriterFactory<?>>) Class.forName(outputWriterClass + "$Factory");
            OutputWriterFactory<?> builder = builderClass.newInstance();
            return CircuitBreakerProxy.create(
                    clock,
                    OutputWriter.class,
                    builder.create(settings),
                    MAX_FAILURES,
                    DISABLE_DURATION_MILLIS);
        } catch (ClassNotFoundException e) {
            throw new JmxtransConfigurationException(
                    format("Could not load class %s, this can happen if you use non standard outputwriters and did not" +
                            " add the appropriate jar on the classpath", outputWriterClass), e);
        }
    }

    @Override
    @Nonnull
    public synchronized Configuration parseConfiguration() throws IOException, SAXException, JAXBException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        try (InputStream in = source.getInputStream()) {
            Document document = documentBuilder.parse(in);
            document = preprocessor.preprocess(document);
            Jmxtrans jmxtrans = (Jmxtrans) unmarshaller.unmarshal(document);

            TemporaryConfiguration newConfiguration = new TemporaryConfiguration();

            parse(jmxtrans, newConfiguration);

            return configuration.replaceWith(newConfiguration);
        } finally {
            documentBuilder.reset();
        }
    }

    @Nonnull
    public static XmlConfigParser newInstance(
            @Nonnull PropertyPlaceholderResolverXmlPreprocessor preprocessor,
            @Nonnull Clock clock) throws JAXBException, ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        JAXBContext jaxbContext = JAXBContext.newInstance(Jmxtrans.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        unmarshaller.setSchema(loadSchema());

        return new XmlConfigParser(
                dbf.newDocumentBuilder(),
                unmarshaller,
                preprocessor,
                clock
        );
    }

    @Nonnull
    private static Schema loadSchema() throws SAXException, IOException {
        Resource xsd = new Resource(JMXTRANS_XSD_PATH);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        try (InputStream in = xsd.getInputStream()) {
            return schemaFactory.newSchema(new StreamSource(in));
        }
    }


    private static final class TemporaryConfiguration implements Configuration {

        @Nonnull
        private final Collection<Query> queries = new ArrayList<>();
        private Interval queryPeriod;
        @Nonnull
        private final Collection<OutputWriter> outputWriters = new ArrayList<>();
        @Nonnull
        private final Collection<Invocation> invocations = new ArrayList<>();
        private Interval invocationPeriod;

        @Nonnull
        @Override
        public Iterable<Query> getQueries() {
            return queries;
        }

        @Nonnull
        @Override
        public Interval getQueryPeriod() {
            if (queryPeriod == null) return DefaultConfiguration.getInstance().getQueryPeriod();
            return queryPeriod;
        }

        @Nonnull
        @Override
        public Iterable<OutputWriter> getOutputWriters() {
            return outputWriters;
        }

        @Nonnull
        @Override
        public Iterable<Invocation> getInvocations() {
            return invocations;
        }

        @Nonnull
        @Override
        public Interval getInvocationPeriod() {
            if (invocationPeriod == null) return DefaultConfiguration.getInstance().getInvocationPeriod();
            return invocationPeriod;
        }
    }
}
