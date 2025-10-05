package io.github.springaicommunity.watsonx.chat.util.audio;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Supported audio formats in watsonx.ai. */
public enum AudioFormat {
  @JsonProperty("mp3")
  MP3,

  @JsonProperty("wav")
  WAV
}
