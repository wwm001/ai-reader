package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class NaturalTextChunkerTest {
    @Test
    public void chunksLongJapaneseTextAtNaturalBoundaries() {
        String text = "これは最初の長い文章です。これは二番目の長い文章で、読み上げ単位として分割されるべきです。";

        Assert.assertTrue(NaturalTextChunker.chunk(text).size() >= 1);
        Assert.assertTrue(NaturalTextChunker.chunk("短い").isEmpty());
    }

    @Test
    public void chunksNewlinesAndBullets() {
        String text = "これは見出しです\n・最初の項目です\n・二番目の項目です";

        Assert.assertEquals(3, NaturalTextChunker.chunk(text).size());
    }
}
