package com.ncgroup2.eventmanager.dao;

import com.ncgroup2.eventmanager.entity.Event;

public interface EventDao {

    void createEvent(String creatorId, Event event);

    void deleteEvent(Event event);
}
