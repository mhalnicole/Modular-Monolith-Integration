package edu.cit.patonog.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_cursor")
class ChannelCursor {

    @Id
    private Integer id;

    @Column(name = "next_cursor", nullable = false)
    private Long nextCursor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ChannelCursor() {}

    public ChannelCursor(Integer id, Long nextCursor) {
        this.id = id;
        this.nextCursor = nextCursor;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(Long nextCursor) {
        this.nextCursor = nextCursor;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
