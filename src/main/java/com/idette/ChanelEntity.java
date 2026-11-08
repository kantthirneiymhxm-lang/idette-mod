package com.idette;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** ชาแนล (ผมชมพู): พ่อค้า — เอาแร่มาแลกของวิเศษ / ให้เหรียญเยอะๆ จะเป็นเมีย */
public class ChanelEntity extends GirlEntity {
    public static final int WIFE_COST = 300;      // จำนวนเหรียญรวมที่ต้องให้
    private int money = 0;

    record Offer(Item cost, int n, Item give, int gn) {}
    static final Offer[] OFFERS = {
        new Offer(Items.IRON_INGOT, 16, Items.GOLDEN_APPLE, 2),
        new Offer(Items.GOLD_INGOT, 16, Items.ENDER_PEARL, 8),
        new Offer(Items.DIAMOND, 5, Items.TOTEM_OF_UNDYING, 1),
        new Offer(Items.EMERALD, 16, Items.ENCHANTED_GOLDEN_APPLE, 1),
        new Offer(Items.DIAMOND, 16, Items.ELYTRA, 1),
        new Offer(Items.NETHERITE_INGOT, 2, Items.NETHER_STAR, 1)
    };

    public ChanelEntity(EntityType<? extends PathAwareEntity> t, World w) { super(t, w); }

    private int price(Offer o) { return owner != null ? Math.max(1, (o.n() + 1) / 2) : o.n(); } // เมียลดครึ่งราคา

    @Override
    public ActionResult interactMob(PlayerEntity p, Hand hand) {
        if (getWorld().isClient) return ActionResult.SUCCESS;
        ItemStack s = p.getStackInHand(hand);
        ServerWorld sw = (ServerWorld) getWorld();

        // ให้เงิน (เหรียญ)
        if (s.isOf(IdetteMod.COIN)) {
            int c = s.getCount();
            if (!p.getAbilities().creativeMode) s.decrement(c);
            money += c;
            sw.spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 6, .3, .3, .3, 0);
            if (money >= WIFE_COST && owner == null) {
                owner = p.getUuid();
                p.sendMessage(Text.literal("ชาแนลประทับใจมาก! ตอนนี้เธอเป็นเมียคุณ (แลกของลดครึ่งราคา และตามคุณไปทุกที่)"), false);
            } else {
                p.sendMessage(Text.literal("ชาแนลรับเหรียญ " + c + " เหรียญ (รวม " + money + (owner == null ? "/" + WIFE_COST : "") + ")"), true);
            }
            return ActionResult.SUCCESS;
        }

        // ดูรายการแลก: Shift + คลิกขวามือเปล่า
        if (s.isEmpty()) {
            if (p.isSneaking()) {
                p.sendMessage(Text.literal("— ของวิเศษที่ชาแนลแลกให้ —"), false);
                for (Offer o : OFFERS)
                    p.sendMessage(Text.literal(o.cost().getName().getString() + " x" + price(o) + "  →  "
                        + o.give().getName().getString() + " x" + o.gn()), false);
                p.sendMessage(Text.literal("ถือแร่ที่ต้องการแลกแล้วคลิกขวาที่เธอ | ให้ 'เหรียญ' เยอะๆ เธอจะเป็นเมีย"), false);
            } else {
                p.sendMessage(Text.literal("ชาแนล: Shift+คลิกขวามือเปล่า เพื่อดูของที่แลกได้ (เหรียญที่ได้รับ " + money + ")"), true);
            }
            return ActionResult.SUCCESS;
        }

        // แลกของ: เลือกข้อเสนอที่ตรงกับของในมือ (เอาอันที่แพงสุดที่จ่ายไหว)
        Offer best = null, cheapest = null;
        for (Offer o : OFFERS) {
            if (!s.isOf(o.cost())) continue;
            if (cheapest == null || price(o) < price(cheapest)) cheapest = o;
            if (s.getCount() >= price(o) && (best == null || price(o) > price(best))) best = o;
        }
        if (best == null) {
            if (cheapest != null) p.sendMessage(Text.literal("ต้องการ " + s.getItem().getName().getString() + " x" + price(cheapest) + " (ตอนนี้มี " + s.getCount() + ")"), true);
            else p.sendMessage(Text.literal("ชาแนลไม่สนใจของชิ้นนี้ (Shift+คลิกขวามือเปล่าเพื่อดูรายการ)"), true);
            return ActionResult.SUCCESS;
        }
        if (!p.getAbilities().creativeMode) s.decrement(price(best));
        p.getInventory().offerOrDrop(new ItemStack(best.give(), best.gn()));
        sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.6, getZ(), 8, .4, .4, .4, 0);
        p.sendMessage(Text.literal("แลกสำเร็จ: " + best.give().getName().getString() + " x" + best.gn()), true);
        return ActionResult.SUCCESS;
    }

    @Override public void writeCustomDataToNbt(NbtCompound n) { super.writeCustomDataToNbt(n); n.putInt("Money", money); }
    @Override public void readCustomDataFromNbt(NbtCompound n) { super.readCustomDataFromNbt(n); money = n.getInt("Money"); }
}
