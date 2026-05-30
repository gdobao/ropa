package com.colorinchi.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProviderResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProviderResponseParser(objectMapper);
    }

    @Test
    void parsesTextChunk() {
        var result = parser.parseLines(Flux.just(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"},\"index\":0}]}"))
                .collectList()
                .block();

        assertThat(result).containsExactly("Hello");
    }

    @Test
    void parsesMultipleChunks() {
        var result = parser.parseLines(Flux.just(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"},\"index\":0}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\" world\"},\"index\":0}]}"))
                .collectList()
                .block();

        assertThat(result).containsExactly("Hello", " world");
    }

    @Test
    void skipsNonDataLines() {
        var result = parser.parseLines(Flux.just(
                ": keep-alive comment",
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"},\"index\":0}]}",
                "",
                "event: done"))
                .collectList()
                .block();

        assertThat(result).containsExactly("Hi");
    }

    @Test
    void skipsDoneMarker() {
        var result = parser.parseLines(Flux.just(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Bye\"},\"index\":0}]}",
                "data: [DONE]"))
                .collectList()
                .block();

        assertThat(result).containsExactly("Bye");
    }

    @Test
    void skipsEmptyContentFromRoleOnlyChunk() {
        var result = parser.parseLines(Flux.just(
                "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\"},\"index\":0}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\"Text\"},\"index\":0}]}"))
                .collectList()
                .block();

        assertThat(result).containsExactly("Text");
    }

    @Test
    void handlesInvalidJsonGracefully() {
        var result = parser.parseLines(Flux.just(
                "data: not valid json",
                "data: {\"choices\":[{\"delta\":{\"content\":\"OK\"},\"index\":0}]}"))
                .collectList()
                .block();

        assertThat(result).containsExactly("OK");
    }

    @Test
    void returnsEmptyForNoDataLines() {
        var result = parser.parseLines(Flux.just(
                "event: ping",
                "",
                ": comment"))
                .collectList()
                .block();

        assertThat(result).isEmpty();
    }

    @Test
    void skipsBlankLines() {
        var result = parser.parseLines(Flux.just(
                "",
                "  ",
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"},\"index\":0}]}",
                "\t"))
                .collectList()
                .block();

        assertThat(result).containsExactly("Hi");
    }

    @Test
    void extractContentWithValidChunkReturnsText() {
        String data = "{\"choices\":[{\"delta\":{\"content\":\"Chunk\"},\"index\":0}]}";
        assertThat(parser.extractContent(data)).isEqualTo("Chunk");
    }

    @Test
    void extractContentWithInvalidChunkReturnsEmpty() {
        assertThat(parser.extractContent("invalid json")).isEmpty();
    }

    @Test
    void extractContentWithNoContentReturnsEmpty() {
        String data = "{\"choices\":[{\"delta\":{\"role\":\"assistant\"},\"index\":0}]}";
        assertThat(parser.extractContent(data)).isEmpty();
    }
}
