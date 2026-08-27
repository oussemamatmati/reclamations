package com.bank.complaints.dto;

import java.util.List;
import java.util.Map;

public class DashboardStats {
    private long totalReclamations;
    private Map<String, Long> statutCounts;
    private Map<String, Long> prioriteCounts;
    private Map<String, Long> categorieCounts;
    private Map<String, Long> agentWorkload;
    private List<ReclamationDTO> recentReclamations;
    private long totalUsers;
    private long totalComments;
    private long totalClients;
    private long totalAgents;
    private long totalAdmins;

    // Constructors
    public DashboardStats() {}

    public DashboardStats(long totalReclamations, Map<String, Long> statutCounts, Map<String, Long> prioriteCounts,
                          Map<String, Long> categorieCounts, Map<String, Long> agentWorkload,
                          List<ReclamationDTO> recentReclamations, long totalUsers, long totalComments,
                          long totalClients, long totalAgents, long totalAdmins) {
        this.totalReclamations = totalReclamations;
        this.statutCounts = statutCounts;
        this.prioriteCounts = prioriteCounts;
        this.categorieCounts = categorieCounts;
        this.agentWorkload = agentWorkload;
        this.recentReclamations = recentReclamations;
        this.totalUsers = totalUsers;
        this.totalComments = totalComments;
        this.totalClients = totalClients;
        this.totalAgents = totalAgents;
        this.totalAdmins = totalAdmins;
    }

    // Getters and Setters
    public long getTotalReclamations() {
        return totalReclamations;
    }

    public void setTotalReclamations(long totalReclamations) {
        this.totalReclamations = totalReclamations;
    }

    public Map<String, Long> getStatutCounts() {
        return statutCounts;
    }

    public void setStatutCounts(Map<String, Long> statutCounts) {
        this.statutCounts = statutCounts;
    }

    public Map<String, Long> getPrioriteCounts() {
        return prioriteCounts;
    }

    public void setPrioriteCounts(Map<String, Long> prioriteCounts) {
        this.prioriteCounts = prioriteCounts;
    }

    public Map<String, Long> getCategorieCounts() {
        return categorieCounts;
    }

    public void setCategorieCounts(Map<String, Long> categorieCounts) {
        this.categorieCounts = categorieCounts;
    }

    public Map<String, Long> getAgentWorkload() {
        return agentWorkload;
    }

    public void setAgentWorkload(Map<String, Long> agentWorkload) {
        this.agentWorkload = agentWorkload;
    }

    public List<ReclamationDTO> getRecentReclamations() {
        return recentReclamations;
    }

    public void setRecentReclamations(List<ReclamationDTO> recentReclamations) {
        this.recentReclamations = recentReclamations;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public long getTotalClients() {
        return totalClients;
    }

    public void setTotalClients(long totalClients) {
        this.totalClients = totalClients;
    }

    public long getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(long totalAgents) {
        this.totalAgents = totalAgents;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }
}
