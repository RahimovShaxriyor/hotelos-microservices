package com.hotelos.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class EnvelopeFingerprint {
    private static final ObjectMapper CANONICAL_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private EnvelopeFingerprint() {}

    public static String computeSha256(EventEnvelope<?> envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("EventEnvelope cannot be null for fingerprinting");
        }
        try {
            JsonNode tree = CANONICAL_MAPPER.valueToTree(envelope);
            JsonNode sortedTree = recursivelySortObjectKeys(tree);
            byte[] canonicalBytes = CANONICAL_MAPPER.writeValueAsBytes(sortedTree);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonicalBytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute canonical envelope fingerprint", ex);
        }
    }

    private static JsonNode recursivelySortObjectKeys(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            Iterator<String> fieldNames = objNode.fieldNames();
            List<String> sortedKeys = new ArrayList<>();
            fieldNames.forEachRemaining(sortedKeys::add);
            Collections.sort(sortedKeys);

            ObjectNode sortedObj = JsonNodeFactory.instance.objectNode();
            for (String key : sortedKeys) {
                sortedObj.set(key, recursivelySortObjectKeys(objNode.get(key)));
            }
            return sortedObj;
        } else if (node.isArray()) {
            ArrayNode arrNode = (ArrayNode) node;
            ArrayNode sortedArr = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : arrNode) {
                sortedArr.add(recursivelySortObjectKeys(item));
            }
            return sortedArr;
        }
        return node;
    }
}
