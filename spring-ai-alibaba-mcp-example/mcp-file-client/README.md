# Spring AI Alibaba MCP File Client

This module demonstrates how to use the Model Context Protocol (MCP) to read local files.

## Features

- Connects to an MCP server through stdio transport
- Requests file reading operations from the server

## Usage

1. Build the server module first:
   ```
   cd ../mcp-file-server
   mvn clean install
   ```

2. Build the client module:
   ```
   cd ../mcp-file-client
   mvn clean install
   ```

3. Set your DashScope API key:
   ```
   export DASHSCOPE_API_KEY=your_api_key_here
   ```

4. Run the client:
   ```
   java -jar target/mcp-file-client.jar
   ```

5. The client will send a request to read a local file (README.md by default) to the server and display the result.

## Configuration

The client is configured to connect to the server through stdio transport. The server command is specified in the `application.properties` file.