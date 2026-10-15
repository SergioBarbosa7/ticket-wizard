package br.com.ticket.wizard.base;

import com.google.protobuf.Timestamp;

import java.time.OffsetDateTime;

public final class Tempo {

    private Tempo() {
    }

    public static Timestamp paraTimestamp(OffsetDateTime instante) {
        return Timestamp.newBuilder()
                        .setSeconds(instante.toEpochSecond())
                        .setNanos(instante.getNano())
                        .build();
    }
}
