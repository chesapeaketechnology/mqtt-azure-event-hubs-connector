package com.chesapeaketechnology.mqtt.connector;

/**
 * Constants for the MQTT to Azure Event Hubs Connector.
 *
 * @since 0.1.0
 */
public final class ConnectorConstants
{
    private ConnectorConstants()
    {
    }

    /**
     * The file name (without the extension) of the Typesafe configuration that contains the MQTT Connector settings.
     */
    public static final String MQTT_CONNECTOR_CONFIG_FILE_NAME = "mqtt-connector";

    /**
     * The config value key for the MQTT Server URI.  This constant can be used to pull the server URI string out of the
     * Typesafe config file.
     */
    public static final String MQTT_SERVER_KEY = "connector-config.mqtt-server";

    /**
     * The config value key for the MQTT Server URI.  This constant can be used to pull the server URI string out of the
     * Typesafe config file.
     */
    public static final String MQTT_TOPICS_KEY = "connector-config.mqtt-topics";
    public static final String MQTT_USERNAME_KEY = "connector-config.mqtt-username";
    public static final String MQTT_PASSWORD_KEY = "connector-config.mqtt-password";

    /**
     * The config value key for the Azure Event Hubs Connection String.  This constant can be used to pull the
     * connection string out of the Typesafe config file.
     */
    public static final String AZURE_EVENT_HUBS_CONNECTION_STRING_KEY = "connector-config.azure-event-hubs-connection-string";

    /**
     * The config value key for the Azure Event Hubs schedule interval.  This constant can be used to pull the
     * scheduling interval in milliseconds out of the Typesafe config file.
     * <p>
     * The Scheduling interval defines how often the MQTT message queue will be emptied out and then sent in batches to
     * the appropriate Azure Event Hub.  This value represents the delay between the termination of one execution and
     * the commencement of the next.
     */
    public static final String AZURE_EVENT_HUBS_SCHEDULE_INTERVAL_KEY = "connector-config.scheduled-interval-ms";

    /**
     * The config value key for the Azure Event Hubs Batch Size.  This constant can be used to pull the
     * batch size out of the Typesafe config file.
     * <p>
     * The batch size defines the number of messages that will be included in one batch of event data messages sent via
     * a producer to an Azure Event Hub.  The limit to this value is how many messages the Azure transport can support.
     */
    public static final String AZURE_EVENT_HUBS_BATCH_SIZE_KEY = "connector-config.azure-event-hubs-batch-size";

    /**
     * The ID that this java application uses as its client identifier to the MQTT Broker.
     */
    public static final String MQTT_CLIENT_ID = "MQTT_Azure_Event_Hubs_Connector";

    /**
     * When the {@link com.azure.messaging.eventhubs.EventHubProducerClient} runs and pulls all the messages from the
     * MQTT message queue, if the number of messages is larger than this constant, then a warning will be logged.
     */
    public static final int MQTT_MESSAGE_QUEUE_WARNING_SIZE = 1000;

    /**
     * Delimiter character for the list of topics configuration.
     */
    public static final String MQTT_TOPIC_LIST_DELIMITER = ",";
}
