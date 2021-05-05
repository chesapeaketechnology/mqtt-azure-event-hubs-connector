package com.chesapeaketechnology.mqtt.connector;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.chesapeaketechnology.mqtt.connector.ConnectorConstants.*;

/**
 * This class handles most of the logic for connecting an MQTT Broker to Azure Event Hubs.  It is responsible for
 * reading the Typesafe configuration and then using those values to establish a connection to an MQTT broker.  The
 * configured topics are subscribed to, and then an {@link com.azure.messaging.eventhubs.EventHubProducerClient} is
 * created for each topic.  As messages come in from the MQTT broker, they are passed to the
 * {@link com.azure.messaging.eventhubs.EventHubProducerClient} to be sent to the appropriate event hub.
 *
 * @since 0.1.0
 */
public class MqttAzureConnector
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String mqttServerUri;
    private final Set<String> mqttTopics;
    private final String mqttUsername;
    private final String mqttPassword;
    private final String connectionString;
    private final int batchSize;
    private final int connectorExecutionIntervalMs;
    private IMqttClient mqttClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<String, EventHubTopicProducer> topicToEventHubProducerMap = new ConcurrentHashMap<>();

    /**
     * Constructs an instance of this class, and reads the provided Typesafe configuration for all the settings.
     * <p>
     * After creating an instance, call {@link #connect()} to establish a connection to the MQTT server and Azure Event
     * Hubs.
     * <p>
     * To stop this connector call {@link #shutdown()}.
     *
     * @param typesafeConfig The configuration that defines how this connector works.
     * @throws ConfigException.Missing If one of the configuration values is missing.
     */
    MqttAzureConnector(Config typesafeConfig) throws ConfigException.Missing
    {
        mqttServerUri = typesafeConfig.getString(ConnectorConstants.MQTT_SERVER_KEY);
        mqttTopics = new HashSet<>(Arrays.asList(typesafeConfig.getString(ConnectorConstants.MQTT_TOPICS_KEY).split(MQTT_TOPIC_LIST_DELIMITER)));
        mqttUsername = typesafeConfig.getString(ConnectorConstants.MQTT_USERNAME_KEY);
        mqttPassword = getPasswordForUser(typesafeConfig);
        connectionString = typesafeConfig.getString(ConnectorConstants.AZURE_EVENT_HUBS_CONNECTION_STRING_KEY);
        batchSize = typesafeConfig.getInt(ConnectorConstants.AZURE_EVENT_HUBS_BATCH_SIZE_KEY);
        connectorExecutionIntervalMs = typesafeConfig.getInt(ConnectorConstants.AZURE_EVENT_HUBS_SCHEDULE_INTERVAL_KEY);

        // The empty check should be covered when extracting the list using getStringList, but double checking to be safe.
        if (mqttTopics.isEmpty())
        {
            logger.error("The list of MQTT Topics to subscribe to was empty.  At least one topic must be specified");
            throw new ConfigException.Missing("The list of MQTT Topics to subscribe to was empty.  At least one topic must be specified in the config file.");
        }

        scheduledExecutorService = Executors.newScheduledThreadPool(mqttTopics.size());
    }

    /**
     * Gets the mosquitto password to connect to the MQTT broker with. If the password is present in the
     * config file, it's used for the connection. Otherwise, it is read from Consul.
     */
    private String getPasswordForUser(Config typesafeConfig)
    {
        String password = "";
        try
        {
            password = typesafeConfig.getString(ConnectorConstants.MQTT_PASSWORD_KEY);
        } catch (ConfigException configException)
        {
            String usernameKey = "mqtt/user/" + mqttUsername;
            KeyValueClient kvClient = Consul.builder().build().keyValueClient();

            do // wait for Consul to become available
            {
                try
                {
                    logger.info("Reading the password for user {} from Consul.", mqttUsername);
                    password = kvClient.getValueAsString(usernameKey).orElseThrow();
                    Thread.sleep(2000);
                } catch (Exception e)
                {
                    logger.warn("Not able to reach Consul. Retrying ...", e);
                }
            } while (password.isEmpty());
        }
        return password;
    }

    /**
     * This method performs the following tasks:
     * <p>
     * 1. Establishes a connection to the MQTT Broker.
     * 2. Creates an {@link com.azure.messaging.eventhubs.EventHubProducerClient} for each topic from the config file.
     * 3. Subscribes to each topic on the MQTT broker.
     */
    void connect()
    {
        // TODO MqttClientPersistence mqttClientPersistence
        try
        {
            logger.info("Connecting to the MQTT Broker at {}", mqttServerUri);
            mqttClient = new MqttClient(mqttServerUri, ConnectorConstants.MQTT_CLIENT_ID);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false); // Maintain state between connections
            options.setConnectionTimeout(0); // Wait for the connection with no timeout
            options.setUserName(mqttUsername);
            options.setPassword(mqttPassword.toCharArray());
            mqttClient.connect(options);

            logger.info("Successfully connected to the MQTT broker, starting to create the Event Hub connections");

            mqttTopics.forEach(messageTopic -> {
                final EventHubTopicProducer eventHubTopicProducer = new EventHubTopicProducer(connectionString,
                        messageTopic, scheduledExecutorService, batchSize, connectorExecutionIntervalMs);
                topicToEventHubProducerMap.put(messageTopic, eventHubTopicProducer);
                try
                {
                    if (eventHubTopicProducer.connect())
                    {
                        mqttClient.subscribe(messageTopic, 2, (topic, message) -> eventHubTopicProducer.queueNewMessage(message));
                    }
                } catch (MqttException e)
                {
                    logger.error("Could not subscribe to an MQTT topic {}", messageTopic, e);
                }
            });

            logger.info("Finished connecting to the MQTT Broker");
        } catch (MqttException e)
        {
            logger.error("An exception occurred when connecting to the MQTT Broker", e);
        }
    }

    /**
     * Closes the connection to the MQTT broker, and then lets the scheduled jobs finish running so that the local MQTT
     * message queue is flushed out and those messages are pushed to the appropriate Event Hub.
     * <p>
     * If the scheduled jobs can't finish after a timeout period, then the jobs are canceled.
     */
    void shutdown()
    {
        try
        {
            logger.info("Shutting down the MQTT Connection and stopping the scheduled executor service");

            mqttClient.disconnect();
            mqttClient.close();

            scheduledExecutorService.shutdown();
            scheduledExecutorService.awaitTermination(120, TimeUnit.SECONDS);

            topicToEventHubProducerMap.forEach((topic, eventHubTopicProducer) -> eventHubTopicProducer.disconnect());

            logger.info("Shutdown complete!");
        } catch (Exception e)
        {
            logger.error("An error occurred when trying to shutdown the MQTT Connection", e);
        }
    }
}
