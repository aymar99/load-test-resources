package com.aymar;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.sun.net.httpserver.*;

import static java.util.stream.Collectors.*;


public class MainApp {

    public static final Map<Integer, String> IN_MEMORY_DB = new HashMap<>();
    public static AtomicInteger ID_COUNTER = new AtomicInteger(0);

    public static Map<String, List<String>> splitQuery(String query) {
        if (query == null || "".equals(query)) {
            return Collections.emptyMap();
        }

        return Pattern.compile("&").splitAsStream(query)
                .map(s -> Arrays.copyOf(s.split("="), 2))
                .collect(groupingBy(s -> decode(s[0]), mapping(s -> decode(s[1]), toList())));

    }

    private static String decode(final String encoded) {
        try {
            return encoded == null ? null : URLDecoder.decode(encoded, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 is a required encoding", e);
        }
    }


    public static void main(String[] args) throws IOException {
        System.out.println("Started");
        int serverPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

        server.createContext("/customer", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, List<String>> params = splitQuery(exchange.getRequestURI().getRawQuery());
                String noNameText = "Anonymous";
                String id = params.getOrDefault("id", List.of(noNameText)).stream().findFirst().orElse(noNameText);
                String respText = IN_MEMORY_DB.get(Integer.valueOf(id));
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else if ("POST".equals(exchange.getRequestMethod())) {
                int id = ID_COUNTER.incrementAndGet();
                IN_MEMORY_DB.put(id, getRequestBody(exchange));
                String respText = "{\n" +
                        "    \"id\":\"" + id + "\"\n" +
                        "}";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.setExecutor(null);
        server.start();
    }

    private static String getRequestBody(HttpExchange exchange) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            requestBody.append(line);
        }
        bufferedReader.close();
        return requestBody.toString();
    }
}


