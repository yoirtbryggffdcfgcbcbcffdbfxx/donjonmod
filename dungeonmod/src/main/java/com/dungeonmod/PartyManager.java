package com.dungeonmod;

public class PartyManager {
    private static String partyId;
    private static String partyCode;
    private static String runId;

    public static boolean isInParty() {
        return partyId != null && !partyId.isEmpty();
    }

    public static String getPartyId() {
        return partyId;
    }

    public static String getPartyCode() {
        return partyCode;
    }

    public static void setParty(String id, String code) {
        partyId = id;
        partyCode = code;
    }

    public static void leaveParty() {
        partyId = null;
        partyCode = null;
        runId = null;
    }

    public static String getRunId() {
        return runId;
    }

    public static void setRunId(String id) {
        runId = id;
    }

    public static void clearRunId() {
        runId = null;
    }

    public static boolean hasActiveRun() {
        return runId != null && !runId.isEmpty();
    }
}
