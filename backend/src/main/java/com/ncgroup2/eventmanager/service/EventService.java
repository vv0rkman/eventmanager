package com.ncgroup2.eventmanager.service;

import com.ncgroup2.eventmanager.dto.EventCountdownDTO;
import com.ncgroup2.eventmanager.dto.EventDTO;
import com.ncgroup2.eventmanager.entity.Event;

import java.util.List;
import java.util.UUID;

public interface EventService {

    void createEvent(EventDTO eventDTO);

    List isCustomersExist(List<String> login);

    void createEventInvitations(List<String> login, UUID eventId);

    List<Event> getEventsByCustId(String custId);

    Event getEventById(String eventId);

    void deleteEventById(String eventId);

    List<Event> getAllPublicAndFriendsEvents(String customerId);

    boolean isParticipant(String customerId, String eventId);

    void removeParticipant(String customerId, String eventId);

    void addParticipant(String customerId, String eventId);

    List<EventCountdownDTO> getCountdownMessages();

    List<Event> getDraftsByCustId(String custId);

    List<Event> getNotesByCustId(String custId);

    List<Event> getInvitesByCustId(String custId);
}
