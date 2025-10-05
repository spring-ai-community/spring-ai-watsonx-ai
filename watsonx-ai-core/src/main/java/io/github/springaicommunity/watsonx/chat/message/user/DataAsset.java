package io.github.springaicommunity.watsonx.chat.message.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data asset reference for an image, video or audio content. This is used primarily by {@link
 * TextChatUserImageUrlContent}, {@link TextChatUserVideoUrlContent} and {@link
 * TextChatUserAudioUrlContent}.
 */
public record DataAsset(@JsonProperty("id") String id) {}
