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
import org.apache.camel.builder.RouteBuilder;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * A spring-boot application that includes a Camel route builder to setup the Camel routes
 */
@SpringBootApplication
public class MyCamelRoute extends FatJarRouter {

    // must have a main method spring-boot can run
    public static void main(String[] args) {
        FatJarRouter.main(args);
    }


    @Configuration
    public static class SyncDirectoryRoutesConfiguration {

        private String sourceDir = "source\\";

        private String fileName = "*.txt";

        private String targetDirs[] = new String[] {"a\\", "b\\"};

        @Bean
        public SyncDirectoryRoutes syncDirectoryRoutes() {
            return new SyncDirectoryRoutes(sourceDir, fileName, targetDirs);
        }

        @Bean
        public RouteBuilder testFilesGenerator() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("timer://foo?period=10&repeatCount=100")
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

            };
        }
    }

}
