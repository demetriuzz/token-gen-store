package ru.demetriuzz.token.client.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.demetriuzz.token.client.TokenParameter;

public class TokenParameterTest {

    @Test
    public void parameterTest() {
        Assertions.assertEquals(8, TokenParameter.values().length);
        try {
            TokenParameter.valueOf("login");
            TokenParameter.valueOf("project");
            TokenParameter.valueOf("machine");
            TokenParameter.valueOf("date");
            TokenParameter.valueOf("sha256");
            TokenParameter.valueOf("token");
            TokenParameter.valueOf("expiration");
            TokenParameter.valueOf("consumer");
        } catch (Exception e) {
            Assertions.fail();
        }
    }

}