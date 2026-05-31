package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.model.ChatSurface;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

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

        Optional<ChatSession> found = repository.findByIdAndOwnerIdAndSurface(saved.getId(), OWNER_ID, ChatSurface.MAIN_CHAT);
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

        List<ChatSession> sessions = repository.findAllByOwnerIdAndSurfaceOrderByUpdatedAtDesc(OWNER_ID, ChatSurface.MAIN_CHAT);

        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(ChatSession::getTitle)
                .containsExactly("Chat B", "Chat A");
    }

    @Test
    void findByIdAndOwnerIdDoesNotReturnForeignSession() {
        ChatSession foreign = repository.save(createSession(OTHER_OWNER_ID, "Foreign", "gpt-4o"));

        Optional<ChatSession> result = repository.findByIdAndOwnerIdAndSurface(foreign.getId(), OWNER_ID, ChatSurface.MAIN_CHAT);

        assertThat(result).isNotPresent();
    }

    @Test
    void deleteByIdAndOwnerIdRemovesOwnSession() {
        ChatSession saved = repository.save(createSession(OWNER_ID, "To Delete", "gpt-4o"));
        assertThat(repository.findByIdAndOwnerIdAndSurface(saved.getId(), OWNER_ID, ChatSurface.MAIN_CHAT)).isPresent();

        long deleted = repository.deleteByIdAndOwnerIdAndSurface(saved.getId(), OWNER_ID, ChatSurface.MAIN_CHAT);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findByIdAndOwnerIdAndSurface(saved.getId(), OWNER_ID, ChatSurface.MAIN_CHAT)).isNotPresent();
    }

    @Test
    void deleteByIdAndOwnerIdDoesNotDeleteForeignSession() {
        ChatSession foreign = repository.save(createSession(OTHER_OWNER_ID, "Foreign", "gpt-4o"));

        long deleted = repository.deleteByIdAndOwnerIdAndSurface(foreign.getId(), OWNER_ID, ChatSurface.MAIN_CHAT);

        assertThat(deleted).isZero();
        assertThat(repository.findByIdAndOwnerIdAndSurface(foreign.getId(), OTHER_OWNER_ID, ChatSurface.MAIN_CHAT)).isPresent();

    }

    @Test
    void mainChatQueriesDoNotReturnCompanionSessions() {
        repository.save(createSession(OWNER_ID, "Main chat", "gpt-4o", ChatSurface.MAIN_CHAT));
        repository.save(createSession(OWNER_ID, "Companion", "gpt-4o", ChatSurface.COMPANION));

        List<ChatSession> sessions = repository.findAllByOwnerIdAndSurfaceOrderByUpdatedAtDesc(OWNER_ID, ChatSurface.MAIN_CHAT);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getTitle()).isEqualTo("Main chat");
    }

    @Test
    void persistedSurfaceUsesEnumStringContract() {
        ChatSession saved = repository.save(createSession(OWNER_ID, "Main chat", "gpt-4o", ChatSurface.MAIN_CHAT));
        entityManager.flush();

        String surface = jdbcTemplate.queryForObject(
                "SELECT surface FROM chat_sessions WHERE id = ?",
                String.class,
                saved.getId());

        assertThat(surface).isEqualTo(ChatSurface.MAIN_CHAT.name());
    }

    @Test
    void migratedUppercaseSurfaceValueMapsToEnum() {
        UUID sessionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO chat_sessions (id, owner_id, title, model, status, created_at, updated_at, archived, surface) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, ?)",
                sessionId,
                OWNER_ID,
                "Migrated chat",
                "gpt-4o",
                "active",
                ChatSurface.COMPANION.name());

        ChatSession found = repository.findById(sessionId).orElseThrow();

        assertThat(found.getSurface()).isEqualTo(ChatSurface.COMPANION);
    }

    // --- Helpers ---

    private ChatSession createSession(UUID ownerId, String title, String model) {
        return createSession(ownerId, title, model, ChatSurface.MAIN_CHAT);
    }

    private ChatSession createSession(UUID ownerId, String title, String model, ChatSurface surface) {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID());
        session.setOwnerId(ownerId);
        session.setTitle(title);
        session.setModel(model);
        session.setStatus("active");
        session.setSurface(surface);
        return session;
    }

    private AnonymousOwner createOwner(UUID ownerId) {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(ownerId);
        owner.setBootstrap(false);
        return owner;
    }
}
