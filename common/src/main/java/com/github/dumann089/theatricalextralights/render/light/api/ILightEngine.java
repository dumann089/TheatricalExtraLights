package com.github.dumann089.theatricalextralights.render.light.api;

/**
 * Contrato de abstracción para el backend de iluminación.
 * Desacopla Theatrical Extra Lights del motor de renderizado subyacente.
 */
public interface ILightEngine {

    /**
     * Prepara el motor para su uso.
     */
    void init();

    /**
     * Envía la configuración de una luz a la cola de renderizado del frame actual.
     * @param state El estado actual de la luz mutable.
     */
    void submitLight(LightState state);

    /**
     * Limpia la cola de luces sin enviarla a la GPU (útil si se apagan los shaders temporalmente).
     */
    void clear();

    /**
     * Despacha todas las luces acumuladas a la GPU.
     */
    void flush();

    /**
     * @return true si el motor está cargado y listo para recibir llamadas.
     */
    boolean isActive();
}