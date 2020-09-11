package ru.demetriuzz.token.client.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.demetriuzz.token.client.Token;
import ru.demetriuzz.token.client.TokenGenerator;

import java.io.File;
import java.util.Base64;

public class TokenGeneratorTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Тестовое хранилище созданно с помощью keytool
    // Алиас "T16", Пароль хранилища и ключа один "Test16"

    private static final String CURRENT_DIR = System.getProperty("user.dir", null);
    private static final String FS = File.separator; // платформозависимо!
    private static final String KEY_STORE_PATH
            = CURRENT_DIR + FS + "src" + FS + "test" + FS + "resources" + FS + "STORE.jks";
    private static final String KEY_ALIAS = "T16";
    private static final String KEY_PASSWORD = "Test16";

    @Test
    void genTest() throws Exception {
        logger.info("CURRENT_DIR=" + CURRENT_DIR);
        logger.info("KEY_STORE_PATH=" + KEY_STORE_PATH);
        Assertions.assertTrue(new File(KEY_STORE_PATH).canRead());

        TokenGenerator generator = new TokenGenerator();
        generator.setKeyStorePath(KEY_STORE_PATH);
        generator.setKeyAlias(KEY_ALIAS);
        generator.setKeyStorePassword(KEY_PASSWORD);
        generator.setKeyPassword(KEY_PASSWORD);

        Token token = generator.generateToken("c1", "p7", "m42", 100_000L);
        Assertions.assertNotNull(token);
        logger.info("Token created, length " + token.getToken().getBytes().length + " byte");

        String[] tokenB64 = token.getToken().split("\\.");
        Assertions.assertEquals(3, tokenB64.length);
        // header
        logger.info("Token, header: " + tokenB64[0]);
        String header = new String(Base64.getDecoder().decode(tokenB64[0]));
        logger.info("Token, header: " + header);
        JSONAssert.assertEquals("{\"typ\":\"JWT\"" +
                ",\"alg\":\"RS512\"}", header, JSONCompareMode.NON_EXTENSIBLE);
        // payload
        logger.info("Token, payload: " + tokenB64[1]);
        String payload = new String(Base64.getDecoder().decode(tokenB64[1]));
        logger.info("Token, payload: " + payload);
        JSONAssert.assertEquals(
                "{\"date\":100000" +
                        ",\"sha256\":\"966708b1e6ce080d4ec497e6128019e1ffa19239c21724e332ac5469680f5ceb\"" +
                        ",\"project\":\"p7\"" +
                        ",\"machine\":\"m42\"" +
                        ",\"login\":\"c1\"}", payload, JSONCompareMode.NON_EXTENSIBLE);
        // signature
        logger.info("Token, signature: " + tokenB64[2]);
        Assertions.assertEquals("m0qLJnZrhgHnHucB4OsSN4RhCOvxj0MSkPQH_rtTc6ZZNWbWrhQRbF-1Mf2sxe0w5Uj962TP" +
                "-VfSl8jOWOBuiuMjl2YEaROhY2zfUvnIofkLOaHKsugdb_NjvdiVW8Jc_mXdj0Q34g79x-vRGsiZRbXOFiVFSlQhswpNJxH0" +
                "90JH_lsPn2L2-fW50M6d-Dc4JTIV2j5Gq5E98dyTxWST_G_78xglryTnSKZwwshrWhSpfVnX6Aa_u6KLRCRomFYam_-MmBUo" +
                "6NOrKH2fWl5esWRjFYtCzD1GzgZievpJzBhYc8gA1SctIt_YUmSnQMO3LbcFdJpNWSd5ecPGjXRoog", tokenB64[2]);
    }

}