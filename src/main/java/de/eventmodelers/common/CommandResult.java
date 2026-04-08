package de.eventmodelers.common;

import java.util.UUID;

public record CommandResult(UUID identifier, long aggregateSequence) {}
