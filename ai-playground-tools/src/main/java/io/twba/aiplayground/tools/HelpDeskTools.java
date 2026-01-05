package io.twba.aiplayground.tools;

import io.twba.aiplayground.TicketRequest;
import io.twba.aiplayground.tools.repository.HelpDeskTicket;
import io.twba.aiplayground.tools.service.HelpDeskTicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HelpDeskTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskTools.class);

    private final HelpDeskTicketService service;

    @Tool(name = "createTicket", description = "Create the Support ticket. This tool has nothing to do with GitHub, and is a custom ticketing management system."/*, returnDirect = true*/)
    String createTicket(@ToolParam(description = "Details to create a Support ticket") TicketRequest ticketRequest,
            ToolContext toolContext) {
        LOGGER.info("Creating support ticket for user: {} with details: {}", toolContext.getContext().get("username"), ticketRequest);
        String userName = (String) toolContext.getContext().get("username");
        HelpDeskTicket savedTicket = service.createTicket(ticketRequest, userName);
        LOGGER.info("Ticket created successfully with id: {}, Username: {}", savedTicket.getId(), savedTicket.getUserName());
        return "Ticket #" + savedTicket.getId() + " created successfully for user " + savedTicket.getUserName();
    }

    @Tool(name = "getTicketStatus", description = "Fetch the status of the tickets based on a given username. This tool has nothing to do with GitHub, and is a custom ticketing management system.")
    List<HelpDeskTicket> getTicketStatus(ToolContext toolContext) {
        LOGGER.info("Fetching support ticket for user: {}", toolContext.getContext().get("username"));
        String username = (String)toolContext.getContext().get("username");
        List<HelpDeskTicket> ticketsByUserName = service.getTicketsByUserName(username);
        LOGGER.info("Found {} tickets for user: {}",ticketsByUserName.size(), username);
        return ticketsByUserName;
    }

}
