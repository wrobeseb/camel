/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.javaspace;

import junit.framework.Assert;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version 
 */
public class JavaSpaceSendReceiveTest extends CamelTestSupport {

    private ClassPathXmlApplicationContext spring;

    @Test
    public void testJavaSpaceSendReceive() throws Exception {
        Endpoint directEndpoint = context.getEndpoint("direct:input");
        Exchange exchange = directEndpoint.createExchange(ExchangePattern.InOnly);
        Message message = exchange.getIn();
        org.apache.camel.component.javaspace.TestEntry entry = new org.apache.camel.component.javaspace.TestEntry();
        entry.id = 1;
        entry.content = "DAVID";
        message.setBody(entry);
        Producer producer = directEndpoint.createProducer();
        int nummsg = 1000;
        MockEndpoint resultEndpoint = context.getEndpoint("mock:mymock", MockEndpoint.class);
        resultEndpoint.expectedMessageCount(nummsg);
        long start = System.currentTimeMillis();
        producer.start();
        for (int i = 0; i < nummsg; ++i) {
            producer.process(exchange);
        }
        resultEndpoint.assertIsSatisfied();
        long stop = System.currentTimeMillis();
        System.out.println(stop - start);
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        spring = new ClassPathXmlApplicationContext("org/apache/camel/component/javaspace/spring.xml");
        SpringCamelContext ctx = SpringCamelContext.springCamelContext(spring);
        return ctx;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {
            public void configure() {
                from("direct:input").to("javaspace:jini://localhost?spaceName=mySpace");
                from("javaspace:jini://localhost?spaceName=mySpace&templateId=template&verb=take&concurrentConsumers=1")
                        .process(new Processor() {
                            public void process(Exchange exc) throws Exception {
                                TestEntry msg = exc.getIn().getBody(TestEntry.class);
                                Assert.assertTrue(msg.id == 1);
                            }
                        }).to("mock:mymock");
            }
        };
    }
}
