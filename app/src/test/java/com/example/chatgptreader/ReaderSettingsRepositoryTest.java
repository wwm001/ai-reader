package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class ReaderSettingsRepositoryTest {
    @Test
    public void clampSpeechRate_limitsAndRounds() {
        Assert.assertEquals(0.5f, ReaderSettingsRepository.clampSpeechRate(0.1f), 0.001f);
        Assert.assertEquals(1.0f, ReaderSettingsRepository.clampSpeechRate(1.04f), 0.001f);
        Assert.assertEquals(1.1f, ReaderSettingsRepository.clampSpeechRate(1.06f), 0.001f);
        Assert.assertEquals(2.0f, ReaderSettingsRepository.clampSpeechRate(3.0f), 0.001f);
    }
}
