package oxy;

import org.apache.camel.*;
import org.apache.camel.component.file.GenericFileProducer;

import java.io.File;
import java.util.Map;

/**
 * Created by SBT-Funikov-YY on 05.11.2015.
 */
class MyEndpoint implements Endpoint {

    private final Endpoint endpoint;
    private final String targetDir;
    private Map<String, String> dirIds;

    public MyEndpoint(Map<String, String> dirIds, Endpoint endpoint, String targetDir) {
        this.endpoint = endpoint;
        this.targetDir = targetDir;
        this.dirIds = dirIds;
    }

    @Override
    public String getEndpointUri() {
//                return endpoint.getEndpointUri();
        return endpoint.getEndpointUri() + "&" + dirIds.get(targetDir);
    }

    @Override
    public EndpointConfiguration getEndpointConfiguration() {
        return endpoint.getEndpointConfiguration();
    }

    @Override
    public String getEndpointKey() {
        return endpoint.getEndpointKey() + "-" + dirIds.get(targetDir);
    }

    @Override
    public Exchange createExchange() {
        return endpoint.createExchange();
    }

    @Override
    public Exchange createExchange(ExchangePattern pattern) {
        return endpoint.createExchange(pattern);
    }

    @Override
    public Exchange createExchange(Exchange exchange) {
        return endpoint.createExchange(exchange);
    }

    @Override
    public CamelContext getCamelContext() {
        return endpoint.getCamelContext();
    }

    @Override
    public Producer createProducer() throws Exception {
        GenericFileProducer<File> producer = (GenericFileProducer<File>) endpoint.createProducer();
        return producer;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return endpoint.createConsumer(processor);
    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        return endpoint.createPollingConsumer();
    }

    @Override
    public void configureProperties(Map<String, Object> options) {
        endpoint.configureProperties(options);
    }

    @Override
    public void setCamelContext(CamelContext context) {
        endpoint.setCamelContext(context);
    }

    @Override
    public boolean isLenientProperties() {
        return endpoint.isLenientProperties();
    }

    @Override
    public boolean isSingleton() {
        return endpoint.isSingleton();
    }

    @Override
    public void start() throws Exception {
        endpoint.start();
    }

    @Override
    public void stop() throws Exception {
        endpoint.stop();
    }

    @Override
    public String toString() {
        return getEndpointKey();
    }

    @Override
    public int hashCode() {
        return getEndpointKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getEndpointKey().equals(((Endpoint) obj).getEndpointKey());
    }
}
