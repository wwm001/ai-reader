package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class ChatGptUiFilterTest {
    @Test
    public void knownUiLabelsAreExcluded() {
        Assert.assertTrue(ChatGptUiFilter.isKnownUiLabel("codex に質問"));
        Assert.assertTrue(ChatGptUiFilter.isKnownUiLabel("共有"));
        Assert.assertFalse(ChatGptUiFilter.isKnownUiLabel("琉球王国の歴史についての本文です"));
    }
}
