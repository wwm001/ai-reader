package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class TextNormalizerTest {
    @Test
    public void fingerprint_isStableForWhitespaceVariants() {
        Assert.assertEquals(
                TextNormalizer.fingerprint("琉球  王国 の説明"),
                TextNormalizer.fingerprint(" 琉球\n王国\tの説明 "));
    }
}
