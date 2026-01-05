package io.twba.aiplayground.tools.helpdesk;

import io.twba.aiplayground.tools.helpdesk.repository.HelpDeskTicket;
import io.twba.aiplayground.tools.helpdesk.service.HelpDeskTicketService;
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

    @Tool(name = "createTicket", description = "Create the Support ticket"/*, returnDirect = true*/)
    String createTicket(@ToolParam(description = "Details to create a Support ticket") TicketRequest ticketRequest) {
        LOGGER.info("Creating support ticket for user: {} with details: {}", ticketRequest.username(), ticketRequest);

        HelpDeskTicket savedTicket = service.createTicket(ticketRequest);
        LOGGER.info("Ticket created successfully with id: {}, Username: {}", savedTicket.getId(), savedTicket.getUserName());
        return "Ticket #" + savedTicket.getId() + " created successfully for user " + savedTicket.getUserName();
    }

    @Tool(name = "getTicketStatus", description = "Fetch the list of tickets for the given username. The tickets can by in any status.")
    List<HelpDeskTicket> getTicketStatus(ToolContext toolContext) {
        LOGGER.info("Fetching support ticket for user: {}", toolContext.getContext().get("username"));
        String username = (String)toolContext.getContext().get("username");
        List<HelpDeskTicket> ticketsByUserName = service.getTicketsByUserName(username);
        LOGGER.info("Found {} tickets for user: {}",ticketsByUserName.size(), username);
        return ticketsByUserName;
    }

}
