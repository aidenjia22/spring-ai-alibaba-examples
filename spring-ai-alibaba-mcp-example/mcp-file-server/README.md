# Spring AI Alibaba MCP File Server

This module provides a simple file reading service through the Model Context Protocol (MCP).

## Features

- Read local files using the MCP protocol
- Simple and easy to use

## Usage

1. Build the project:
   ```
   mvn clean install
   ```

2. Run the server:
   ```
   java -jar target/mcp-file-server.jar
   ```

3. The server will expose a file reading tool that can be used by MCP clients.

## Tool

The server provides a `read_file` tool that accepts a `filePath` parameter and returns the content of the file.