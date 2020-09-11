package ru.demetriuzz.token.client.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.demetriuzz.token.client.Token;
import ru.demetriuzz.token.client.TokenParameter;
import ru.demetriuzz.token.client.TokenStore;

import java.util.HashMap;
import java.util.Random;

public class TokenStoreTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String PREFIX = "TKN-";
    private static final long START = 100_000L;
    private static final long NEXT_EXP = 3_600_000L;
    private static final int MIN_SLEEP = 500;
    private static final int MAX_SLEEP = 1_000;

    private long randomSleep() {
        // минимум MIN_SLEEP, максимум MAX_SLEEP
        int next = new Random().nextInt(MAX_SLEEP - MIN_SLEEP + 1) + MIN_SLEEP;
        return (next > MIN_SLEEP & next < MAX_SLEEP) ? next : MIN_SLEEP;
    }

    private class SomeSetThread extends Thread {

        private final String[] consumer;
        private final long sleep;

        public SomeSetThread(String name, long sleep, String... id) {
            setName(name);
            consumer = id;
            this.sleep = sleep;
        }

        @Override
        public void run() {
            try {
                sleep(sleep);
            } catch (Exception ignore) {}

            for (String c : consumer) {
                TokenStore.setToken(c, new Token(PREFIX + c, System.currentTimeMillis()));
                logger.info(Thread.currentThread().getName() + ", SET token for consumer " + c);
            }
        }

    }

    private class SomeUpdateThread extends Thread {

        private final String[] consumer;
        private final long sleep;

        public SomeUpdateThread(String name, long sleep, String... id) {
            setName(name);
            consumer = id;
            this.sleep = sleep;
        }

        @Override
        public void run() {
            String thName = Thread.currentThread().getName();
            try {
                sleep(sleep);
            } catch (Exception ignore) {}

            for (String c : consumer) {
                try {
                    TokenStore.updateToken(c,
                            new Token(PREFIX + c, System.currentTimeMillis()),
                            new HashMap<>() {{
                                put(TokenParameter.token, PREFIX + c + "_by_" + thName);
                                put(TokenParameter.expiration, Long.toString(System.currentTimeMillis() + NEXT_EXP));
                                put(TokenParameter.consumer, c);
                            }});

                    logger.info(thName + ", UPDATE token for consumer " + c);
                } catch (Exception e) {
                    Assertions.fail();
                }
            }
        }

    }

    @Test
    void controlStoreInOneThread() {
        Thread.currentThread().setName("TEST");
        TokenStore.clear();
        // в главном потоке пусто
        Assertions.assertEquals(0, TokenStore.size());
        // нет токена, тоже что "просрочен"
        Assertions.assertTrue(TokenStore.isInvalid("c2"));

        String[] consumer = new String[] {"c42", "c0", "c1", "c2"};
        for (int i = 0; i < consumer.length; i++) {
            String c = consumer[i];
            TokenStore.setToken(c, new Token(PREFIX + c, START + (i * 10)));
        }
        Assertions.assertEquals(4, TokenStore.size());
        // проверка
        for (int i = 0; i < consumer.length; i++) {
            String c = consumer[i];
            Assertions.assertEquals(PREFIX + c, TokenStore.getToken(c).getToken());
            Assertions.assertEquals(START + (i * 10), TokenStore.getToken(c).getDate());
            Assertions.assertEquals(-1L, TokenStore.getToken(c).getExpiration());
            // все токены просрочены
            Assertions.assertTrue(TokenStore.isInvalid(c));
        }

        // нет такого пользователя
        TokenStore.removeToken("c-1");
        Assertions.assertEquals(4, TokenStore.size());

        // удаление токена пользователя
        Token t1 = TokenStore.removeToken("c1");
        Assertions.assertEquals(PREFIX + "c1", t1.getToken());
        Assertions.assertEquals(3, TokenStore.size());
        Assertions.assertNull(TokenStore.getToken("c1"));

        // что осталось
        for (int i = 0; i < consumer.length; i++) {
            String c = consumer[i];
            if (c.equalsIgnoreCase("c1")) continue;

            Assertions.assertEquals(PREFIX + c, TokenStore.getToken(c).getToken());
            Assertions.assertEquals(START + (i * 10), TokenStore.getToken(c).getDate());
            Assertions.assertEquals(-1L, TokenStore.getToken(c).getExpiration());
            // все токены просрочены
            Assertions.assertTrue(TokenStore.isInvalid(c));
        }

        // обновление токена для другого пользователя
        try {
            TokenStore.updateToken("c0",
                    new Token("", 100L),
                    new HashMap<>() {{
                        put(TokenParameter.token, "XYZ");
                        put(TokenParameter.expiration, Long.toString(1_000L));
                        // на входе c0, в ответе сервера c2
                        put(TokenParameter.consumer, "c2");
                    }});
        } catch (Exception e) {
            Assertions.fail();
        }
        // ничего не изменилось
        for (String c : consumer) {
            if (c.equalsIgnoreCase("c1")) continue;

            Assertions.assertEquals(PREFIX + c, TokenStore.getToken(c).getToken());
        }

        // обновление токена
        try {
            TokenStore.updateToken("c2",
                    new Token("", 100L),
                    new HashMap<>() {{
                        put(TokenParameter.token, "NEW-TOKEN");
                        put(TokenParameter.expiration, Long.toString(System.currentTimeMillis() + NEXT_EXP));
                        // ответы по пользователю совпадают
                        put(TokenParameter.consumer, "c2");
                    }});
        } catch (Exception e) {
            Assertions.fail();
        }
        // валидность обновленного токена
        Assertions.assertFalse(TokenStore.isInvalid("c2"));
        Assertions.assertEquals("NEW-TOKEN", TokenStore.getToken("c2").getToken());
        Assertions.assertTrue(TokenStore.getToken("c2").getExpiration() > System.currentTimeMillis());

        // другие - без изменений
        for (String c : consumer) {
            if (c.equalsIgnoreCase("c1") || c.equalsIgnoreCase("c2")) continue;

            Assertions.assertEquals(PREFIX + c, TokenStore.getToken(c).getToken());
        }
    }

    @Test
    void controlStoreInManyThread() throws Exception {
        Thread.currentThread().setName("TEST");
        TokenStore.clear();
        // пусто
        Assertions.assertEquals(0, TokenStore.size());

        // массив пользователей
        String[] cn = new String[] {"c0", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9"};
        // многопоточность
        // условный список пользователей, в том числе пропускаемых.
        SomeSetThread set0 = new SomeSetThread("SET-0", randomSleep(), cn[0], cn[1], cn[2]);
        SomeSetThread set1 = new SomeSetThread("SET-1", randomSleep(), cn[3], cn[4], cn[5]);
        SomeSetThread set2 = new SomeSetThread("SET-2", randomSleep(), cn[5], cn[6]);
        SomeSetThread set3 = new SomeSetThread("SET-3", randomSleep(), cn[6], cn[7], cn[8], cn[9]);
        // запуск потока по обновлению всех пользователей
        SomeUpdateThread update = new SomeUpdateThread("UP", randomSleep(), cn);
        set0.start();
        set1.start();
        set2.start();
        set3.start();
        update.start();

        // что-то делаем из главного потока
        for (String c : cn) TokenStore.isInvalid(c);
        for (String c : cn) TokenStore.getToken(c);

        // ждем дочерние потоки
        set0.join();
        set1.join();
        set2.join();
        set3.join();
        update.join();

        // хранилище общее для всех потоков
        Assertions.assertEquals(cn.length, TokenStore.size());
        for (String c : cn) {
            Assertions.assertNotNull(TokenStore.getToken(c));
            Assertions.assertTrue(TokenStore.getToken(c).getDate() > START);
        }
    }

}