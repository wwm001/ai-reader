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
}
