# I/O & Event Bus Subsystem (`modules/io`)

> Central inter-process communication (IPC), process stream interception (stdout/stderr), and asynchronous event routing engine for Antigravity on Android.

---

## 1. Architectural Overview

The `modules/io` subsystem implements an asynchronous **Publish/Subscribe Event Bus** equipped with non-blocking stream interceptors and transport adapters (Named FIFOs, UNIX Domain Sockets, and Android Broadcast Intents):

```
 +-------------------------------------------------------------------------------+
 |                               INPUT SOURCES                                   |
 |  [broadcast_receiver.sh]       [fifo_input.py]       [unix_socket_input.py]   |
 |   (Android am Intents)          (Named FIFO)           (AF_UNIX Stream)       |
 +-----------------------+---------------+-----------------------+---------------+
                         |               |                       |
                         +---------------+-----------------------+
                                         |
                                         v  JSON Event Envelopes
 +-------------------------------------------------------------------------------+
 |                              EVENT BROKER CORE                                |
 |                              [event_broker.py]                                |
 |              (Topic dispatch, channel subscription, ring buffers)             |
 +---------------------------------------+---------------------------------------+
                                         |
                                         v  Dispatched Streams
 +-------------------------------------------------------------------------------+
 |                               OUTPUT SINKS                                    |
 |     [stream_splitter.py]            [file_sink.py]         [socket_sink.py]   |
 |   (stdout/stderr tees)           (Rolling log files)     (Client listeners)   |
 +-------------------------------------------------------------------------------+
```

---

## 2. Directory Layout & Key Modules

```
modules/io/
├── event_broker.py         # In-memory pub/sub broker with priority queues
├── envelope.py             # Event envelope serialization and schema validation
├── stream_splitter.py      # Non-blocking stream tee for subprocess stdout/stderr
├── fifo_io.py              # Named FIFO pipe reader and writer
├── unix_socket_io.py       # Local AF_UNIX domain socket server and client
├── broadcast_receiver.sh   # Android intent bridge parsing am broadcast payloads
├── io_hub.py               # Central management daemon managing active I/O channels
└── tests/                  # Automated unit and integration tests
```

---

## 3. Key Capabilities

1. **Non-Blocking Stream Splitting:** Intercepts language server stdout and stderr in real-time, streaming output to debug consoles while simultaneously routing structured telemetry to log sinks.
2. **Standardized Event Envelopes:** Enforces a unified JSON envelope schema (`timestamp`, `source`, `topic`, `payload`, `correlation_id`) across all transport channels.
3. **Android Platform Signal Ingestion:** Receives external operational triggers (e.g., low-battery warnings, screen state shifts, UI push events) via Android broadcast intents.

---

## 4. Testing & Verification

Run the test suite:

```bash
python3 -m unittest discover -s modules/io/tests
```
