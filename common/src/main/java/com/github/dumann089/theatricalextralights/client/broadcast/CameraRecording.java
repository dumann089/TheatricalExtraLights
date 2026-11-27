package com.github.dumann089.theatricalextralights.client.broadcast;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma gravação de movimentos de câmera: um quadro por tick do cliente (20 por segundo). Serve a todas
 * as variantes (broadcast e jib); o que não se aplica à variante fica em 0. Salva como JSON.
 */
public class CameraRecording {

    public static final int TICKS_PER_SECOND = 20;

    // Colunas de cada quadro.
    public static final int YAW = 0;         // pan da cabeça, contínuo (sem salto em ±180)
    public static final int PITCH = 1;       // tilt da cabeça
    public static final int ZOOM = 2;
    public static final int EXPOSURE = 3;    // em terços de EV
    public static final int BOOM_YAW = 4;    // jib: giro do braço, contínuo
    public static final int BOOM_PITCH = 5;  // jib: elevação do braço
    public static final int EXTENSION = 6;   // altura do tripé / comprimento do braço
    public static final int MOVE_X = 7;      // comando de velocidade do carrinho (blocos por tick)
    public static final int MOVE_Z = 8;
    public static final int COLUMNS = 9;

    public int version = 1;
    public String name = "";
    public boolean jib;
    public List<float[]> frames = new ArrayList<>();

    /** Onde a câmera estava ao começar a gravar (a shoulder camera reaparece aí). */
    public boolean hasOrigin;
    public double originX;
    public double originY;
    public double originZ;
    public float originYaw;
    public String dimension = "";

    /** Arquivo de origem (não vai para o JSON). */
    public transient String fileName;

    public float durationSeconds() {
        return frames.size() < 2 ? 0f : (frames.size() - 1) / (float) TICKS_PER_SECOND;
    }

    public boolean isValid() {
        if (frames == null || frames.size() < 2) {
            return false;
        }
        for (float[] frame : frames) {
            if (frame == null || frame.length != COLUMNS) {
                return false;
            }
        }
        return true;
    }

    public static String clock(float seconds) {
        int total = Math.max(0, (int) seconds);
        return String.format(java.util.Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }
}
