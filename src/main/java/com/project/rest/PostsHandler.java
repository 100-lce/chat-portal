package com.project.rest;

import com.project.utils.Config;
import com.project.dao.PostsDAO;
import com.project.models.Post;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.project.server.ApiServer.*;

public class PostsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        System.out.println("Hanlduje request");
        try {
            if (method.equalsIgnoreCase("GET")) {
                handleGetRequest(exchange);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePostRequest(exchange);
            }else if (method.equalsIgnoreCase("PUT")) {
                handlePutRequest(exchange);
            } else if(method.equalsIgnoreCase("DELETE")) {
                handleDeleteRequest(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (IOException | SQLException | InterruptedException e) {
            System.out.println("Error przy handlingu requesta");
            System.out.println(e.getMessage());
            exchange.sendResponseHeaders(404, -1);
        } catch (Exception e) {
            System.out.println("Niestandardowy błąd");
            System.out.println(e.getMessage());
            e.printStackTrace();
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handlePutRequest(HttpExchange exchange) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String postId = path.substring(path.lastIndexOf("/") + 1);

        if (postId.isEmpty()) {
            throw new IllegalArgumentException("Brak id w URL!");
        }

        InputStreamReader streamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        String requestBody = bufferedReader.lines().collect(Collectors.joining("\n"));

        Post newPost = gson.fromJson(requestBody, Post.class);

        PostsDAO dao = new PostsDAO(Config.getDbUrl(), Config.getDbUsername(), Config.getDbPassword());
        dao.connect();

        boolean isUpdated = dao.updatePost(newPost);

        if (isUpdated) {
            sendResponse(exchange, 200, "Post updated successfully");
        } else {
            sendResponse(exchange, 500, "Failed to update post");
        }
    }

    private void handleDeleteRequest(HttpExchange exchange) throws SQLException, IOException {
        Map<String, String> queryParams = getQueryParams(exchange.getRequestURI().getQuery());
        String postId = queryParams.get("postId");
        if (postId == null) {
            throw new IllegalArgumentException("Brak argumentu w zapytaniu!");
        }
        PostsDAO dao = new PostsDAO(Config.getDbUrl(), Config.getDbUsername(), Config.getDbPassword());
        dao.connect();
        boolean isDeleted = dao.deletePostWithId(Integer.parseInt(postId));

        if (!isDeleted) {
            throw new SQLException("Nie udało się usunąć");
        }
        sendResponse(exchange, 200, "Post Deleted Successfully");
    }

    private void handlePostRequest(HttpExchange exchange) throws SQLException, IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String requestBody = bufferedReader.lines().collect(Collectors.joining("\n"));

        Post newPost = gson.fromJson(requestBody, Post.class);
        PostsDAO dao = new PostsDAO(Config.getDbUrl(), Config.getDbUsername(), Config.getDbPassword());
        dao.connect();

        boolean isAdded = dao.addPost(newPost);

        if (isAdded) {
            sendResponse(exchange, 201, "Post added successfully");
        } else {
            sendResponse(exchange, 500, "Failed to add post");
        }

    }

    private void handleGetRequest(HttpExchange exchange) throws IOException, SQLException, InterruptedException {
        Map<String, String> queryParams = getQueryParams(exchange.getRequestURI().getQuery());
        String excludeId = queryParams.get("excludeId");
        if (excludeId != null) {
            getPostsExcludingId(exchange, Integer.parseInt(excludeId));
        } else {
            getAllPosts(exchange);
        }
    }

    private void getPostsExcludingId(HttpExchange exchange, int excludeId) throws SQLException, IOException, InterruptedException {
        PostsDAO dao = new PostsDAO(Config.getDbUrl(), Config.getDbUsername(), Config.getDbPassword());
        dao.connect();

        String responseContent = dao.getAllPostsExcludingId(excludeId);
        System.out.println("resp con:" + responseContent);
        sendResponse(exchange, 200, responseContent);
        System.out.println("z bazy: " + responseContent);
        dao.close();
    }

    private Map<String, String> getQueryParams(String query) {
        if (query == null) {
            return Map.of();
        }

        return Stream.of(query.split("&"))
                .map(p -> p.split("="))
                .collect(Collectors.toMap(pp -> pp[0], pp -> pp.length > 1 ? pp[1] : ""));
    }

    private void getAllPosts(HttpExchange exchange) throws SQLException, IOException {
        PostsDAO dao = new PostsDAO(Config.getDbUrl(), Config.getDbUsername(), Config.getDbPassword());
        dao.connect();
        String responseContent = dao.getAllPosts();

        sendResponse(exchange, 200, responseContent);
        System.out.println("z bazy: " + responseContent);
        dao.close();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseContent) throws IOException {
        exchange.sendResponseHeaders(statusCode, responseContent.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseContent.getBytes());
        os.close();
    }
}

