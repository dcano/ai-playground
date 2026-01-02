package io.twba.aiplayground.tools;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class TimeTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeTools.class);

    @Tool(name="getCurrentLocalTime", description="Get the current time in the user's timezone")
    public String getCurrentLocalTime() {
        LOGGER.info("Return the current time in the server's timezone");
        return LocalTime.now().toString();
    }

    @Tool(name = "getCurrentTimeByZone", description = "Get the current time in the specified time zone.")
    public String getCurrentTimeByZone(@ToolParam(description = "Value representing the time zone") String timeZone) {
        LOGGER.info("Return the current time in the specified time zone {}", timeZone);
        return LocalTime.now(ZoneId.of(timeZone)).toString();
    }

}
