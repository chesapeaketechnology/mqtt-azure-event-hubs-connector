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
     * The config value key for the Azure Event Hubs Connection String.  This constant can be used to pull the
     * connection string out of the Typesafe config file.
     */
    public static final String AZURE_EVENT_HUBS_CONNECTION_STRING_KEY = "connector-config.azure-event-hubs-connection-string";

    /**
     * The config value key for the Azure Event Hubs name.  This constant can be used to pull the event hub name out of
     * the Typesafe config file.
     */
    public static final String AZURE_EVENT_HUBS_NAME_KEY = "connector-config.azure-event-hubs-name";
}
