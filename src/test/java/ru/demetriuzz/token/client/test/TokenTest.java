package ru.demetriuzz.token.client.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.demetriuzz.token.client.Token;

public class TokenTest {

    @Test
    void structure() {
        Token token = new Token("Tt1", 123_456L);
        Assertions.assertEquals("Tt1", token.getToken());
        Assertions.assertEquals(123_456L, token.getDate());
        Assertions.assertEquals(-1L, token.getExpiration());
        Assertions.assertEquals("Bearer Tt1", token.getAuthorizationHeader());

        token.setToken("Qq2");
        token.setExpiration(333_444L);
        Assertions.assertEquals("Qq2", token.getToken());
        Assertions.assertEquals(123_456L, token.getDate());
        Assertions.assertEquals(333_444L, token.getExpiration());
        Assertions.assertEquals("Bearer Qq2", token.getAuthorizationHeader());
    }

}