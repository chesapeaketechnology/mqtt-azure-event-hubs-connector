# MQTT to Azure Event Hubs Connector

This Java application pulls messages from an MQTT Broker and streams the messages into Azure Event hubs.  This java
app is placed in a Docker container to make it easy to run in the cloud.

## Getting Started

### Prerequisites

[Install Docker](https://docs.docker.com/install/)

### Build Steps

To build and install the project follow the steps below:

1. Clone this repo
1. Run the following command to build the Java Application and Docker Container
    1. `./gradlew clean build dockerBuildImage`

### Run the Docker Container 

Execute the following command to run the docker container.
1. `docker run chesapeaketechnology/mqtt-azure-event-hub-connector:latest`

While the above command will run the docker container, it uses the default values and assumes a proper `mqttconnector.conf`
is included in the [src/main/resources](src/main/resources) directory.  A typical deployment of this container will
need to specify the location of the `mqttconnector.conf` file, and also define a location for the log files.  Follow the
process below for specifying the application.conf and logging directory locations.
 
1. Create a directory called `mqtt-docker-connector`, and create a `config` and `log` directory in it
    1. `mkdir mqtt-docker-connector`
    1. `cd mqtt-docker-connector`
    1. `mkdir config log`
1. Create an `mqttconnector.conf` file in the config directory
    1. Set all the values in the mqttconnector.conf file (see [src/main/resources/reference.conf](src/main/resources/reference.conf)) for an example
1. Change directory into the `mqtt-docker-connector` directory
1. Use the following command to start the docker container
    1. `docker run --name MqttAzureConnector -v $(pwd)/config:/mqtt-azure-connector/config -v $(pwd)/log:/mqtt-azure-connector/log chesapeaketechnology/mqtt-azure-event-hub-connector:latest`

## Changelog

##### [0.1.1](https://github.com/chesapeaketechnology/mqtt-azure-event-hubs-connector/releases/tag/v0.1.1) - 2020-06-11
 * Fixed a bug where messages were not being added back to the queue when an error occurred while sending the message to Azure.

##### [0.1.0](https://github.com/chesapeaketechnology/mqtt-azure-event-hubs-connector/releases/tag/v0.1.0) - 2020-04-21
 * Initial cut of this application.