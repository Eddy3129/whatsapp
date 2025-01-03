// Group.java
package org.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String admin;
    private final Set<String> members;
    private final Set<String> pendingInvites;

    public Group(String name, String admin) {
        this.name = name;
        this.admin = admin;
        this.members = new HashSet<>();
        this.pendingInvites = new HashSet<>();
        members.add(admin);
    }

    public String getName() { return name; }
    public String getAdmin() { return admin; }
    public Set<String> getMembers() { return new HashSet<>(members); }
    public Set<String> getPendingInvites() { return new HashSet<>(pendingInvites); }

    public void addMember(String username) {
        members.add(username);
        pendingInvites.remove(username);
    }

    public void addInvite(String username) {
        if (!members.contains(username)) {
            pendingInvites.add(username);
        }
    }

    public boolean hasPendingInvite(String username) {
        return pendingInvites.contains(username);
    }

    public boolean isMember(String username) {
        return members.contains(username);
    }

    // Add missing method
    public void removeMember(String username) {
        members.remove(username);
    }

    // Add member count method
    public int getMemberCount() {
        return members.size();
    }

    // Add message persistence
    private List<Message> messageHistory = new ArrayList<>();

    public void addMessage(Message message) {
        messageHistory.add(message);
    }

    public List<Message> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
}
