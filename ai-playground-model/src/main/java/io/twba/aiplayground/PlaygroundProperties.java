package io.twba.aiplayground;

import lombok.Data;

@Data
public class PlaygroundProperties {

    private boolean hasMemory;
    private String tavilyApiKey;
    private int tavilyResultLimit = 5;
    private Model model;
    private ErrorHandling errorHandling;

    @Data
    public static class Model {
        private String type;
    }

    @Data
    public static class ErrorHandling {
        private String type = "assisted";
    }

}
