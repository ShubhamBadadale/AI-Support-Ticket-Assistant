package com.example.tickets;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class ApiSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TicketRepository tickets;
    @Autowired MessageRepository messages;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    User customer(String n) {
        User u = new User(); u.setUsername(n + System.nanoTime()); u.setPasswordHash(encoder.encode("pw")); u.setRole(Role.CUSTOMER);
        return users.save(u);
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotSeeOthersTicketAndCannotSeeInternalNotes() throws Exception {
        User c1 = customer("c1"); User c2 = customer("c2");
        Ticket t = new Ticket(); t.setTicketNumber("T-" + System.nanoTime()); t.setSubject("Subject for test case here");
        t.setDescription("A sufficiently long description for validation purposes 12345."); t.setCreatedBy(c1);
        tickets.save(t);
        // c2 tries to read c1's ticket
        mvc.perform(get("/api/tickets/" + t.getId()).with(SecurityMockMvcRequestPostProcessors.user(c2.getUsername()).roles("CUSTOMER")))
            .andExpect(status().isForbidden());
        // owner can read but response must not contain internalNotes/drafts
        var res = mvc.perform(get("/api/tickets/" + t.getId()).with(SecurityMockMvcRequestPostProcessors.user(c1.getUsername()).roles("CUSTOMER")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assertions.assertFalse(res.contains("internalNotes"));
        Assertions.assertFalse(res.contains("draftText"));
    }

    @Test
    void invalidStatusTransitionRejected() throws Exception {
        User agent = new User(); agent.setUsername("ag" + System.nanoTime()); agent.setPasswordHash(encoder.encode("pw")); agent.setRole(Role.AGENT);
        users.save(agent);
        User c = customer("cx");
        Ticket t = new Ticket(); t.setTicketNumber("T-" + System.nanoTime()); t.setSubject("Subject for transition test");
        t.setDescription("A sufficiently long description for validation purposes 12345."); t.setCreatedBy(c); t.setAssignee(agent);
        tickets.save(t);
        mvc.perform(post("/api/tickets/" + t.getId() + "/status").with(SecurityMockMvcRequestPostProcessors.user(agent.getUsername()).roles("AGENT"))
                .contentType("application/json").content("{\"status\":\"RESOLVED\"}"))
            .andExpect(status().isBadRequest());
    }
}
