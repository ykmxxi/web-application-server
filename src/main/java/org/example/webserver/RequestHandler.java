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
import java.util.Arrays;
import java.util.Map;

import org.example.db.DataBase;
import org.example.model.User;
import org.example.util.HttpRequestUtils;
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
            // 디버깅용 루프
//            while (!"".equals(line)) {
//                log.debug("header line : {}", line);
//                line = br.readLine(); // 끝에 line을 읽어 EOF 처리
//            }

            String path = HttpRequestUtils.getUrl(line);
            if (path.contains("/user/create")) {
                String[] tokens = path.split("\\?");
                log.debug("tokens = {}", Arrays.toString(tokens));

                User user = createUser(tokens[1]);
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
