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

    @Test
    public void longBodyTextContainingUiWordsIsNotExcludedByLabelOnly() {
        Assert.assertFalse(ChatGptUiFilter.isKnownUiLabel("chatgptの設定方法を共有します"));
        Assert.assertFalse(ChatGptUiFilter.isKnownUiLabel("モデルへのアクセス権限を確認します"));
        Assert.assertFalse(ChatGptUiFilter.isKnownUiLabel("文章をコピーして前の画面へ戻る方法です"));
    }
}
