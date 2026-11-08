package com.idette;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** ลูกของเรา: ตามเราไปเฉยๆ (ไม่ต่อสู้) */
public class ChildEntity extends GirlEntity {
    public ChildEntity(EntityType<? extends PathAwareEntity> t, World w) { super(t, w); }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new FollowOwnerGoal());
        goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        goalSelector.add(4, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity p, Hand hand) {
        if (getWorld().isClient) return ActionResult.SUCCESS;
        ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 1, getZ(), 5, .3, .3, .3, 0);
        p.sendMessage(Text.literal("ลูกยิ้มให้คุณ"), true);
        return ActionResult.SUCCESS;
    }
}
