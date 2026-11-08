package com.idette;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class IdetteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(IdetteMod.IDETTE, c -> new GirlRenderer<IdetteEntity>(c, "idette"));
        EntityRendererRegistry.register(IdetteMod.SUJIN, c -> new GirlRenderer<SujinEntity>(c, "sujin"));
        EntityRendererRegistry.register(IdetteMod.CHANEL, c -> new GirlRenderer<ChanelEntity>(c, "chanel"));
        EntityRendererRegistry.register(IdetteMod.CHILD, c -> new GirlRenderer<ChildEntity>(c, "child", 0.5f));
        EntityRendererRegistry.register(IdetteMod.CAPSULE, c -> new FlyingItemEntityRenderer<>(c, 4.0f, true));
        EntityRendererRegistry.register(IdetteMod.CANDY_BOMB, c -> new FlyingItemEntityRenderer<>(c, 1.5f, true));
        EntityRendererRegistry.register(IdetteMod.GIANT_CANDY, c -> new FlyingItemEntityRenderer<>(c, 10.0f, true));
    }
}
