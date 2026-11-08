package com.idette;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** ซูจิน (ผมฟ้า): ดูแลเธอ (ให้อาหาร/ดอกไม้/ลูบหัว) จน "ความรัก" ถึง 100 แล้วเธอจะเป็นแฟน */
public class SujinEntity extends GirlEntity {
    private int care = 10;
    private long nextFeed = 0, nextPet = 0, nextHug = 0;

    public SujinEntity(EntityType<? extends PathAwareEntity> t, World w) { super(t, w); }

    private void addCare(PlayerEntity p, int n) {
        care = Math.min(100, care + n);
        ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 4, .3, .3, .3, 0);
        if (care >= 100 && owner == null) {
            owner = p.getUuid();
            p.sendMessage(Text.literal("ซูจินยอมรับใจคุณแล้ว! ตอนนี้เธอเป็นแฟนคุณ (นอนบนเตียงใกล้ๆ เธอได้)"), false);
        } else if (owner == null) {
            p.sendMessage(Text.literal("ซูจิน ความรัก " + care + "/100"), true);
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity p, Hand hand) {
        if (getWorld().isClient) return ActionResult.SUCCESS;
        ItemStack s = p.getStackInHand(hand);
        long t = getWorld().getTime();

        if (s.getItem().isFood() && t >= nextFeed) {
            nextFeed = t + 100;
            int h = s.getItem().getFoodComponent().getHunger();
            if (!p.getAbilities().creativeMode) s.decrement(1);
            heal(h);
            if (owner == null) addCare(p, 2 + h);
            return ActionResult.SUCCESS;
        }
        if (s.isIn(ItemTags.SMALL_FLOWERS)) {
            if (!p.getAbilities().creativeMode) s.decrement(1);
            if (owner == null) addCare(p, 12);
            else ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 6, .3, .3, .3, 0);
            return ActionResult.SUCCESS;
        }
        if (s.isEmpty()) {
            if (owner == null) {
                if (t >= nextPet) { nextPet = t + 200; addCare(p, 3); }
                else p.sendMessage(Text.literal("ซูจิน ความรัก " + care + "/100 (ให้อาหาร/ดอกไม้ ได้เร็วกว่า)"), true);
                return ActionResult.SUCCESS;
            }
            if (isOwnedBy(p) && p.isSneaking()) { // กอด: ฟื้นพลัง
                if (t < nextHug) { p.sendMessage(Text.literal("ซูจินขอพักก่อนนะ (คูลดาวน์)"), true); return ActionResult.SUCCESS; }
                nextHug = t + 20 * 180;
                p.heal(10);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 2));
                ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.5, p.getZ(), 12, .5, .5, .5, 0);
                p.sendMessage(Text.literal("ซูจินกอดคุณ — ฟื้นพลัง!"), true);
                return ActionResult.SUCCESS;
            }
            p.sendMessage(Text.literal("ซูจิน: ความสนิท " + bond), true);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(p, hand);
    }

    @Override
    public boolean damage(DamageSource src, float amount) {
        if (!getWorld().isClient && owner == null && src.getAttacker() instanceof PlayerEntity) care = Math.max(0, care - 20);
        return super.damage(src, amount);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        // ยังไม่เป็นแฟน: ความรักลดลงเรื่อยๆ ต้องคอยดูแล
        if (!getWorld().isClient && owner == null && age % 1200 == 0 && care > 0) care = Math.max(0, care - 2);
    }

    @Override public void writeCustomDataToNbt(NbtCompound n) { super.writeCustomDataToNbt(n); n.putInt("Care", care); }
    @Override public void readCustomDataFromNbt(NbtCompound n) { super.readCustomDataFromNbt(n); care = n.getInt("Care"); }
}
