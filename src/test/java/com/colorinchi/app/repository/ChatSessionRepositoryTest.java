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
import com.colorinchi.app.model.ChatSession;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatSessionRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private ChatSessionRepository repository;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        anonymousOwnerRepository.save(createOwner(OWNER_ID));
        anonymousOwnerRepository.save(createOwner(OTHER_OWNER_ID));
    }

    @Test
    void saveAndFindById() {
        ChatSession session = createSession(OWNER_ID, "Test Chat", "gpt-4o");
        ChatSession saved = repository.save(session);

        assertThat(saved.getId()).isNotNull();

        Optional<ChatSession> found = repository.findByIdAndOwnerId(saved.getId(), OWNER_ID);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Chat");
        assertThat(found.get().getModel()).isEqualTo("gpt-4o");
        assertThat(found.get().getStatus()).isEqualTo("active");
    }

    @Test
    void findAllByOwnerIdOrderByUpdatedAtDescReturnsOwnSessions() {
        repository.save(createSession(OWNER_ID, "Chat A", "gpt-4o"));
        repository.save(createSession(OWNER_ID, "Chat B", "gpt-4o"));
        repository.save(createSession(OTHER_OWNER_ID, "Chat C", "gpt-4o"));

        List<ChatSession> sessions = repository.findAllByOwnerIdOrderByUpdatedAtDesc(OWNER_ID);

        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(ChatSession::getTitle)
                .containsExactly("Chat B", "Chat A");
    }

    @Test
    void findByIdAndOwnerIdDoesNotReturnForeignSession() {
        ChatSession foreign = repository.save(createSession(OTHER_OWNER_ID, "Foreign", "gpt-4o"));

        Optional<ChatSession> result = repository.findByIdAndOwnerId(foreign.getId(), OWNER_ID);

        assertThat(result).isNotPresent();
    }

    @Test
    void deleteByIdAndOwnerIdRemovesOwnSession() {
        ChatSession saved = repository.save(createSession(OWNER_ID, "To Delete", "gpt-4o"));
        assertThat(repository.findByIdAndOwnerId(saved.getId(), OWNER_ID)).isPresent();

        long deleted = repository.deleteByIdAndOwnerId(saved.getId(), OWNER_ID);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findByIdAndOwnerId(saved.getId(), OWNER_ID)).isNotPresent();
    }

    @Test
    void deleteByIdAndOwnerIdDoesNotDeleteForeignSession() {
        ChatSession foreign = repository.save(createSession(OTHER_OWNER_ID, "Foreign", "gpt-4o"));

        long deleted = repository.deleteByIdAndOwnerId(foreign.getId(), OWNER_ID);

        assertThat(deleted).isZero();
        assertThat(repository.findByIdAndOwnerId(foreign.getId(), OTHER_OWNER_ID)).isPresent();
    }

    // --- Helpers ---

    private ChatSession createSession(UUID ownerId, String title, String model) {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID());
        session.setOwnerId(ownerId);
        session.setTitle(title);
        session.setModel(model);
        session.setStatus("active");
        return session;
    }

    private AnonymousOwner createOwner(UUID ownerId) {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(ownerId);
        owner.setBootstrap(false);
        return owner;
    }
}
