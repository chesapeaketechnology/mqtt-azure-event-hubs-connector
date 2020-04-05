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

## Changelog

##### [0.1.0]() - TBD
 * Initial cut of this application