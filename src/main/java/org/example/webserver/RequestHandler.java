package org.example.webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.example.db.DataBase;
import org.example.model.User;
import org.example.util.HttpRequestUtils;
import org.example.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(final Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // 요구사항 1. index.html 응답
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)); // UTF-8 처리
            String line = br.readLine();
            if (line == null) { // 마지막 줄 EOF를 null로 읽어옴
                return;
            }

            // 요구사항 3. POST 방식으로 회원가입하기
            String path = HttpRequestUtils.getUrl(line);
            Map<String, String> headers = new HashMap<>();
            while (!line.isEmpty()) {
                log.debug("header : {}", line);
                line = br.readLine();

                String[] headerTokens = line.split(": ");
                if (headerTokens.length == 2) {
                    headers.put(headerTokens[0], headerTokens[1]); // content-length 구해서 넣기
                }
            }

            if (path.startsWith("/user/create")) {
                log.debug("path = {}", path);

                String body = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                User user = createUser(body);
                log.debug("user = {}", user);

                path = "/index.html";
            }

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private User createUser(final String queryString) {
        Map<String, String> params = HttpRequestUtils.parseQueryString(queryString);
        log.debug("requests = {}", params);

        User user = new User(
                params.get("userId"),
                params.get("password"),
                params.get("name"),
                params.get("email")
        );
        DataBase.addUser(user);
        return user;
    }

    private void response200Header(final DataOutputStream dos, final int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(final DataOutputStream dos, final byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
