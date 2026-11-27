package com.github.dumann089.theatricalextralights.client.jumbotron;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.function.IntSupplier;

/**
 * "Textura" que não carrega nada: só aponta para o id GL da imagem de um canal do jumbotron. Registrada no
 * TextureManager com um ResourceLocation fixo por slot, para que um RenderType comum possa usá-la (e então
 * os desenhos da tela entram nos lotes normais do mundo, sem desenho imediato fora de ordem).
 */
final class JumbotronFeedTexture extends AbstractTexture {

    private final IntSupplier idSupplier;

    JumbotronFeedTexture(IntSupplier idSupplier) {
        this.idSupplier = idSupplier;
    }

    @Override
    public void load(ResourceManager resourceManager) {
        // nada a carregar: a imagem é desenhada pelo JumbotronFeed
    }

    @Override
    public int getId() {
        return idSupplier.getAsInt();
    }

    @Override
    public void releaseId() {
        // o JumbotronFeed é o dono da textura
    }

    @Override
    public void close() {
        // idem
    }
}
