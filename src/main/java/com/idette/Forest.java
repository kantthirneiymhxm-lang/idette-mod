package com.idette;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.List;

/** ป่าแคปซูล: มิติแยกจากโลกมนุษย์ ตายแล้วมาเกิดที่นี่ */
public class Forest {
    public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, new Identifier(IdetteMod.MOD_ID, "capsule_forest"));
    // ตำแหน่งแคปซูล 3 อัน (ไอเด็ตต์, ซูจิน, ชาแนล) ห่างจุดเกิด
    private static final int[][] PODS = { {24, 8}, {-30, 22}, {8, -34} };

    static class State extends PersistentState {
        boolean done = false;
        @Override public NbtCompound writeNbt(NbtCompound n) { n.putBoolean("done", done); return n; }
        static State read(NbtCompound n) { State s = new State(); s.done = n.getBoolean("done"); return s; }
    }

    public static void sendToForest(ServerPlayerEntity p) {
        ServerWorld fw = p.getServer().getWorld(KEY);
        if (fw == null) { p.sendMessage(Text.literal("ไม่พบมิติป่าแคปซูล"), false); return; }
        fw.getChunk(0, 0);
        State st = fw.getPersistentStateManager().getOrCreate(State::read, State::new, "idettemod_forest");
        if (!st.done) { for (int i = 0; i < 3; i++) buildPod(fw, PODS[i][0], PODS[i][1], i); st.done = true; st.markDirty(); }
        int y = fw.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        p.teleport(fw, 0.5, y + 1, 0.5, 0, 0);
        p.sendMessage(Text.literal("คุณตื่นขึ้นมาในป่าที่มีแคปซูล... ตามหาเธอ แล้วสร้างบ้านที่มีเตียง 2 หลังใกล้ๆ แคปซูล"), false);
    }

    private static void buildPod(ServerWorld w, int x0, int z0, int which) {
        for (int cx = (x0 >> 4) - 1; cx <= ((x0 + 5) >> 4) + 1; cx++)
            for (int cz = (z0 >> 4) - 1; cz <= ((z0 + 3) >> 4) + 1; cz++) { w.getChunk(cx, cz); w.setChunkForced(cx, cz, true); }
        int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x0 + 2, z0 + 1);
        for (int dx = 0; dx < 5; dx++) for (int dz = 0; dz < 3; dz++) for (int dy = 0; dy < 4; dy++) {
            boolean shell = dx == 0 || dx == 4 || dz == 0 || dz == 2 || dy == 0 || dy == 3;
            BlockPos b = new BlockPos(x0 + dx, y + dy - 1, z0 + dz);
            if (!shell) w.setBlockState(b, Blocks.AIR.getDefaultState());
            else if (dz == 0 && dy >= 1 && dy <= 2 && dx >= 1 && dx <= 3) w.setBlockState(b, Blocks.GLASS.getDefaultState()); // กระจกหน้า
            else if (dy == 3 && dx == 2 && dz == 1) w.setBlockState(b, Blocks.SEA_LANTERN.getDefaultState());
            else w.setBlockState(b, Blocks.IRON_BLOCK.getDefaultState());
        }
        Entity e;
        switch (which) {
            case 1 -> e = IdetteMod.SUJIN.create(w);
            case 2 -> e = IdetteMod.CHANEL.create(w);
            default -> { IdetteEntity i = IdetteMod.IDETTE.create(w); if (i != null) i.setHealth(6.0f); e = i; }
        }
        if (e == null) return;
        e.refreshPositionAndAngles(x0 + 2.5, y, z0 + 1.5, 0, 0);
        if (e instanceof PathAwareEntity pe) { pe.setPositionTarget(new BlockPos(x0 + 2, y, z0 + 1), 24); pe.setPersistent(); }
        w.spawnEntity(e);
    }

    /** /idette home — กลับโลกมนุษย์ พร้อมแฟนที่อยู่ใกล้ๆ */
    public static int goHome(ServerPlayerEntity p) {
        ServerWorld dest = p.getServer().getOverworld();
        if (p.getServerWorld() == dest) { p.sendMessage(Text.literal("คุณอยู่โลกมนุษย์แล้ว"), false); return 0; }
        List<GirlEntity> girls = p.getServerWorld().getEntitiesByClass(GirlEntity.class,
            p.getBoundingBox().expand(32), g -> g.isOwnedBy(p) && !g.isVengeful());
        BlockPos sp = p.getSpawnPointPosition();
        if (sp == null || p.getSpawnPointDimension() != World.OVERWORLD) sp = dest.getSpawnPos();
        dest.getChunk(sp);
        double x = sp.getX() + 0.5, z = sp.getZ() + 0.5;
        double y = Math.max(sp.getY(), dest.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ()));
        p.teleport(dest, x, y, z, 0, 0);
        for (GirlEntity g : girls)
            FabricDimensions.teleport(g, dest, new TeleportTarget(new Vec3d(x + 1.5, y, z + 1.5), Vec3d.ZERO, 0, 0));
        p.sendMessage(Text.literal("กลับถึงโลกมนุษย์แล้ว พร้อมคนที่ไปกับคุณ " + girls.size() + " คน"), false);
        return 1;
    }
}
