package fr.firstsky.firstparkour.model;

import java.util.UUID;

public record DailyEntry(UUID uuid, String name, int score) {}
