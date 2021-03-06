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
package org.apache.pulsar.admin.cli;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.pulsar.client.admin.PersistentTopics;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.impl.BatchMessageIdImpl;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.apache.pulsar.common.compaction.CompactionStatus;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.CommaParameterSplitter;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

@Parameters(commandDescription = "Operations on persistent topics")
public class CmdPersistentTopics extends CmdBase {
    private final PersistentTopics persistentTopics;

    public CmdPersistentTopics(PulsarAdmin admin) {
        super("persistent", admin);
        persistentTopics = admin.persistentTopics();

        jcommander.addCommand("list", new ListCmd());
        jcommander.addCommand("list-partitioned-topics", new PartitionedTopicListCmd());
        jcommander.addCommand("permissions", new Permissions());
        jcommander.addCommand("grant-permission", new GrantPermissions());
        jcommander.addCommand("revoke-permission", new RevokePermissions());
        jcommander.addCommand("lookup", new Lookup());
        jcommander.addCommand("bundle-range", new GetBundleRange());
        jcommander.addCommand("delete", new DeleteCmd());
        jcommander.addCommand("unload", new UnloadCmd());
        jcommander.addCommand("subscriptions", new ListSubscriptions());
        jcommander.addCommand("unsubscribe", new DeleteSubscription());
        jcommander.addCommand("create-subscription", new CreateSubscription());
        jcommander.addCommand("stats", new GetStats());
        jcommander.addCommand("stats-internal", new GetInternalStats());
        jcommander.addCommand("info-internal", new GetInternalInfo());
        jcommander.addCommand("partitioned-stats", new GetPartitionedStats());
        jcommander.addCommand("skip", new Skip());
        jcommander.addCommand("skip-all", new SkipAll());
        jcommander.addCommand("expire-messages", new ExpireMessages());
        jcommander.addCommand("expire-messages-all-subscriptions", new ExpireMessagesForAllSubscriptions());
        jcommander.addCommand("create-partitioned-topic", new CreatePartitionedCmd());
        jcommander.addCommand("update-partitioned-topic", new UpdatePartitionedCmd());
        jcommander.addCommand("get-partitioned-topic-metadata", new GetPartitionedTopicMetadataCmd());
        jcommander.addCommand("delete-partitioned-topic", new DeletePartitionedCmd());
        jcommander.addCommand("peek-messages", new PeekMessages());
        jcommander.addCommand("reset-cursor", new ResetCursor());
        jcommander.addCommand("terminate", new Terminate());
        jcommander.addCommand("compact", new Compact());
        jcommander.addCommand("compaction-status", new CompactionStatusCmd());
    }

    @Parameters(commandDescription = "Get the list of topics under a namespace.")
    private class ListCmd extends CliCommand {
        @Parameter(description = "property/cluster/namespace\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String namespace = validateNamespace(params);
            print(persistentTopics.getList(namespace));
        }
    }

    @Parameters(commandDescription = "Get the list of partitioned topics under a namespace.")
    private class PartitionedTopicListCmd extends CliCommand {
        @Parameter(description = "property/cluster/namespace\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String namespace = validateNamespace(params);
            print(persistentTopics.getPartitionedTopicList(namespace));
        }
    }

