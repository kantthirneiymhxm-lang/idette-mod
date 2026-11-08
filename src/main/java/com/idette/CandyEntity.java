package com.idette;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** ท่า1 = ลูกอมระเบิด, ท่า2 = แคนดี้ยักษ์ (ใช้คลาสเดียว ต่างกันที่ EntityType) */
public class CandyEntity extends ThrownItemEntity {
    public CandyEntity(EntityType<CandyEntity> t, World w) { super(t, w); }
    public CandyEntity(EntityType<CandyEntity> t, LivingEntity owner, World w) { super(t, owner, w); }
    public CandyEntity(EntityType<CandyEntity> t, double x, double y, double z, World w) { super(t, x, y, z, w); }

    private boolean giant() { return getType() == IdetteMod.GIANT_CANDY; }

    @Override
    protected Item getDefaultItem() { return giant() ? Items.SWEET_BERRIES : Items.SUGAR; }

    @Override
    protected void onCollision(HitResult r) {
        super.onCollision(r);
        if (getWorld().isClient) return;
        getWorld().createExplosion(this, getX(), getY(), getZ(),
            giant() ? 4.5f : 2.0f, World.ExplosionSourceType.NONE); // ไม่ทำลายบล็อก
        discard();
    }
}
