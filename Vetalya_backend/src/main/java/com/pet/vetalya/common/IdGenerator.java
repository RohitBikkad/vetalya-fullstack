package com.pet.vetalya.common;

import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Component;

/**
 * Generates ULID-based string identifiers for entities.
 * ULIDs are lexicographically sortable, 26-char Crockford Base32 strings
 * that encode a 48-bit millisecond timestamp + 80-bit random component.
 */
@Component
public class IdGenerator {

    /**
     * @return a new monotonic ULID string (26 characters)
     */
    public String newId() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
