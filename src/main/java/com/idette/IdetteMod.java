package com.idette;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public class IdetteMod implements ModInitializer {
    public static final String MOD_ID = "idettemod";

    public static final EntityType<IdetteEntity> IDETTE = FabricEntityTypeBuilder
        .create(SpawnGroup.CREATURE, IdetteEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build();
    public static final EntityType<SujinEntity> SUJIN = FabricEntityTypeBuilder
        .create(SpawnGroup.CREATURE, SujinEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build();
    public static final EntityType<ChanelEntity> CHANEL = FabricEntityTypeBuilder
        .create(SpawnGroup.CREATURE, ChanelEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build();

    public static final EntityType<ChildEntity> CHILD = FabricEntityTypeBuilder
        .create(SpawnGroup.CREATURE, ChildEntity::new).dimensions(EntityDimensions.fixed(0.3f, 0.9f)).build();

    public static final EntityType<CapsuleEntity> CAPSULE = FabricEntityTypeBuilder
        .<CapsuleEntity>create(SpawnGroup.MISC, CapsuleEntity::new)
        .dimensions(EntityDimensions.fixed(1.0f, 1.0f)).trackRangeBlocks(128).trackedUpdateRate(2).build();
    public static final EntityType<CandyEntity> CANDY_BOMB = FabricEntityTypeBuilder
        .<CandyEntity>create(SpawnGroup.MISC, CandyEntity::new)
        .dimensions(EntityDimensions.fixed(0.5f, 0.5f)).trackRangeBlocks(64).trackedUpdateRate(2).build();
    public static final EntityType<CandyEntity> GIANT_CANDY = FabricEntityTypeBuilder
        .<CandyEntity>create(SpawnGroup.MISC, CandyEntity::new)
        .dimensions(EntityDimensions.fixed(3.0f, 3.0f)).trackRangeBlocks(128).trackedUpdateRate(2).build();

    public static Item CAPSULE_SIGNAL, CAPSULE_SIGNAL_SUJIN, CAPSULE_SIGNAL_CHANEL, COIN;

    @Override
    public void onInitialize() {
        reg("idette", IDETTE); reg("child", CHILD); reg("sujin", SUJIN); reg("chanel", CHANEL);
        reg("capsule", CAPSULE); reg("candy_bomb", CANDY_BOMB); reg("giant_candy", GIANT_CANDY);
        FabricDefaultAttributeRegistry.register(IDETTE, IdetteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SUJIN, GirlEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CHANEL, GirlEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CHILD, GirlEntity.createAttributes());

        CAPSULE_SIGNAL = item("capsule_signal", new CapsuleSignalItem(new FabricItemSettings().maxCount(16), 0));
        CAPSULE_SIGNAL_SUJIN = item("capsule_signal_sujin", new CapsuleSignalItem(new FabricItemSettings().maxCount(16), 1));
        CAPSULE_SIGNAL_CHANEL = item("capsule_signal_chanel", new CapsuleSignalItem(new FabricItemSettings().maxCount(16), 2));
        COIN = item("coin", new Item(new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> {
            e.add(CAPSULE_SIGNAL); e.add(CAPSULE_SIGNAL_SUJIN); e.add(CAPSULE_SIGNAL_CHANEL); e.add(COIN);
        });

        // นอนบนเตียง
        EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
            if (!(entity instanceof ServerPlayerEntity sp)) return;
            ServerWorld w = sp.getServerWorld();
            for (GirlEntity g : w.getEntitiesByClass(GirlEntity.class, new Box(pos).expand(10),
                    x -> !(x instanceof ChildEntity) && !x.isVengeful() && (x.isOwnedBy(sp) || x.getOwnerId() == null))) {
                boolean owned = g.isOwnedBy(sp);
                if (!owned && countBeds(w, pos) < 2) {
                    sp.sendMessage(Text.literal("ต้องมีเตียง 2 หลังในบ้าน เธอถึงจะมานอนด้วย"), true);
                    break;
                }
                if (!g.markNight(w.getTimeOfDay() / 24000L)) {
                    sp.sendMessage(Text.literal(g.getName().getString() + " นอนกับคุณไปแล้ววันนี้"), true);
                    break;
                }
                g.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 1.2, 0, 0);
                g.calm();
                g.addBond();
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
                sp.sendMessage(Text.literal(g.getName().getString() + " นอนอยู่ข้างๆ คุณ... (ความสนิท " + g.getBond() + (owned ? ")" : "/3)")), false);
                if (!owned && g.getBond() >= 3) g.claim(sp);
                else if (owned) g.tryConceive(sp);
                break;
            }
        });

        // ตายแล้วไปเกิดในป่าแคปซูล
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> { if (!alive) Forest.sendToForest(newP); });

        CommandRegistrationCallback.EVENT.register((d, reg, env) -> d.register(CommandManager.literal("idette")
            .then(CommandManager.literal("keep").executes(c -> GirlEntity.choose(c.getSource().getPlayerOrThrow(), true)))
            .then(CommandManager.literal("end").executes(c -> GirlEntity.choose(c.getSource().getPlayerOrThrow(), false)))
            .then(CommandManager.literal("home").executes(c -> Forest.goHome(c.getSource().getPlayerOrThrow())))
            .then(CommandManager.literal("forest").executes(c -> { Forest.sendToForest(c.getSource().getPlayerOrThrow()); return 1; }))));
    }

    private static int countBeds(ServerWorld w, BlockPos c) {
        int n = 0;
        for (BlockPos b : BlockPos.iterate(c.add(-8, -3, -8), c.add(8, 3, 8))) {
            BlockState st = w.getBlockState(b);
            if (st.getBlock() instanceof BedBlock && st.get(BedBlock.PART) == BedPart.HEAD) n++;
        }
        return n;
    }

    private static Item item(String id, Item i) { return Registry.register(Registries.ITEM, new Identifier(MOD_ID, id), i); }
    private static void reg(String name, EntityType<?> t) { Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, name), t); }
}
