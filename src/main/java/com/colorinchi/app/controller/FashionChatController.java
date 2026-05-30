package com.colorinchi.app.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.dto.chat.WardrobeContext;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.ModelRouter;
import com.colorinchi.app.service.WardrobeContextAssembler;

@Controller
public class FashionChatController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ModelRouter modelRouter;
    private final WardrobeContextAssembler wardrobeContextAssembler;

    public FashionChatController(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ModelRouter modelRouter,
            WardrobeContextAssembler wardrobeContextAssembler) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.modelRouter = modelRouter;
        this.wardrobeContextAssembler = wardrobeContextAssembler;
    }

    @GetMapping("/chat")
    String chatIndex(Model model) {
        List<ChatSession> sessions = chatSessionService.listByOwner();
        List<AiModelConfig> models = modelRouter.getAvailableModels();
        AiModelConfig defaultModel = modelRouter.findDefault();
        WardrobeContext ctx = wardrobeContextAssembler.assemble();

        model.addAttribute("sessions", sessions);
        model.addAttribute("models", models);
        model.addAttribute("defaultModel", defaultModel);
        model.addAttribute("wardrobeContext", ctx);
        model.addAttribute("activeNav", "chat");
        return "chat";
    }

    @GetMapping("/chat/{sessionId}")
    String chatThread(@PathVariable UUID sessionId, Model model) {
        ChatSession session = chatSessionService.getById(sessionId);
        List<ChatMessage> messages = chatMessageService.listBySession(sessionId);
        List<ChatSession> sessions = chatSessionService.listByOwner();
        List<AiModelConfig> models = modelRouter.getAvailableModels();
        AiModelConfig defaultModel = modelRouter.findDefault();
        WardrobeContext ctx = wardrobeContextAssembler.assemble();

        model.addAttribute("currentSession", session);
        model.addAttribute("messages", messages);
        model.addAttribute("sessions", sessions);
        model.addAttribute("models", models);
        model.addAttribute("defaultModel", defaultModel);
        model.addAttribute("wardrobeContext", ctx);
        model.addAttribute("activeNav", "chat");
        return "chat";
    }
}
