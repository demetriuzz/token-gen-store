package ru.demetriuzz.token.client;

/**
 * Объект <b>JWT</b><br />
 * Данные для заголовка "Authorization"
 */
public class Token {

    // данные для заголовка "Authorization"
    private final String type = "Bearer";
    private String token;
    // дата и время генерации токена
    private final long date;
    // дата и время до которого действует токен
    private long expiration = -1L;

    public Token(String token, long date) {
        this.token = token;
        this.date = date;
    }

    /**
     * Тип токена
     */
    public String getType() {
        return type;
    }

    /**
     * Обновление токена
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Значение токена
     */
    public String getToken() {
        return token;
    }

    /**
     * Дата и время генерации токена
     */
    public long getDate() {
        return date;
    }

    /**
     * Значение для заголовка Authorization
     */
    public String getAuthorizationHeader() {
        return type + " " + token;
    }

    //

    /**
     * Установка даты и времени до которого действует токен (полученное от сервера)
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    /**
     * Дата и время до которого действует токен
     */
    public long getExpiration() {
        return expiration;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type='" + type + '\'' +
                ", token='" + token + '\'' +
                ", date=" + date +
                ", expiration=" + expiration +
                '}';
    }

}