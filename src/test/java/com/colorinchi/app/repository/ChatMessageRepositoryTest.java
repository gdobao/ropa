package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSession;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatMessageRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    private UUID sessionId;
    private UUID foreignSessionId;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        sessionRepository.deleteAll();
        anonymousOwnerRepository.deleteAll();

        anonymousOwnerRepository.save(createOwner(OWNER_ID));
        anonymousOwnerRepository.save(createOwner(OTHER_OWNER_ID));

        sessionId = sessionRepository.save(createSession(OWNER_ID)).getId();
        foreignSessionId = sessionRepository.save(createSession(OTHER_OWNER_ID)).getId();
    }

    @Test
    void saveAndFindById() {
        ChatMessage msg = createMessage(sessionId, OWNER_ID, "user", "Hello");
        ChatMessage saved = messageRepository.save(msg);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getContent()).isEqualTo("Hello");
    }

    @Test
    void findAllBySessionIdAndOwnerIdOrderByCreatedAtAscReturnsOwnMessages() {
        messageRepository.save(createMessage(sessionId, OWNER_ID, "user", "First"));
        messageRepository.save(createMessage(sessionId, OWNER_ID, "assistant", "Reply"));
        messageRepository.save(createMessage(foreignSessionId, OTHER_OWNER_ID, "user", "Foreign"));

        List<ChatMessage> messages = messageRepository
                .findAllBySessionIdAndOwnerIdOrderByCreatedAtAsc(sessionId, OWNER_ID);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("First");
        assertThat(messages.get(1).getContent()).isEqualTo("Reply");
    }

    @Test
    void findByIdAndSessionIdAndOwnerIdScopesCorrectly() {
        ChatMessage own = messageRepository.save(createMessage(sessionId, OWNER_ID, "user", "Own"));
        ChatMessage foreign = messageRepository.save(createMessage(foreignSessionId, OTHER_OWNER_ID, "user", "Foreign"));

        Optional<ChatMessage> foundOwn = messageRepository
                .findByIdAndSessionIdAndOwnerId(own.getId(), sessionId, OWNER_ID);
        Optional<ChatMessage> foundForeign = messageRepository
                .findByIdAndSessionIdAndOwnerId(foreign.getId(), foreignSessionId, OWNER_ID);

        assertThat(foundOwn).isPresent();
        assertThat(foundForeign).isNotPresent();
    }

    @Test
    void countBySessionIdAndOwnerIdReturnsOwnCount() {
        messageRepository.save(createMessage(sessionId, OWNER_ID, "user", "A"));
        messageRepository.save(createMessage(sessionId, OWNER_ID, "assistant", "B"));
        messageRepository.save(createMessage(foreignSessionId, OTHER_OWNER_ID, "user", "C"));

        long count = messageRepository.countBySessionIdAndOwnerId(sessionId, OWNER_ID);

        assertThat(count).isEqualTo(2);
    }

    // --- Helpers ---

    private ChatSession createSession(UUID ownerId) {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID());
        session.setOwnerId(ownerId);
        session.setTitle("Test");
        session.setModel("gpt-4o");
        session.setStatus("active");
        return session;
    }

    private ChatMessage createMessage(UUID sessionId, UUID ownerId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID());
        msg.setSessionId(sessionId);
        msg.setOwnerId(ownerId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTokens(0);
        return msg;
    }

    private AnonymousOwner createOwner(UUID ownerId) {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(ownerId);
        owner.setBootstrap(false);
        return owner;
    }
}
