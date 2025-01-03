// Message.java
package org.example;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;
    private final MessageType type;
    private final String targetGroup;  // null for direct messages

    public enum MessageType {
        DIRECT,
        GROUP,
        SYSTEM
    }

    public Message(String sender, String content, MessageType type, String targetGroup) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.targetGroup = targetGroup;
    }

    public Message(String sender, String content) {
        this(sender, content, MessageType.DIRECT, null);
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public MessageType getType() { return type; }
    public String getTargetGroup() { return targetGroup; }

    public String getFormattedMessage() {
        return String.format("[%s] %s: %s",
                timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                sender,
                content);
    }
}
