package io.twba.aiplayground.tools.helpdesk.service;

import io.twba.aiplayground.tools.helpdesk.TicketRequest;
import io.twba.aiplayground.tools.helpdesk.repository.HelpDeskTicket;
import io.twba.aiplayground.tools.helpdesk.repository.HelpDeskTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpDeskTicketService {

    private final HelpDeskTicketRepository helpDeskTicketRepository;

    public HelpDeskTicket createTicket(TicketRequest ticketInput) {

        HelpDeskTicket ticket = HelpDeskTicket.builder()
                .issue(ticketInput.issue())
                .userName(ticketInput.username())
                .status("OPEN")
                .createdAt(java.time.LocalDateTime.now())
                .eta(java.time.LocalDateTime.now().plusDays(7))
                .build();

        return helpDeskTicketRepository.save(ticket);
    }

    public List<HelpDeskTicket> getTicketsByUserName(String userName) {
        return helpDeskTicketRepository.findByUserName(userName);
    }

}
