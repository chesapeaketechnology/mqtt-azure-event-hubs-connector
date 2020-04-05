package com.chesapeaketechnology.mqtt.connector;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * The Main class that is use to kick off the java application that pulls messages from the MQTT Broker and pushes them
 * into Azure Event Hubs.
 *
 * @since 0.1.0
 */
public class MqttAzureEventHubsConnectorMain
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args)
    {
        logger.info("Starting the MQTT to Azure Event Hubs Connector");

        // Details on how Typesafe config works:  https://github.com/lightbend/config
        final Config typesafeConfig = ConfigFactory.load();

        logger.info("The Typesafe Configuration: {}", typesafeConfig);

        // TODO catch and log the exceptions if something goes wrong.
        final String connectionString = typesafeConfig.getString(ConnectorConstants.AZURE_EVENT_HUBS_CONNECTION_STRING_KEY);
        final String eventHubName = typesafeConfig.getString(ConnectorConstants.AZURE_EVENT_HUBS_NAME_KEY);

        try (EventHubProducerClient producer = new EventHubClientBuilder()
                .connectionString(connectionString, eventHubName)
                .buildProducerClient())
        {
            EventDataBatch batch = producer.createBatch();
            batch.tryAdd(new EventData("First event"));
            batch.tryAdd(new EventData("Second event"));
            batch.tryAdd(new EventData("Third event"));
            batch.tryAdd(new EventData("Fourth event"));
            batch.tryAdd(new EventData("Fifth event"));

            producer.send(batch);
        } catch (Exception e)
        {
            logger.error("An exception occurred when trying to run the MQTT to Azure Event Hubs Connector", e);
        }
    }
}
