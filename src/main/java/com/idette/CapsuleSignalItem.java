package com.idette;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CapsuleSignalItem extends Item {
    private final int which; // 0 = ไอเด็ตต์, 1 = ซูจิน, 2 = ชาแนล
    public CapsuleSignalItem(Settings s, int which) { super(s); this.which = which; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            CapsuleEntity c = new CapsuleEntity(IdetteMod.CAPSULE, user.getX() + 8, user.getY() + 60, user.getZ(), world);
            c.setWhich(which);
            c.setVelocity(0, -1.5, 0);
            world.spawnEntity(c);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        user.getItemCooldownManager().set(this, 40);
        return TypedActionResult.success(stack, world.isClient);
    }
}