    @Parameters(commandDescription = "Grant a new permission to a client role on a single topic.")
    private class GrantPermissions extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = "--role", description = "Client role to which grant permissions", required = true)
        private String role;

        @Parameter(names = "--actions", description = "Actions to be granted (produce,consume)", required = true, splitter = CommaParameterSplitter.class)
        private List<String> actions;

        @Override
        void run() throws PulsarAdminException {
            String topic = validateTopicName(params);
            persistentTopics.grantPermission(topic, role, getAuthActions(actions));
        }
    }

    @Parameters(commandDescription = "Revoke permissions on a topic \n "
            + "\t\t\t   Revoke permissions to a client role on a single topic. If the permission \n"
            + "\t\t\t   was not set at the topic level, but rather at the namespace level, this \n"
            + "\t\t\t   operation will return an error (HTTP status code 412).")
    private class RevokePermissions extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = "--role", description = "Client role to which revoke permissions", required = true)
        private String role;

        @Override
        void run() throws PulsarAdminException {
            String topic = validateTopicName(params);
            persistentTopics.revokePermissions(topic, role);
        }
    }

    @Parameters(commandDescription = "Get the permissions on a topic\n"
            + "\t\t     Retrieve the effective permissions for a topic. These permissions are defined \n"
            + "\t\t     by the permissions set at the namespace level combined (union) with any eventual \n"
            + "\t\t     specific permission set on the topic.")
    private class Permissions extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String topic = validateTopicName(params);
            print(persistentTopics.getPermissions(topic));
        }
    }

    @Parameters(commandDescription = "Lookup a topic from the current serving broker")
    private class Lookup extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String topic = validateTopicName(params);
            print(admin.lookups().lookupTopic(topic));
        }
    }

    @Parameters(commandDescription = "Get Namespace bundle range of a topic")
    private class GetBundleRange extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String topic = validateTopicName(params);
            print(admin.lookups().getBundleRange(topic));
        }
    }

    @Parameters(commandDescription = "Create a partitioned topic. \n"
            + "\t\tThe partitioned topic has to be created before creating a producer on it.")
    private class CreatePartitionedCmd extends CliCommand {

        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-p",
                "--partitions" }, description = "Number of partitions for the topic", required = true)
        private int numPartitions;

        @Override
        void run() throws Exception {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.createPartitionedTopic(persistentTopic, numPartitions);
        }
    }

    @Parameters(commandDescription = "Update existing non-global partitioned topic. \n"
            + "\t\tNew updating number of partitions must be greater than existing number of partitions.")
    private class UpdatePartitionedCmd extends CliCommand {

        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-p",
                "--partitions" }, description = "Number of partitions for the topic", required = true)
        private int numPartitions;

        @Override
        void run() throws Exception {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.updatePartitionedTopic(persistentTopic, numPartitions);
        }
    }

    @Parameters(commandDescription = "Get the partitioned topic metadata. \n"
            + "\t\tIf the topic is not created or is a non-partitioned topic, it returns empty topic with 0 partitions")
    private class GetPartitionedTopicMetadataCmd extends CliCommand {

        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws Exception {
            String persistentTopic = validatePersistentTopic(params);
            print(persistentTopics.getPartitionedTopicMetadata(persistentTopic));
        }
    }

    @Parameters(commandDescription = "Delete a partitioned topic. \n"
            + "\t\tIt will also delete all the partitions of the topic if it exists.")
    private class DeletePartitionedCmd extends CliCommand {

        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws Exception {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.deletePartitionedTopic(persistentTopic);
        }
    }

    @Parameters(commandDescription = "Delete a topic. \n"
            + "\t\tThe topic cannot be deleted if there's any active subscription or producers connected to it.")
    private class DeleteCmd extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.delete(persistentTopic);
        }
    }

    @Parameters(commandDescription = "Unload a topic. \n")
    private class UnloadCmd extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.unload(persistentTopic);
        }
    }

    @Parameters(commandDescription = "Get the list of subscriptions on the topic")
    private class ListSubscriptions extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws Exception {
            String persistentTopic = validatePersistentTopic(params);
            print(persistentTopics.getSubscriptions(persistentTopic));
        }
    }

    @Parameters(commandDescription = "Delete a durable subscriber from a topic. \n"
            + "\t\tThe subscription cannot be deleted if there are any active consumers attached to it \n")
    private class DeleteSubscription extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s", "--subscription" }, description = "Subscription to be deleted", required = true)
        private String subName;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.deleteSubscription(persistentTopic, subName);
        }
    }

    @Parameters(commandDescription = "Get the stats for the topic and its connected producers and consumers. \n"
            + "\t       All the rates are computed over a 1 minute window and are relative the last completed 1 minute period.")
    private class GetStats extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            print(persistentTopics.getStats(persistentTopic));
        }
    }

    @Parameters(commandDescription = "Get the internal stats for the topic")
    private class GetInternalStats extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            print(persistentTopics.getInternalStats(persistentTopic));
        }
    }

    @Parameters(commandDescription = "Get the internal metadata info for the topic")
    private class GetInternalInfo extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            JsonObject result = persistentTopics.getInternalInfo(persistentTopic);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(result));
        }
    }

    @Parameters(commandDescription = "Get the stats for the partitioned topic and its connected producers and consumers. \n"
            + "\t       All the rates are computed over a 1 minute window and are relative the last completed 1 minute period.")
    private class GetPartitionedStats extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic\n", required = true)
        private java.util.List<String> params;

        @Parameter(names = "--per-partition", description = "Get per partition stats")
        private boolean perPartition = false;

        @Override
        void run() throws Exception {
            String persistentTopic = validatePersistentTopic(params);
            print(persistentTopics.getPartitionedStats(persistentTopic, perPartition));
        }
    }

    @Parameters(commandDescription = "Skip all the messages for the subscription")
    private class SkipAll extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s", "--subscription" }, description = "Subscription to be cleared", required = true)
        private String subName;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.skipAllMessages(persistentTopic, subName);
        }
    }

    @Parameters(commandDescription = "Skip some messages for the subscription")
    private class Skip extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s",
                "--subscription" }, description = "Subscription to be skip messages on", required = true)
        private String subName;

        @Parameter(names = { "-n", "--count" }, description = "Number of messages to skip", required = true)
        private long numMessages;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.skipMessages(persistentTopic, subName, numMessages);
        }
    }

    @Parameters(commandDescription = "Expire messages that older than given expiry time (in seconds) for the subscription")
    private class ExpireMessages extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s",
                "--subscription" }, description = "Subscription to be skip messages on", required = true)
        private String subName;

        @Parameter(names = { "-t", "--expireTime" }, description = "Expire messages older than time in seconds", required = true)
        private long expireTimeInSeconds;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.expireMessages(persistentTopic, subName, expireTimeInSeconds);
        }
    }

    @Parameters(commandDescription = "Expire messages that older than given expiry time (in seconds) for all subscriptions")
    private class ExpireMessagesForAllSubscriptions extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-t", "--expireTime" }, description = "Expire messages older than time in seconds", required = true)
        private long expireTimeInSeconds;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            persistentTopics.expireMessagesForAllSubscriptions(persistentTopic, expireTimeInSeconds);
        }
    }

    @Parameters(commandDescription = "Create a new subscription on a topic")
    private class CreateSubscription extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s",
                "--subscription" }, description = "Subscription to reset position on", required = true)
        private String subscriptionName;

        @Parameter(names = { "--messageId",
                "-m" }, description = "messageId where to create the subscription. It can be either 'latest', 'earliest' or (ledgerId:entryId)", required = false)
        private String messageIdStr = "latest";

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            MessageId messageId;
            if (messageIdStr.equals("latest")) {
                messageId = MessageId.latest;
            } else if (messageIdStr.equals("earliest")) {
                messageId = MessageId.earliest;
            } else {
                messageId = validateMessageIdString(messageIdStr);
            }

            persistentTopics.createSubscription(persistentTopic, subscriptionName, messageId);
        }
    }

    @Parameters(commandDescription = "Reset position for subscription to position closest to timestamp or messageId")
    private class ResetCursor extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s",
                "--subscription" }, description = "Subscription to reset position on", required = true)
        private String subName;

        @Parameter(names = { "--time",
                "-t" }, description = "time in minutes to reset back to (or minutes, hours,days,weeks eg: 100m, 3h, 2d, 5w)", required = false)
        private String resetTimeStr;

        @Parameter(names = { "--messageId",
                "-m" }, description = "messageId to reset back to (ledgerId:entryId)", required = false)
        private String resetMessageIdStr;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            if (isNotBlank(resetMessageIdStr)) {
                MessageId messageId = validateMessageIdString(resetMessageIdStr);
                persistentTopics.resetCursor(persistentTopic, subName, messageId);
            } else if (isNotBlank(resetTimeStr)) {
                int resetBackTimeInMin = validateTimeString(resetTimeStr);
                long resetTimeInMillis = TimeUnit.MILLISECONDS.convert(resetBackTimeInMin, TimeUnit.MINUTES);
                // now - go back time
                long timestamp = System.currentTimeMillis() - resetTimeInMillis;
                persistentTopics.resetCursor(persistentTopic, subName, timestamp);
            } else {
                throw new PulsarAdminException(
                        "Either Timestamp (--time) or Position (--position) has to be provided to reset cursor");
            }
        }
    }

    @Parameters(commandDescription = "Terminate a topic and don't allow any more messages to be published")
    private class Terminate extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);

            try {
                MessageId lastMessageId = persistentTopics.terminateTopicAsync(persistentTopic).get();
                System.out.println("Topic succesfully terminated at " + lastMessageId);
            } catch (InterruptedException | ExecutionException e) {
                throw new PulsarAdminException(e);
            }
        }
    }

    @Parameters(commandDescription = "Peek some messages for the subscription")
    private class PeekMessages extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-s",
                "--subscription" }, description = "Subscription to get messages from", required = true)
        private String subName;

        @Parameter(names = { "-n", "--count" }, description = "Number of messages (default 1)", required = false)
        private int numMessages = 1;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);
            List<Message<byte[]>> messages = persistentTopics.peekMessages(persistentTopic, subName, numMessages);
            int position = 0;
            for (Message<byte[]> msg : messages) {
                if (++position != 1) {
                    System.out.println("-------------------------------------------------------------------------\n");
                }
                if (msg.getMessageId() instanceof BatchMessageIdImpl) {
                    BatchMessageIdImpl msgId = (BatchMessageIdImpl) msg.getMessageId();
                    System.out.println("Batch Message ID: " + msgId.getLedgerId() + ":" + msgId.getEntryId() + ":" + msgId.getBatchIndex());
                } else {
                    MessageIdImpl msgId = (MessageIdImpl) msg.getMessageId();
                    System.out.println("Message ID: " + msgId.getLedgerId() + ":" + msgId.getEntryId());
                }
                if (msg.getProperties().size() > 0) {
                    System.out.println("Tenants:");
                    print(msg.getProperties());
                }
                ByteBuf data = Unpooled.wrappedBuffer(msg.getData());
                System.out.println(ByteBufUtil.prettyHexDump(data));
            }
        }
    }

    @Parameters(commandDescription = "Compact a topic")
    private class Compact extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);

            persistentTopics.triggerCompaction(persistentTopic);
            System.out.println("Topic compaction requested for " + persistentTopic);
        }
    }

    @Parameters(commandDescription = "Status of compaction on a topic")
    private class CompactionStatusCmd extends CliCommand {
        @Parameter(description = "persistent://property/cluster/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-w", "--wait-complete" },
                   description = "Wait for compaction to complete", required = false)
        private boolean wait = false;

        @Override
        void run() throws PulsarAdminException {
            String persistentTopic = validatePersistentTopic(params);

            try {
                CompactionStatus status = persistentTopics.compactionStatus(persistentTopic);
                while (wait && status.status == CompactionStatus.Status.RUNNING) {
                    Thread.sleep(1000);
                    status = persistentTopics.compactionStatus(persistentTopic);
                }

                switch (status.status) {
                case NOT_RUN:
                    System.out.println("Compaction has not been run for " + persistentTopic
                                       + " since broker startup");
                    break;
                case RUNNING:
                    System.out.println("Compaction is currently running");
                    break;
                case SUCCESS:
                    System.out.println("Compaction was a success");
                    break;
                case ERROR:
                    System.out.println("Error in compaction");
                    throw new PulsarAdminException("Error compacting: " + status.lastError);
                }
            } catch (InterruptedException e) {
                throw new PulsarAdminException(e);
            }
        }
    }

    private static int validateTimeString(String s) {
        char last = s.charAt(s.length() - 1);
        String subStr = s.substring(0, s.length() - 1);
        switch (last) {
        case 'm':
        case 'M':
            return Integer.parseInt(subStr);

        case 'h':
        case 'H':
            return Integer.parseInt(subStr) * 60;

        case 'd':
        case 'D':
            return Integer.parseInt(subStr) * 24 * 60;

        case 'w':
        case 'W':
            return Integer.parseInt(subStr) * 7 * 24 * 60;

        default:
            return Integer.parseInt(s);
        }
    }

    private MessageId validateMessageIdString(String resetMessageIdStr) throws PulsarAdminException {
        String[] messageId = resetMessageIdStr.split(":");
        try {
            checkArgument(messageId.length == 2);
            return new MessageIdImpl(Long.parseLong(messageId[0]), Long.parseLong(messageId[1]), -1);
        } catch (Exception e) {
            throw new PulsarAdminException(
                    "Invalid reset-position (must be in format: ledgerId:entryId) value " + resetMessageIdStr);
        }
    }
}
