package com.ibdiscord.view;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class TicketViewHandler {

    private final Map<Long, TicketView> views = new ConcurrentHashMap<>();

}
