package ru.demetriuzz.token.client;

import java.util.HashMap;

/**
 * Хранилище JWT, с токеном для каждого пользователя.<br />
 * (Предполагается идентификатор пользователя как положительное число, от 0 и выше.<br />
 * В служебных целях, идентификатор может быть отрицательным числом.)
 *
 * @see TokenParameter
 * @since 0.0.1
 */
public class TokenStore {

    private static final HashMap<String, Token> store = new HashMap<>();
    private static final Object MONITOR = new Object();

    public static void setToken(String uidConsumer, Token token) {
        synchronized (MONITOR) {
            store.put(uidConsumer, token);
            MONITOR.notifyAll();
        }
    }

    public static Token getToken(String uidConsumer) {
        synchronized (MONITOR) {
            return store.get(uidConsumer);
        }
    }

    public static Token removeToken(String uidConsumer) {
        synchronized (MONITOR) {
            Token removed = store.remove(uidConsumer);
            MONITOR.notifyAll();
            return removed;
        }
    }

    /**
     * Размер хранилища
     */
    public static int size() {
        synchronized (MONITOR) {
            return store.size();
        }
    }

    /**
     * Очистка хранилища
     */
    public static void clear() {
        synchronized (MONITOR) {
            store.clear();
            MONITOR.notifyAll();
        }
    }

    /**
     * Действие токена завершилось?
     */
    public static boolean isInvalid(String uidConsumer) {
        synchronized (MONITOR) {
            return (store.get(uidConsumer) == null
                    || store.get(uidConsumer).getExpiration() <= System.currentTimeMillis());
        }
    }

    /**
     * Обновление данных токена<br />
     * Требования при обновлении:<br />
     * 1.<b>Наличие</b> полного ответа сервера<br />
     * 2.<b>Совпадение</b> идентификатора пользователя в параметре и в ответе сервера.
     *
     * @see TokenParameter#token
     * @see TokenParameter#expiration
     * @see TokenParameter#consumer
     */
    public static void updateToken(String uidConsumer,
                                   Token updated,
                                   HashMap<TokenParameter, String> response) {
        // нет ответа, нет обновления
        if (response == null || response.isEmpty()) return;

        String uidResponse = null;
        if (response.containsKey(TokenParameter.consumer)
                && response.get(TokenParameter.consumer) != null)
            uidResponse = response.get(TokenParameter.consumer);
        // необходимо наличие пользователя в ответе
        if (uidResponse == null) return;

        if (response.containsKey(TokenParameter.token)
                && response.get(TokenParameter.token) != null)
            updated.setToken(response.get(TokenParameter.token));

        if (response.containsKey(TokenParameter.expiration)
                && response.get(TokenParameter.expiration) != null)
            updated.setExpiration(Long.parseLong(response.get(TokenParameter.expiration)));

        synchronized (MONITOR) {
            // обновление при идентичности пользователя в ответе сервера
            if (uidConsumer.equalsIgnoreCase(uidResponse)) store.put(uidConsumer, updated);
            MONITOR.notifyAll();
        }
    }

}