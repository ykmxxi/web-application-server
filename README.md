# web-application-server

## 📌 요구사항
### 1. index.html 응답하기
- http://localhost:8080/index.html 접속했을 때 webapp 디렉토리의 index.html 파일을 읽어 클라이언트에 응답
- `BufferedReader`, `Files.readAllBytes()`
- 별도의 Util로 분리해 재사용

### 2. GET 방식으로 회원가입하기
- http://localhost:8080/user/form.html 이동하면서 회원가입
- 회원가입을 하면 다음과 같은 형태로 사용자가 입력한 값이 서버에 전달
  - `GET /user/create?userId=javajigi&password=password&name=JaeSung HTTP/1.1`    
- key=value 형태의 값을 User에 담는다
  - key=value 파싱은 `HttpRequestUtils` 클래스의 `parseQueryString()` 활용
- 요청 URL과 key=value 값을 분리해야 한다
  - `?` 기준 split
  - url: `/user/create`
  - query: `userId=javajigi&password=password&...`

### 3. POST 방식으로 회원가입하기
### 4. 302 status code 적용
### 5. 로그인하기
### 6. 사용자 목록 출력
### 7. CSS 지원
