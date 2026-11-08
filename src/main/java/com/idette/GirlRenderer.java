package com.idette;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public class GirlRenderer<E extends MobEntity> extends BipedEntityRenderer<E, BipedEntityModel<E>> {
    private final Identifier tex;
    private final float size;
    public GirlRenderer(EntityRendererFactory.Context ctx, String name) { this(ctx, name, 1f); }
    public GirlRenderer(EntityRendererFactory.Context ctx, String name, float size) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f * size);
        this.tex = new Identifier(IdetteMod.MOD_ID, "textures/entity/" + name + ".png");
        this.size = size;
    }
    @Override protected void scale(E e, MatrixStack m, float f) { m.scale(size, size, size); }
    @Override public Identifier getTexture(E e) { return tex; }
}
