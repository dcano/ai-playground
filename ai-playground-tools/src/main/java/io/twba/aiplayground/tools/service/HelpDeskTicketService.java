package io.twba.aiplayground.tools.service;

import io.twba.aiplayground.TicketRequest;
import io.twba.aiplayground.tools.repository.HelpDeskTicket;
import io.twba.aiplayground.tools.repository.HelpDeskTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpDeskTicketService {

    private final HelpDeskTicketRepository helpDeskTicketRepository;

    public HelpDeskTicket createTicket(TicketRequest ticketInput, String userName) {

        HelpDeskTicket ticket = HelpDeskTicket.builder()
                .issue(ticketInput.issue())
                .userName(userName)
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
