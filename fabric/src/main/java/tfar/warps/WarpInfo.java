package tfar.warps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record WarpInfo(ResourceKey<Level> level, Vec3 position, float yaw, float pitch, ItemStack icon) {

    public static final Codec<WarpInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(WarpInfo::level),
                    Vec3.CODEC.fieldOf("position").forGetter(WarpInfo::position),
            Codec.FLOAT.fieldOf("yaw").forGetter(WarpInfo::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(WarpInfo::pitch),
            ItemStack.CODEC.fieldOf("icon").forGetter(WarpInfo::icon)
            ).apply(instance,WarpInfo::new)
    );

    public void teleport(ServerPlayer player) {
        MinecraftServer server = player.server;
        player.teleportTo(server.getLevel(level),position.x,position.y,position.z,yaw,pitch);
    }
}
