package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class DiagnosticStoreTest {
    @Test
    public void candidateSnippet_isLimitedToEightyCharacters() {
        String snippet = DiagnosticStore.limitSnippet("1234567890".repeat(9));

        Assert.assertEquals(80, snippet.length());
    }
}
