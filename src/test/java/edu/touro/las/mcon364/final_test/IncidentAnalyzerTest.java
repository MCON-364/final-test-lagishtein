package edu.touro.las.mcon364.final_test;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IncidentAnalyzerTest {

    private static int idSeq = 1;

    private SupportTicket ticket(String category, Priority priority, boolean resolved, int minutesToResolve) {
        return new SupportTicket(idSeq++, category, priority, minutesToResolve, resolved);
    }

    // ── Constructor ────────────────────────────────────────────────────────────

    @Test
    void constructorRejectsNullList() {
       Exception ex = assertThrows(
                Exception.class,
                () -> new IncidentAnalyzer(null)
        );
    }

    @Test
    void constructorMakesDefensiveCopy() {
        List<SupportTicket> source = new ArrayList<>();
        source.add(ticket("network", Priority.HIGH, true, 10));

        IncidentAnalyzer analyzer = new IncidentAnalyzer(source);

        // Mutate source after construction – analyzer must be unaffected.
        source.add(ticket("db", Priority.HIGH, true, 99));

        assertEquals(1L, analyzer.getClosedCount(),
                "Analyzer should not reflect tickets added to source list after construction");
        assertEquals(10.0, analyzer.getAverageTimeToClose(), 0.000001,
                "Average must be computed only over the originally copied incidents");
    }

    // ── getClosedCount ─────────────────────────────────────────────────────────

    @Test
    void getClosedCountReturnsZeroWhenNoneResolved() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, false, 0),
                ticket("db", Priority.LOW, false, 0)
        ));
        assertEquals(0L, analyzer.getClosedCount());
    }

    @Test
    void getClosedCountCountsOnlyResolvedIncidents() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, true, 30),
                ticket("network", Priority.LOW, false, 0),
                ticket("db", Priority.MEDIUM, true, 45),
                ticket("ui", Priority.HIGH, false, 0)
        ));
        assertEquals(2L, analyzer.getClosedCount());
    }

    @Test
    void getClosedCountReturnsAllWhenAllResolved() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, true, 5),
                ticket("db", Priority.MEDIUM, true, 15)
        ));
        assertEquals(2L, analyzer.getClosedCount());
    }

    // ── getAverageTimeToClose ──────────────────────────────────────────────────

    @Test
    void getAverageTimeToCloseReturnsZeroWhenNoClosedIncidents() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, false, 15),
                ticket("db", Priority.MEDIUM, false, 50)
        ));
        assertEquals(0.0, analyzer.getAverageTimeToClose(), 0.000001);
    }

    @Test
    void getAverageTimeToCloseIgnoresOpenIncidents() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, true, 10),
                ticket("db", Priority.MEDIUM, true, 20),
                ticket("ui", Priority.LOW, false, 999)   // must NOT affect average
        ));
        assertEquals(15.0, analyzer.getAverageTimeToClose(), 0.000001);
    }

    @Test
    void getAverageTimeToCloseWithSingleClosedIncident() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, true, 42)
        ));
        assertEquals(42.0, analyzer.getAverageTimeToClose(), 0.000001);
    }

    @Test
    void getAverageTimeToCloseOnEmptyList() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of());
        assertEquals(0.0, analyzer.getAverageTimeToClose(), 0.000001);
    }

    // ── getCountByCategory ─────────────────────────────────────────────────────

    @Test
    void getCountByCategoryGroupsAndCountsCorrectly() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, true, 20),
                ticket("network", Priority.LOW, false, 0),
                ticket("db", Priority.MEDIUM, true, 40),
                ticket("network", Priority.MEDIUM, true, 35),
                ticket("ui", Priority.LOW, false, 0)
        ));

        Map<String, Long> counts = analyzer.getCountByCategory();

        assertEquals(3L, counts.get("network"));
        assertEquals(1L, counts.get("db"));
        assertEquals(1L, counts.get("ui"));
        assertEquals(3, counts.size());
    }

    @Test
    void getCountByCategoryCountsAllRegardlessOfResolvedStatus() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("ops", Priority.HIGH, true, 10),
                ticket("ops", Priority.LOW, false, 0)
        ));
        assertEquals(2L, analyzer.getCountByCategory().get("ops"));
    }

    @Test
    void getCountByCategoryReturnsUnmodifiableMap() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, true, 10)
        ));

        Map<String, Long> counts = analyzer.getCountByCategory();

        assertThrows(UnsupportedOperationException.class, () -> counts.put("x", 1L));
        assertThrows(UnsupportedOperationException.class, counts::clear);
    }

    @Test
    void getCountByCategoryReturnsEmptyMapForEmptyList() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of());
        assertTrue(analyzer.getCountByCategory().isEmpty());
    }

    // ── getCriticalOpenIncidents ───────────────────────────────────────────────

    @Test
    void getCriticalOpenIncidentsReturnsOnlyOpenHighPriority() {
        SupportTicket openHigh1   = ticket("network", Priority.HIGH,   false, 0);
        SupportTicket openHigh2   = ticket("db",      Priority.HIGH,   false, 0);
        SupportTicket closedHigh  = ticket("ui",      Priority.HIGH,   true,  12);
        SupportTicket openLow     = ticket("ops",     Priority.LOW,    false, 0);
        SupportTicket openMedium  = ticket("ops",     Priority.MEDIUM, false, 0);

        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                openHigh1, closedHigh, openLow, openMedium, openHigh2
        ));

        List<SupportTicket> result = analyzer.getCriticalOpenIncidents();

        assertEquals(2, result.size());
        assertTrue(result.contains(openHigh1));
        assertTrue(result.contains(openHigh2));
        assertFalse(result.contains(closedHigh), "Closed HIGH tickets must be excluded");
        assertFalse(result.contains(openLow),    "Open LOW tickets must be excluded");
        assertFalse(result.contains(openMedium), "Open MEDIUM tickets must be excluded");
    }

    @Test
    void getCriticalOpenIncidentsReturnsEmptyListWhenNoneMatch() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH,   true,  5),   // closed HIGH
                ticket("db",      Priority.MEDIUM, false, 0),   // open MEDIUM
                ticket("ui",      Priority.LOW,    false, 0)    // open LOW
        ));
        assertTrue(analyzer.getCriticalOpenIncidents().isEmpty());
    }

    @Test
    void getCriticalOpenIncidentsReturnsUnmodifiableList() {
        IncidentAnalyzer analyzer = new IncidentAnalyzer(List.of(
                ticket("network", Priority.HIGH, false, 0)
        ));

        List<SupportTicket> result = analyzer.getCriticalOpenIncidents();

        assertThrows(UnsupportedOperationException.class,
                () -> result.add(ticket("db", Priority.HIGH, false, 0)));
        assertThrows(UnsupportedOperationException.class, result::clear);
    }
}

