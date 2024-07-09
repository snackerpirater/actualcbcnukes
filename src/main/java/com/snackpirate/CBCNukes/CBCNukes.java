package com.snackpirate.CBCNukes;

import com.github.alexmodguy.alexscaves.server.block.ACSoundTypes;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.CBCTags;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.ModGroup;
import rbasamoyai.createbigcannons.datagen.assets.CBCBuilderTransformers;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesSerializers;
import rbasamoyai.createbigcannons.multiloader.EntityTypeConfigurator;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileRenderer;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesSerializer;

import java.util.function.Consumer;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CBCNukes.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CBCNukes
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "cbc_nukes";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    static {
        REGISTRATE.setTooltipModifierFactory(item -> {
            return new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
        });
    }
    //put everthing in one class because the mod is one block lmao
    public static final BlockEntry<NukeShellBlock> NUKE_SHELL = REGISTRATE
            .block("nuke_shell", NukeShellBlock::new)
            .transform(shell(MapColor.COLOR_GRAY))
            .transform(axeOrPickaxe())
            .transform(projectile("projectile/nuke_shell", false))
            .transform(CBCBuilderTransformers.safeNbt())
            .properties(p -> p.sound(ACSoundTypes.NUCLEAR_BOMB))
            .loot(CBCBuilderTransformers.shellLoot())
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.createnukelangtest.nuke_shell"))
            .lang("Nuclear Shell")
            .item()
            .tag(CBCTags.CBCItemTags.BIG_CANNON_PROJECTILES)
            .build()
            .register();

    public static final EntityEntry<NukeShellProjectile> NUKE_SHELL_PROJECTILE = cannonProjectile("nuke_shell_projectile", NukeShellProjectile::new, "Nuclear Shell", CBCMunitionPropertiesSerializers.COMMON_SHELL_BIG_CANNON_PROJECTILE);



    public CBCNukes()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so tabs get registered

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::dataGen);
        REGISTRATE.registerEventListeners(modEventBus);

    }



    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == ModGroup.MAIN_TAB_KEY)
            event.accept(NUKE_SHELL.asItem());
    }
    @SubscribeEvent
	void dataGen(GatherDataEvent event) {
        if (event.includeClient()) {
            REGISTRATE.addRawLang("block.createnukelangtest.nuke_shell.tooltip.summary", "\"Now, I am become _Death_, the destroyer of worlds.\"");
            REGISTRATE.addRawLang("block.createnukelangtest.nuke_shell.tooltip.condition1", "On Detonation");
            REGISTRATE.addRawLang("block.createnukelangtest.nuke_shell.tooltip.behaviour1", "Unleashes a powerful nuclear blast, devastating the immediate area in an inferno and irradiating the surrounding environment.");
        }
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    private static <T extends Block, P> NonNullUnaryOperator<BlockBuilder<T, P>> shell(MapColor color) {
        return b -> b.addLayer(() -> RenderType::solid)
                .properties(p -> p.mapColor(color))
                .properties(p -> p.strength(2.0f, 3.0f))
                .properties(p -> p.sound(SoundType.STONE));
    }

    private static <P extends BigCannonProjectileProperties, T extends AbstractBigCannonProjectile<P>> EntityEntry<T>
    cannonProjectile(String id, EntityType.EntityFactory<T> factory, String enUSdiffLang, MunitionPropertiesSerializer<P> ser) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(cannonProperties())
                .renderer(() -> BigCannonProjectileRenderer::new)
                .lang(enUSdiffLang)
                .onRegister(type -> MunitionPropertiesHandler.registerPropertiesSerializer(type, ser))
                .register();
    }
    private static <T> NonNullConsumer<T> cannonProperties() {
        return configure(c -> c.size(0.8f, 0.8f)
                .fireImmune()
                .updateInterval(1)
                .updateVelocity(false) // Ditto
                .trackingRange(16));
    }

    private static <T> NonNullConsumer<T> configure(Consumer<EntityTypeConfigurator> cons) {
        return b -> cons.accept(EntityTypeConfigurator.of(b));
    }
    public static <T extends Block, P> NonNullUnaryOperator<BlockBuilder<T, P>> projectile(String pathAndMaterial, boolean useStandardModel) {
        ResourceLocation baseLoc = CreateBigCannons.resource(String.format("block/%sprojectile_block", useStandardModel ? "standard_" : ""));
        ResourceLocation sideLoc = resource("block/" + pathAndMaterial);
        ResourceLocation topLoc = resource("block/" + pathAndMaterial + "_top");
        ResourceLocation bottomLoc = resource("block/" + pathAndMaterial + "_bottom");
        return (b) -> {
            return b.properties((p) -> {
                return p.noOcclusion();
            }).addLayer(() -> {
                return RenderType::solid;
            }).blockstate((c, p) -> {
                BlockModelBuilder builder = (BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models().withExistingParent(c.getName(), baseLoc)).texture("side", sideLoc)).texture("top", topLoc)).texture("particle", topLoc);
                if (!useStandardModel) {
                    builder.texture("bottom", bottomLoc);
                }

                p.directionalBlock((Block)c.get(), builder);
            });
        };
    }
    public static ResourceLocation resource(String path) {
        return new ResourceLocation(MODID, path);
    }
}
