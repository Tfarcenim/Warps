package tfar.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WarpsFabric implements ModInitializer {

    public static final AttachmentType<List<WarpInfo>> WARPS = AttachmentRegistry.<List<WarpInfo>>builder()
            .persistent(WarpInfo.CODEC.listOf())
            .initializer(ArrayList::new)
            //.copyOnDeath()
            .buildAndRegister(new ResourceLocation("warps:warps"));

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Warps.init();
    }

    void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection var3) {
        dispatcher.register(Commands.literal("warps")
                .executes(WarpsFabric::openGui)
                .then(Commands.literal("create").requires(sourceStack -> sourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context1 -> createWarpWithName(context1, null))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context1 -> createWarpWithName(context1, StringArgumentType.getString(context1, "name"))
                                )
                        )
                )
                .then(Commands.literal("remove").requires(sourceStack -> sourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(WarpsFabric::removeWarp)
                )
        );
    }

    static int createWarpWithName(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty()) {
            List<WarpInfo> warpInfos = new ArrayList<>(player.server.overworld().getAttachedOrCreate(WARPS));

            if (warpInfos.size() >= 9) {
                context.getSource().sendFailure(Component.literal("Too many warps"));
                return 0;
            }
            ItemStack copy = stack.copy();
            copy.setCount(1);
            if (name != null) {
                copy.setHoverName(Component.literal(name));
            }

            Vec3 position = player.position();
            ResourceKey<Level> level = player.level().dimension();

            setLore(copy,List.of(Component.literal("Dimension: "+level.location()),
                    Component.literal("Position: "+Math.round(position.x)+" "+Math.round(position.y)+" "+Math.round(position.z))));

            WarpInfo warpInfo = new WarpInfo(player.level().dimension(),position, player.getYRot(), player.getXRot(), copy);
            warpInfos.add(warpInfo);

            player.server.overworld().setAttached(WARPS, warpInfos);
            context.getSource().sendSuccess(() -> Component.literal("Created warp: " + name), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Must be holding an item in mainhand"));
            return 0;
        }
    }

    static void setLore(ItemStack stack,List<Component> lore) {
        ListTag listTag = new ListTag();//LIST_TAG = 9
        for (Component component : lore) {
            listTag.add(StringTag.valueOf(Component.Serializer.toJson(component))); //STRING_TAG = 8
        }
        CompoundTag displayTag = stack.getOrCreateTagElement(ItemStack.TAG_DISPLAY);
        displayTag.put(ItemStack.TAG_LORE,listTag);
    }


    static int removeWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        List<WarpInfo> warpInfos = new ArrayList<>(player.server.overworld().getAttachedOrCreate(WARPS));

        if (warpInfos.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No warps to remove"));
            return 0;
        }

        warpInfos.remove(warpInfos.size() - 1);

        player.server.overworld().setAttached(WARPS, warpInfos);
        context.getSource().sendSuccess(() -> Component.literal("Removed last warp"), false);
        return 1;
    }

    static int openGui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SimpleGui simpleGui = new SimpleGui(MenuType.GENERIC_9x1, player, false);

        simpleGui.setTitle(Component.literal("Warps"));

        List<WarpInfo> warpInfos = player.server.overworld().getAttachedOrCreate(WARPS);

        for (int i = 0; i < warpInfos.size(); i++) {
            WarpInfo warpInfo = warpInfos.get(i);
            simpleGui.setSlot(i, GuiElementBuilder.from(warpInfo.icon())
                    .setCallback((i1, clickType, clickType1) -> {
                        warpInfo.teleport(player);
                    })
            );
        }
        simpleGui.open();
        return 1;
    }
}
