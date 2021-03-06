/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.functions.instance.processors;

import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.bookkeeper.common.annotation.InterfaceStability.Evolving;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.functions.api.SerDe;
import org.apache.pulsar.functions.instance.InputMessage;
import org.apache.pulsar.functions.proto.Function.FunctionDetails;
import org.apache.pulsar.functions.proto.Function.FunctionDetails.ProcessingGuarantees;

/**
 * A processor that processes messages, used by {@link org.apache.pulsar.functions.instance.JavaInstanceRunnable}.
 */
@Evolving
public interface MessageProcessor extends AutoCloseable {

    static MessageProcessor create(PulsarClient client,
                                   FunctionDetails functionDetails) {
        FunctionDetails.SubscriptionType fnSubType = functionDetails.getSubscriptionType();
        ProcessingGuarantees processingGuarantees = functionDetails.getProcessingGuarantees();
        SubscriptionType subType;
        if (FunctionDetails.SubscriptionType.SHARED == fnSubType) {
            subType = SubscriptionType.Shared;
        } else if (FunctionDetails.SubscriptionType.EXCLUSIVE == fnSubType) {
            subType = SubscriptionType.Exclusive;
        } else {
            subType = SubscriptionType.Failover;
        }

        if (processingGuarantees == ProcessingGuarantees.EFFECTIVELY_ONCE) {
            return new EffectivelyOnceProcessor(
                client,
                functionDetails);
        } else if (processingGuarantees == ProcessingGuarantees.ATMOST_ONCE) {
            return new AtMostOnceProcessor(
                client,
                functionDetails,
                subType);
        } else {
            return new AtLeastOnceProcessor(
                client,
                functionDetails,
                subType);
        }
    }

    /**
     * Setup the input with a provided <i>processQueue</i>. The implementation of this processor is responsible for
     * setting up the input and passing the received messages from input to the provided <i>processQueue</i>.
     *
     * @param inputSerDe SerDe to deserialize messages from input.
     */
    void setupInput(Map<String, SerDe> inputSerDe)
        throws Exception;

    /**
     * Return the input.
     *
     * @return the input consumer.
     */
    Consumer getInputConsumer();

    /**
     * Setup the output with a provided <i>outputSerDe</i>. The implementation of this processor is responsible for
     * setting up the output
     *
     * @param outputSerDe output serde.
     * @throws Exception
     */
    void setupOutput(SerDe outputSerDe) throws Exception;

    /**
     * Send the output message to the output topic. The output message is computed from <i>inputMsg</i>.
     *
     * <p>If the <i>outputMsgBuilder</i> is null, the implementation doesn't have to send any messages to the output.
     * The implementation can decide to acknowledge the input message based on its process guarantees.
     *
     * @param inputMsg input message
     * @param outputMsgBuilder output message builder. it can be null.
     */
    void sendOutputMessage(InputMessage inputMsg,
                           MessageBuilder outputMsgBuilder) throws PulsarClientException, Exception;

    /**
     * Get the next message to process
     * @return the next input message
     * @throws Exception
     */
    InputMessage recieveMessage() throws Exception;

    @Override
    void close();

}
