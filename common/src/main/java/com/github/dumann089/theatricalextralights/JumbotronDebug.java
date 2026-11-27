package com.github.dumann089.theatricalextralights;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/** Log de diagnóstico da jumbotron, limitado a 1 linha a cada 3 s por chave (não enche o latest.log). */
public final class JumbotronDebug {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Long> LAST = new HashMap<>();
    /** Mude para false quando a jumbotron estiver funcionando. */
    public static final boolean ENABLED = true;

    private JumbotronDebug() {
    }

    public static synchronized void log(String key, String message) {
        if (!ENABLED) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST.get(key);
        if (last == null || now - last >= 3000L) {
            LAST.put(key, now);
            LOGGER.info("[JUMBOTRON-DEBUG] {}: {}", key, message);
        }
    }
}
