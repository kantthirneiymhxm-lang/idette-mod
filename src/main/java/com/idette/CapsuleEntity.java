package com.idette;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** แคปซูลตกลงมาระเบิด แล้วสาวคนที่เลือกออกมา */
public class CapsuleEntity extends ThrownItemEntity {
    private int which = 0;
    public CapsuleEntity(EntityType<CapsuleEntity> t, World w) { super(t, w); }
    public CapsuleEntity(EntityType<CapsuleEntity> t, double x, double y, double z, World w) { super(t, x, y, z, w); }
    public void setWhich(int w) { which = w; }

    @Override protected Item getDefaultItem() { return Items.IRON_BLOCK; }

    @Override
    protected void onCollision(HitResult r) {
        super.onCollision(r);
        if (getWorld().isClient) return;
        ServerWorld sw = (ServerWorld) getWorld();
        sw.createExplosion(this, getX(), getY(), getZ(), 3.0f, World.ExplosionSourceType.NONE);
        String msg;
        Entity e;
        switch (which) {
            case 1 -> {
                e = IdetteMod.SUJIN.create(sw);
                msg = "แคปซูลตกลงมา! ซูจิน (ผมฟ้า) ออกมาแล้ว — ดูแลเธอ: ให้อาหาร ดอกไม้ ลูบหัว (คลิกขวา)";
            }
            case 2 -> {
                e = IdetteMod.CHANEL.create(sw);
                msg = "แคปซูลตกลงมา! ชาแนล (ผมชมพู) ออกมาแล้ว — Shift+คลิกขวามือเปล่าเพื่อดูของที่แลกได้";
            }
            default -> {
                IdetteEntity i = IdetteMod.IDETTE.create(sw);
                if (i != null) i.setHealth(6.0f);
                e = i;
                msg = "แคปซูลตกลงมา! ไอเด็ตต์บาดเจ็บอยู่ — ให้แอปเปิลทองเพื่อรักษาเธอ";
            }
        }
        if (e != null) {
            e.refreshPositionAndAngles(getX(), getY(), getZ(), 0, 0);
            sw.spawnEntity(e);
            sw.getPlayers(p -> p.squaredDistanceTo(this) < 96 * 96).forEach(p -> p.sendMessage(Text.literal(msg), false));
        }
        discard();
    }
}
