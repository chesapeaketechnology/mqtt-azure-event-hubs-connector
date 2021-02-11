package com.chesapeaketechnology.mqtt.connector;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A producer that is responsible for sending messages to a specific Azure Event Hub (an Azure Event Hubs Namespace
 * contains one or more Event Hub).
 * <p>
 * It is required that the event hub with the provided name (in the constructor) already be created before this producer
 * tries to send messages to it.  If not, you might receive the message "The messaging entity 'entity name' could not be
 * found."
 *
 * @since 0.1.0
 */
public class EventHubTopicProducer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String connectionString;
    private final String eventHubName;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int connectorExecutionIntervalMs;
    private final int configuredBatchSize;

    private final ConcurrentLinkedQueue<MqttMessage> mqttMessageQueue = new ConcurrentLinkedQueue<>();
    private EventHubProducerClient producer;

    /**
     * Constructs an event hub producer for the provided event hub (defined by the {@code connectionString} +
     * {@code eventHubName}.
     *
     * @param connectionString             The Azure Event Hubs Namespace connection string
     * @param eventHubName                 The name of the event hub to send messages to.
     * @param scheduledExecutorService     An executor service that can be used to schedule processing of the MQTT
     *                                     message queue and send those messages to the Event Hub.
     * @param configuredBatchSize          The maximum number of messages to include in a batch that is sent to the Event Hub.
     * @param connectorExecutionIntervalMs The interval, in milliseconds, between executions of the message processing.
     */
    EventHubTopicProducer(String connectionString, String eventHubName,
                          ScheduledExecutorService scheduledExecutorService, int configuredBatchSize,
                          int connectorExecutionIntervalMs)
    {
        this.connectionString = connectionString;
        this.eventHubName = eventHubName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.configuredBatchSize = configuredBatchSize;
        this.connectorExecutionIntervalMs = connectorExecutionIntervalMs;
    }

    /**
     * Trigger this Event hub producer to connect to the event hub, and schedule a recurring execution that pulls and
     * sends all messages from the MQTT message queue.
     *
     * @return True if the connection was successful, false if something went wrong.
     */
    boolean connect()
    {
        try
        {
            logger.info("Connecting to the {} Event Hub", eventHubName);

            createProducer();

            scheduledExecutorService.scheduleWithFixedDelay(this::processMessageQueue,
                    connectorExecutionIntervalMs, connectorExecutionIntervalMs, TimeUnit.MILLISECONDS);
        } catch (Exception e)
        {
            logger.error("An exception occurred when trying to run the MQTT to Azure Event Hubs Connector", e);
            return false;
        }

        return true;
    }

    /**
     * Add the provided MQTT message to the queue.  The message will be sent to the Azure Event Hub on the next running
     * of the scheduled executor.
     *
     * @param mqttMessage The message to add to the queue.
     */
    void queueNewMessage(MqttMessage mqttMessage)
    {
        mqttMessageQueue.add(mqttMessage);
    }

    /**
     * Pull all the messages from the MQTT message queue (if any), and send those messages to the Azure Event hub in
     * batches.
     */
    void processMessageQueue()
    {
        try
        {
            final int queueSize = mqttMessageQueue.size();

            logger.trace("About to process {} MQTT messages from the {} queue", queueSize, eventHubName);

            if (queueSize > ConnectorConstants.MQTT_MESSAGE_QUEUE_WARNING_SIZE)
            {
                logger.warn("An MQTT message queue has grown to {} for topic/eventhub {}", queueSize, eventHubName);
            }

            final LinkedList<MqttMessage> mqttMessages = new LinkedList<>();

            // I am concerned that the messages coming into the MQTT broker will be coming so fast that if we continue
            // to loop on checking if the !mqttMessageQueue.isEmpty(), then we will continue to process one or two
            // messages and the scheduled executor job will last a lot longer than intended.  By grabbing all the
            // messages from the  queue first, this should help with making sure all but the last batch has a size equal
            // to the configured batch size.
            while (!mqttMessageQueue.isEmpty())
            {
                mqttMessages.add(mqttMessageQueue.poll());
            }

            while (!mqttMessages.isEmpty())
            {
                int batchSize = 0;
                final EventDataBatch messageBatch = producer.createBatch();

                while (batchSize < configuredBatchSize && !mqttMessages.isEmpty())
                {
                    final MqttMessage mqttMessage = mqttMessages.poll();
                    if (mqttMessage == null) break;

                    try
                    {
                        if (messageBatch.tryAdd(new EventData(mqttMessage.getPayload())))
                        {
                            batchSize++;
                        } else
                        {
                            logger.error("Could not add an MQTT message to the Azure Event Hub {} batch because the message was too large", eventHubName);
                            mqttMessages.offerFirst(mqttMessage);
                            break;
                        }
                    } catch (Exception e)
                    {
                        logger.error("Could not add an MQTT message to the Azure Event Hub {} batch because of an exception", eventHubName, e);
                        mqttMessages.offerFirst(mqttMessage);
                        break;
                    }
                }

                logger.trace("Sending {} messages to the {} Azure Event Hub", batchSize, eventHubName);

                producer.send(messageBatch);
            }
        } catch (IllegalStateException e)
        {
            logger.warn("Caught an IllegalArgumentException when trying to process the MQTT message queue, attempting to rebuild the producer connection", e);
            createProducer();
        } catch (Exception e)
        {
            logger.error("Caught an Exception when trying to process the MQTT message queue", e);
        }
    }

    /**
     * Closes the Event Hub producer which closes the connection to the Azure Event Hub.  The scheduled executor service
     * job passed into the constructor of this object must be shutdown prior to calling this method so that the producer
     * can be used to send any remaining messages to the event hub during the final running of the executor.
     */
    void disconnect()
    {
        producer.close();
    }

    /**
     * Create the Event Hub producer that is used to create a connection to the Azure Event Hub and publish messages.
     *
     * @since 0.1.5
     */
    private void createProducer()
    {
        producer = new EventHubClientBuilder()
                .connectionString(connectionString, eventHubName)
                .buildProducerClient(); // TODO Do we want an async or sync producer?
    }
}
