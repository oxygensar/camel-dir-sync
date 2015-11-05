/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package oxy;

import org.apache.camel.*;
import org.apache.camel.component.file.FileComponent;
import org.apache.camel.component.file.FileConsumer;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.spring.boot.FatJarRouter;
import org.apache.camel.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.Lifecycle;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * A spring-boot application that includes a Camel route builder to setup the Camel routes
 */
@SpringBootApplication
public class MyCamelRoute extends FatJarRouter implements Lifecycle {

    private Logger LOGGER = LoggerFactory.getLogger(MyCamelRoute.class);

    String targetDirs[] = new String[] {"a\\", "b\\"};

    String sourceDir = "source\\";

    String fileName = "*.txt";

    Map<String, String> dirIds = new HashMap<>(targetDirs.length);

    Map<String, HashSet<String>> processedFiles = new HashMap<>();

//    @Autowired
//    private CamelContext context;

    // must have a main method spring-boot can run
    public static void main(String[] args) {
        FatJarRouter.main(args);
    }

    @Override
    public void configure() throws Exception {

        List<String> targetConsumers = new ArrayList<>();

        for (String targetDir: targetDirs) {
            dirIds.put(targetDir, Integer.toHexString(targetDir.hashCode()));

            targetConsumers.add(setupRoute(sourceDir, targetDir));
        }

        String uri = MessageFormat.format("file:{0}?antInclude={1}&noop=true", sourceDir, fileName);


        addConsumers(
                from(uri).id(MessageFormat.format("filesync-{0}", sourceDir)).
                    multicast(),
                        targetConsumers)
                    .end();

        from("timer://foo?period=10&repeatCount=10000")
            .setBody().constant("Hello World")
                .bean(fileNameGenerator())
            .to("file:source/");
    }

    private Processor fileNameGenerator() {
        return new Processor() {
            int num=0;

            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setHeader(Exchange.FILE_NAME, "test"+(++num)+".txt");
            }
        };
    }

    private MulticastDefinition addConsumers(MulticastDefinition multicast, List<String> targetConsumers) {
        for (String consumer: targetConsumers) {
            multicast = multicast.to(consumer);
        }

        return multicast;
    }

    private String setupRoute(String sourceDir, final String targetDir) throws Exception {

//
//        FileComponent fileComponent = new FileComponent(getContext());
//
//        final Endpoint endpoint = fileComponent.createEndpoint(uri);
//
//        Endpoint wrappingEndpoint = new MyEndpoint(endpoint, targetDir);
//
//        LOGGER.info("{}, {} {} {}", Integer.toHexString(System.identityHashCode(endpoint)),  endpoint.getEndpointKey(), endpoint.getEndpointUri(), endpoint.toString());
//
//LOGGER.info("{} {} {} {}", Integer.toHexString(System.identityHashCode(wrappingEndpoint)), wrappingEndpoint.getEndpointKey(), wrappingEndpoint.getEndpointUri(), wrappingEndpoint.toString());

        String sourceUri = MessageFormat.format("direct:{0}", targetDir);

        String targetUri = MessageFormat.format("file:{0}", targetDir);

        from(sourceUri).id(MessageFormat.format("filesync-{0}-{1}", sourceDir, targetDir)).to(targetUri).bean(removeIfAllDone(targetDir));

        return  sourceUri;
    }

    private Processor removeIfAllDone(final String targetDir) {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                String fileName = (String) exchange.getIn().getHeader(Exchange.FILE_NAME);
                String filePath = (String) exchange.getIn().getHeader("CamelFileAbsolutePath");

                synchronized (processedFiles) {
                    HashSet<String> fileProcessedBy = processedFiles.get(fileName);

                    if (fileProcessedBy == null) {
                        fileProcessedBy = new HashSet<>();

                        processedFiles.put(fileName, fileProcessedBy);
                    }

                    fileProcessedBy.add(targetDir);

                    boolean canRemove = true;
                    for (String targetDir : targetDirs) {
                        canRemove = fileProcessedBy.contains(targetDir);
                        if (!canRemove) {
                            LOGGER.info("file {} can't be removed because not yet processed by {}", fileName, targetDir);
                            break;
                        }
                    }

                    if (canRemove) {
                        FileUtil.deleteFile(new File(filePath));
                        LOGGER.info("file {} removed", fileName);
                    }
                }
            }
        };
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    private class MyEndpoint implements Endpoint {

        private final Endpoint endpoint;
        private final String targetDir;

        public MyEndpoint(Endpoint endpoint, String targetDir) {
            this.endpoint = endpoint;
            this.targetDir = targetDir;
        }

        @Override
        public String getEndpointUri() {
//                return endpoint.getEndpointUri();
            return endpoint.getEndpointUri()+"&"+dirIds.get(targetDir);
        }

        @Override
        public EndpointConfiguration getEndpointConfiguration() {
            return endpoint.getEndpointConfiguration();
        }

        @Override
        public String getEndpointKey() {
            return endpoint.getEndpointKey()+"-"+dirIds.get(targetDir);
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
            return getEndpointKey().equals(((Endpoint)obj).getEndpointKey());
        }
    }
}
