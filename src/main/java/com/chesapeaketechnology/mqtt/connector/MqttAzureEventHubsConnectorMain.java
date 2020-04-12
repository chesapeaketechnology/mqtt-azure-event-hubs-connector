package com.chesapeaketechnology.mqtt.connector;

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
        final Config typesafeConfig = ConfigFactory.load("mqtt-connector");

        logger.debug("The Typesafe Configuration: {}", typesafeConfig);

        final MqttAzureConnector mqttAzureConnector = new MqttAzureConnector(typesafeConfig);

        mqttAzureConnector.connect();

        Runtime.getRuntime().addShutdownHook(new Thread(mqttAzureConnector::shutdown));
    }
}
