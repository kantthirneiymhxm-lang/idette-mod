package com.idette;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class IdetteEntity extends GirlEntity {
    private boolean healed = false;
    private int anger = 0;
    private long fusionReady = 0;

    private static final Set<net.minecraft.item.Item> SWEETS = Set.of(
        Items.COOKIE, Items.CAKE, Items.SWEET_BERRIES, Items.HONEY_BOTTLE, Items.PUMPKIN_PIE, Items.SUGAR);

    public IdetteEntity(EntityType<? extends PathAwareEntity> t, World w) { super(t, w); }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new CandyAttackGoal());
        goalSelector.add(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override public boolean canStart() { return super.canStart() && (vengeful || !(getTarget() instanceof PlayerEntity)); }
        });
        goalSelector.add(3, new FollowOwnerGoal());
        goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false,
            e -> healed && anger <= 0));
    }

    // ---------- state ----------
    public boolean isAngry() { return anger > 0; }
    public void setAnger(int t) { anger = t; if (t <= 0) setTarget(null); }
    public boolean isOwnedBy(PlayerEntity p) { return healed && p.getUuid().equals(owner); }
    public void addBond() { bond++; }
    public int getBond() { return bond; }
    @Override public void calm() { setAnger(0); }
    @Override public void claim(PlayerEntity p) { healed = true; super.claim(p); }
    @Override public void beginRevenge() { super.beginRevenge(); anger = 20 * 600; }
    @Override protected void endRevenge() { super.endRevenge(); setAnger(0); }

    // ---------- interaction ----------
    @Override
    public ActionResult interactMob(PlayerEntity p, Hand hand) {
        ItemStack s = p.getStackInHand(hand);
        if (getWorld().isClient) return ActionResult.SUCCESS;

        if (s.isOf(Items.GOLDEN_APPLE)) { // รักษา
            heal(10f);
            if (!p.getAbilities().creativeMode) s.decrement(1);
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.5, getZ(), 8, .4, .4, .4, 0);
            if (!healed && getHealth() >= getMaxHealth() - 0.5f) {
                healed = true; owner = p.getUuid();
                p.sendMessage(Text.literal("รักษาสำเร็จ! ไอเด็ตต์จะอยู่ใช้ชีวิตกับคุณ (นอนบนเตียงใกล้ๆ เธอได้)"), false);
            } else if (!healed) {
                p.sendMessage(Text.literal("ไอเด็ตต์ " + (int) getHealth() + "/" + (int) getMaxHealth() + " — ให้แอปเปิลทองอีก"), true);
            }
            return ActionResult.SUCCESS;
        }
        if (SWEETS.contains(s.getItem())) { // ง้อ
            if (!p.getAbilities().creativeMode) s.decrement(1);
            setAnger(0);
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.5, getZ(), 8, .4, .4, .4, 0);
            return ActionResult.SUCCESS;
        }
        // แปลงร่าง/ฟิวชั่น: กด Shift + คลิกขวาด้วยมือเปล่า
        if (p.isSneaking() && s.isEmpty() && isOwnedBy(p)) {
            if (getWorld().getTime() < fusionReady) {
                p.sendMessage(Text.literal("ยังใช้พลังไม่ได้ (คูลดาวน์)"), true);
                return ActionResult.SUCCESS;
            }
            fusionReady = getWorld().getTime() + 20 * 120;
            int d = 20 * 60;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, d, 2));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, d, 2));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, d, 1));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, d, 0));
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, .5, 1, .5, .1);
            p.sendMessage(Text.literal("แปลงร่างร่วมกับไอเด็ตต์! (60 วินาที)"), true);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(p, hand);
    }

    @Override
    public boolean damage(DamageSource src, float amount) {
        if (src.isIn(DamageTypeTags.IS_EXPLOSION)) return false; // ไม่โดนแคนดี้ตัวเอง
        if (!getWorld().isClient && src.getAttacker() instanceof PlayerEntity p) {
            anger = 20 * 60;
            setTarget(p);
            p.sendMessage(Text.literal("ไอเด็ตต์โกรธแล้ว! (ง้อด้วยขนม: คุกกี้/เค้ก/น้ำตาล)"), true);
        }
        return super.damage(src, amount);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (!getWorld().isClient && anger > 0 && --anger == 0) setTarget(null);
    }

    // ---------- save ----------
    @Override public void writeCustomDataToNbt(NbtCompound n) {
        super.writeCustomDataToNbt(n);
        n.putBoolean("Healed", healed); n.putInt("Anger", anger); n.putInt("Bond", bond);
        if (owner != null) n.putUuid("Owner", owner);
    }
    @Override public void readCustomDataFromNbt(NbtCompound n) {
        super.readCustomDataFromNbt(n);
        healed = n.getBoolean("Healed"); anger = n.getInt("Anger"); bond = n.getInt("Bond");
        owner = n.containsUuid("Owner") ? n.getUuid("Owner") : null;
    }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }

    // ---------- goals ----------
    class CandyAttackGoal extends Goal {
        int cd = 40;
        CandyAttackGoal() { setControls(EnumSet.of(Control.LOOK)); }

        @Override public boolean canStart() {
            return isAngry() && getTarget() instanceof PlayerEntity t && t.isAlive();
        }

        @Override public boolean shouldRunEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().lookAt(t, 30, 30);
            if (--cd > 0) return;
            cd = 70 + random.nextInt(40);
            if (random.nextBoolean()) throwBomb(t); else dropGiant(t);
        }

        void throwBomb(LivingEntity t) { // ท่า 1
            CandyEntity c = new CandyEntity(IdetteMod.CANDY_BOMB, IdetteEntity.this, getWorld());
            double dx = t.getX() - c.getX(), dy = t.getBodyY(0.5) - c.getY(), dz = t.getZ() - c.getZ();
            c.setVelocity(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.1, dz, 1.2f, 1.5f);
            getWorld().spawnEntity(c);
        }

        void dropGiant(LivingEntity t) { // ท่า 2
            CandyEntity c = new CandyEntity(IdetteMod.GIANT_CANDY, t.getX(), t.getY() + 24, t.getZ(), getWorld());
            c.setOwner(IdetteEntity.this);
            c.setNoGravity(true);
            c.setVelocity(0, -0.4, 0);
            getWorld().spawnEntity(c);
            if (t instanceof PlayerEntity p) p.sendMessage(Text.literal("แคนดี้ยักษ์กำลังตกลงมา! หนีเร็ว!"), true);
        }
    }

    class FollowOwnerGoal extends Goal {
        PlayerEntity o;
        FollowOwnerGoal() { setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() {
            if (!healed || owner == null || isAngry()) return false;
            o = getWorld().getPlayerByUuid(owner);
            return o != null && squaredDistanceTo(o) > 25;
        }
        @Override public boolean shouldContinue() { return o != null && squaredDistanceTo(o) > 9 && !isAngry(); }
        @Override public void tick() {
            if (squaredDistanceTo(o) > 24 * 24) refreshPositionAndAngles(o.getX(), o.getY(), o.getZ(), 0, 0);
            else getNavigation().startMovingTo(o, 1.1);
        }
    }
}
