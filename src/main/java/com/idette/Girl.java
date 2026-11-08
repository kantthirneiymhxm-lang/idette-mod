package com.idette;

import net.minecraft.entity.player.PlayerEntity;

/** ตัวละครสาวๆ ทุกคนใช้ interface นี้ (ให้ระบบนอนบนเตียงทำงานร่วมกัน) */
public interface Girl {
    boolean isOwnedBy(PlayerEntity p);
    void addBond();
    int getBond();
    default void calm() {}
}
