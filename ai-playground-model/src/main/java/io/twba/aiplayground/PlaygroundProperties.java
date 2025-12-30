package io.twba.aiplayground;

import lombok.Data;

@Data
public class PlaygroundProperties {

    private boolean hasMemory;
    private String tavilyApiKey;
    private int tavilyResultLimit = 5;

}
