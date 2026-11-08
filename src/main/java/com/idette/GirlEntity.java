package com.idette;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** ฐานของสาวๆ: owner = null แปลว่ายังไม่ได้เป็นแฟน/เมีย */
public abstract class GirlEntity extends PathAwareEntity implements Girl {
    protected UUID owner = null;
    protected int bond = 0;
    protected long lastNight = -1;
    // ตั้งครรภ์
    protected boolean pregnant = false, keep = false;
    protected long due = 0;
    // โกรธแค้น (เลือก "เอาออก")
    protected boolean vengeful = false;
    protected int revengeTimer = 0;

    protected GirlEntity(EntityType<? extends PathAwareEntity> t, World w) {
        super(t, w);
        setPathfindingPenalty(PathNodeType.WATER, 0f); // เดินลงน้ำเล่นกับเราได้
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 30)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.add(2, new FollowOwnerGoal());
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        goalSelector.add(5, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, e -> owner != null && !vengeful));
    }

    // ---------- state ----------
    @Override public boolean isOwnedBy(PlayerEntity p) { return owner != null && owner.equals(p.getUuid()); }
    @Override public void addBond() { bond++; }
    @Override public int getBond() { return bond; }
    public UUID getOwnerId() { return owner; }
    public boolean isVengeful() { return vengeful; }
    public boolean isPregnant() { return pregnant; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }

    /** นอนได้คืนละครั้ง (นับตามวันของโลก) */
    public boolean markNight(long day) {
        if (day == lastNight) return false;
        lastNight = day; return true;
    }

    /** กลายเป็นแฟนของผู้เล่น */
    public void claim(PlayerEntity p) {
        owner = p.getUuid();
        clearPositionTarget();
        p.sendMessage(Text.literal(getName().getString() + " ยอมเป็นแฟนคุณแล้ว! พาเธอกลับโลกมนุษย์ด้วย /idette home"), false);
    }

    // ---------- ตั้งครรภ์ ----------
    public void tryConceive(ServerPlayerEntity p) {
        if (pregnant || bond < 5 || random.nextFloat() > 0.5f) return;
        pregnant = true; keep = false;
        due = getWorld().getTime() + 24000; // ครบกำหนดใน 1 วัน
        p.sendMessage(Text.literal(getName().getString() + " ตั้งครรภ์แล้ว! คุณจะเลือกอะไร?"), false);
        p.sendMessage(Text.literal("[เลี้ยงลูก]").styled(s -> s.withColor(Formatting.GREEN)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/idette keep")))
            .append(Text.literal("   "))
            .append(Text.literal("[เอาออก]").styled(s -> s.withColor(Formatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/idette end")))), false);
    }

    public void keepBaby(PlayerEntity p) {
        keep = true;
        p.sendMessage(Text.literal(getName().getString() + " ดีใจมาก — เธอจะคลอดลูกเมื่อครบกำหนด"), false);
    }

    /** เลือก "เอาออก": เธอโกรธแค้นและมาฆ่าเรา */
    public void endPregnancy(PlayerEntity p) {
        pregnant = false;
        p.sendMessage(Text.literal(getName().getString() + ": ...คุณทำแบบนี้ได้ยังไง!! (เธอมาฆ่าคุณ)"), false);
        beginRevenge();
    }

    public void beginRevenge() {
        vengeful = true; revengeTimer = 20 * 600;
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(60);
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4);
    }

    protected void endRevenge() {
        vengeful = false; owner = null; setTarget(null);
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(5);
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
    }

    private void giveBirth() {
        ServerWorld sw = (ServerWorld) getWorld();
        ChildEntity c = IdetteMod.CHILD.create(sw);
        if (c != null) {
            c.refreshPositionAndAngles(getX(), getY(), getZ(), 0, 0);
            c.owner = owner;
            sw.spawnEntity(c);
            sw.spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.5, getZ(), 12, .5, .5, .5, 0);
            PlayerEntity p = owner == null ? null : sw.getPlayerByUuid(owner);
            if (p != null) p.sendMessage(Text.literal(getName().getString() + " คลอดลูกแล้ว!"), false);
        }
        pregnant = false;
    }

    public static int choose(ServerPlayerEntity p, boolean keepIt) {
        List<GirlEntity> l = p.getServerWorld().getEntitiesByClass(GirlEntity.class,
            p.getBoundingBox().expand(64), g -> g.isOwnedBy(p) && g.isPregnant());
        if (l.isEmpty()) { p.sendMessage(Text.literal("ไม่มีใครตั้งครรภ์อยู่ใกล้ๆ"), false); return 0; }
        if (keepIt) l.get(0).keepBaby(p); else l.get(0).endPregnancy(p);
        return 1;
    }

    // ---------- tick ----------
    @Override
    public void tickMovement() {
        super.tickMovement();
        if (getWorld().isClient) return;
        PlayerEntity o = owner == null ? null : getWorld().getPlayerByUuid(owner);

        if (vengeful) {
            if (o != null && !o.isAlive() || --revengeTimer <= 0) { endRevenge(); return; }
            if (o != null) {
                setTarget(o);
                if (age % 80 == 0 && squaredDistanceTo(o) > 144)
                    refreshPositionAndAngles(o.getX() + 3, o.getY(), o.getZ(), 0, 0);
            }
            return;
        }
        if (pregnant && getWorld().getTime() >= due) giveBirth();

        // เล่นน้ำด้วยกัน
        if (o != null && age % 40 == 0 && isTouchingWater() && o.isTouchingWater() && squaredDistanceTo(o) < 36) {
            o.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 200, 0));
            o.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0));
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.4, getZ(), 3, .3, .3, .3, 0);
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 10, .5, .3, .5, 0);
        }
    }

    // ---------- save ----------
    @Override public void writeCustomDataToNbt(NbtCompound n) {
        super.writeCustomDataToNbt(n);
        n.putInt("Bond", bond); n.putLong("LastNight", lastNight);
        n.putBoolean("Pregnant", pregnant); n.putBoolean("Keep", keep); n.putLong("Due", due);
        n.putBoolean("Vengeful", vengeful); n.putInt("RevengeTimer", revengeTimer);
        if (owner != null) n.putUuid("Owner", owner);
        BlockPos h = getPositionTarget();
        if (hasPositionTarget()) { n.putInt("HomeX", h.getX()); n.putInt("HomeY", h.getY()); n.putInt("HomeZ", h.getZ()); n.putBoolean("HasHome", true); }
    }
    @Override public void readCustomDataFromNbt(NbtCompound n) {
        super.readCustomDataFromNbt(n);
        bond = n.getInt("Bond"); lastNight = n.contains("LastNight") ? n.getLong("LastNight") : -1;
        pregnant = n.getBoolean("Pregnant"); keep = n.getBoolean("Keep"); due = n.getLong("Due");
        vengeful = n.getBoolean("Vengeful"); revengeTimer = n.getInt("RevengeTimer");
        owner = n.containsUuid("Owner") ? n.getUuid("Owner") : null;
        if (n.getBoolean("HasHome")) setPositionTarget(new BlockPos(n.getInt("HomeX"), n.getInt("HomeY"), n.getInt("HomeZ")), 24);
        if (vengeful) beginRevenge();
    }

    class FollowOwnerGoal extends Goal {
        PlayerEntity o;
        FollowOwnerGoal() { setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() {
            if (owner == null || vengeful) return false;
            o = getWorld().getPlayerByUuid(owner);
            return o != null && squaredDistanceTo(o) > 25;
        }
        @Override public boolean shouldContinue() { return o != null && !vengeful && squaredDistanceTo(o) > 9; }
        @Override public void tick() {
            if (squaredDistanceTo(o) > 24 * 24) refreshPositionAndAngles(o.getX(), o.getY(), o.getZ(), 0, 0);
            else getNavigation().startMovingTo(o, 1.1);
        }
    }
}
