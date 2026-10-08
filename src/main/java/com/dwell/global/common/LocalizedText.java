package com.dwell.global.common;

import com.fasterxml.jackson.annotation.JsonProperty;

// JSONB {en, native}: English original + the user's native-language mirror
public record LocalizedText(
        String en,
        @JsonProperty("native") String nativeText
) {
}
