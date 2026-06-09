package edu.touro.las.mcon364.final_test;

import edu.touro.las.mcon364.final_test.Priority;
import edu.touro.las.mcon364.final_test.SupportTicket;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Analyzing incidents from an operational incident tracking system.
 *
 * An incident management system tracks operational issues after they are reported
 * and worked on. Each incident has information such as its category, priority,
 * whether it has been closed, and how many minutes it took to close.
 * The goal of this class is to turn a list of individual incidents into
 * one aggregated summary.
 *
 * Requirements:
 * - The constructor receives the incidents to analyze.
 * - The original list must not be modified by this class.
 * - getClosedCount() returns how many incidents have been closed.
 * - getAverageTimeToClose() returns the average closing time for closed incidents only.
 * - getCountByCategory() returns how many incidents belong to each category.
 * - getCriticalOpenIncidents() returns open incidents with high priority.
 * - Use streams to express the data processing logic.
 * - Do not use loops.
 */
public class IncidentAnalyzer {
    //TODO - uncomment this field and initialize it in the constructor to store the incidents passed in.
    private final List<SupportTicket> incidents;

    /**
     * Store the incidents that this analyzer will examine.
     * The constructor should make a defensive copy of the list to prevent
     * external modification of the internal state of this class. If the input list is null, throw a NullPointerException.
     */
    public IncidentAnalyzer(List<SupportTicket> incidents) {
        this.incidents = List.copyOf(Objects.requireNonNull(incidents));
    }

    /**
     * Return how many incidents in this data set were closed.
     */
    public long getClosedCount() {
        return incidents.stream().filter(SupportTicket::resolved).count();
    }

    /**
     * Return the average closing time for closed incidents only.
     *
     * Incidents that are still open should not affect this average.
     */
    public double getAverageTimeToClose() {
        return incidents.stream().filter(SupportTicket::resolved).mapToInt(SupportTicket::minutesToResolve).average().orElse(0.0);
    }

    /**
     * Return how many incidents belong to each category.
     */
    public Map<String, Long> getCountByCategory() {
        var map = incidents.stream().collect(Collectors.groupingBy(SupportTicket::category, Collectors.counting()));
        return Map.copyOf(map);
    }

    /**
     * Return open incidents that require immediate attention.
     */
    public List<SupportTicket> getCriticalOpenIncidents() {
        return incidents.stream().filter(Predicate.not(SupportTicket::resolved)).filter(ticket -> ticket.priority() == Priority.HIGH).toList();
    }
}
