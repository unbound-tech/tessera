package com.github.tessera.data.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BdbDumpFileTest {

    public BdbDumpFileTest() {
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws IOException {

    }

    @Test
    public void execute() throws Exception {

        Path inputFile = Paths.get(getClass().getResource("/bdb-sample.txt").toURI());
        BdbDumpFile instance = new BdbDumpFile(inputFile);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            instance.execute(outputStream);

            List<String> results = Arrays.asList(outputStream.toString().split(System.lineSeparator()));

            assertThat(results).hasSize(12);

        }

    }

    @Test
    public void executeSingleEntry() throws Exception {

        Path inputFile = Paths.get(getClass().getResource("/single-entry.txt").toURI());
        BdbDumpFile instance = new BdbDumpFile(inputFile);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            instance.execute(outputStream);

            List<String> results = Arrays.asList(outputStream.toString().split(System.lineSeparator()));

            assertThat(results).hasSize(1);

            assertThat(results).containsExactly("INSERT INTO ENCRYPTED_TRANSACTION "
                    + "(ENC_TX_SEQ,HASH,ENCODED_PAYLOAD) VALUES "
                    + "(ENC_TX_SEQ.NEXTVAL,"
                    + "'5530576f2f39665838594a724d38617a4d30302b304358366155416c7838663848675463636e6d6e3167726d6431494b306667564341705a6539644d632f674f7531662b68505a54714d467a666743325267484e55773d3d','00000000000000200542de47c272516862bae08c53f1cb034439a739184fe707208dd92817b2dc1a0000000000000179d2e6ee7f25feacc8b91a0366c326ff2569020a56067545495b51446a174a0c68c13f895ff0aede655926ed0817ba5a05f9f117f8a82f486999de0a6dd07281da290c034871c8a6ba7ce77f3c645f7f1fb89b1af4f76c36027c1637097b36f0331ce79a9ce959f156169cc192fee0ff0c8c66d55c0269b2b76f85c58ae02fc12948b823bc2d4d6ee88f96e1d60d85362d53dac7746bac16e2cf542711ecb586fa49c346cbbfea0d172b9b17101fffedf8a289e4819b2b1fe410b2aa2f2a15737faf2cdff4b6b36f00794643514a5a74f2b5529289e9544a3de1beb9963c7f8fe649ce90d35225bccf28b7cb55b952207519aff3e2d08aae7dc101d28d982002ff84a8ecb36c7b294e6ca8415442d84f8a3f93abcc089fcf57e5c14bd3330774bc1059350e873526f07ad192ed4866af0d0de49927e624f1c3a5c09d76ded38921395c775fef13322e895885cfbc974af1664aed1d4b8edecafa6f7a0237633ae17b32ac80474f13d85c074be18fc4f879695b81456acff3a5de00000000000000188e802f3106b991b49cf07182036b37012bfad5988083db1f0000000000000001000000000000003091d7e03ba7bbcde5404aa7c19f360cf6986f9c9e04224349c7d20f64ebd6f2d5484081d471f65269af7a3dce1c6cc8a40000000000000018922d2cb41117b400b57046616cbab42064d2bd6ba76240ab0000000000000001000000000000002044e019056b5269cc5742b39edc5180a890f226315e3d1e5c7b84d2233989d017');");

        }

    }
}
