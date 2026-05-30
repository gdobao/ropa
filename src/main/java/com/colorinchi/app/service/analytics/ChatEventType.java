package com.colorinchi.app.service.analytics;

/**
 * Event taxonomy for the premium fashion chat analytics pipeline.
 * Each constant maps to a chat_analytics_events.event_type value.
 */
public enum ChatEventType {

    /** A new chat session was created. */
    SESSION_CREATED,

    /** A user sent a message. */
    MESSAGE_SENT,

    /** An assistant response (full) was received or stream completed. */
    MESSAGE_RECEIVED,

    /** An AI stream started delivering tokens. */
    STREAM_STARTED,

    /** A single chunk within an active stream (optional — use when batching). */
    STREAM_CHUNK,

    /** An AI stream completed normally. */
    STREAM_COMPLETED,

    /** An AI stream was disconnected or cancelled mid-way. */
    STREAM_DISCONNECTED,

    /** A run (model invocation) started execution. */
    RUN_STARTED,

    /** A run completed successfully. */
    RUN_COMPLETED,

    /** A run failed with an error. */
    RUN_FAILED,

    /** A policy evaluation blocked the request (e.g. outfit_request). */
    POLICY_BLOCK,

    /** A policy evaluation flagged the request (e.g. styling/color advice). */
    POLICY_FLAG,

    /** A rate-limit was exceeded for the owner. */
    RATE_LIMIT_HIT,

    /** Feedback (thumbs up/down) was submitted for a run. */
    FEEDBACK_SUBMITTED,

    /** The user switched AI model mid-session. */
    MODEL_SWITCHED,

    /** A generic or unexpected error occurred. */
    ERROR_GENERIC
}
