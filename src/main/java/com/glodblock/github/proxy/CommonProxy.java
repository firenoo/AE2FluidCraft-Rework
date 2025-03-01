package com.glodblock.github.proxy;

import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.IAppEngApi;
import appeng.api.config.Upgrades;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.Config;
import com.glodblock.github.common.tile.TileWalrus;
import com.glodblock.github.inventory.external.AEFluidInterfaceHandler;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.glodblock.github.util.ModAndClassUtil;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class CommonProxy {

    public final SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel(FluidCraft.MODID);

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {
        this.registerMovables();
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (ModAndClassUtil.isBigInterface) {
            Upgrades.PATTERN_CAPACITY.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_INTERFACE), 3);
            Upgrades.PATTERN_CAPACITY.registerItem(new ItemStack(ItemAndBlockHolder.INTERFACE), 3);
        }
        Upgrades.CRAFTING.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_INTERFACE), 1);
        Upgrades.CRAFTING.registerItem(new ItemStack(ItemAndBlockHolder.INTERFACE), 1);
        Upgrades.CAPACITY.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_STORAGE_BUS), 5);
        Upgrades.INVERTER.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_STORAGE_BUS), 1);

        if (Config.fluidIOBus) {
            Upgrades.CAPACITY.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_EXPORT_BUS), 2);
            Upgrades.CAPACITY.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_IMPORT_BUS), 2);
            Upgrades.REDSTONE.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_EXPORT_BUS), 1);
            Upgrades.REDSTONE.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_IMPORT_BUS), 1);
            Upgrades.SPEED.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_EXPORT_BUS), 4);
            Upgrades.SPEED.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_IMPORT_BUS), 4);
            Upgrades.SUPERSPEED.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_EXPORT_BUS), 4);
            Upgrades.SUPERSPEED.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_IMPORT_BUS), 4);
            Upgrades.CRAFTING.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_EXPORT_BUS), 1);
        }
        AEApi.instance().registries().externalStorage().addExternalStorageInterface(new AEFluidInterfaceHandler());
    }

    public void registerRenderers() {}

    public void registerMovables() {
        IAppEngApi api = AEApi.instance();
        api.registries().movable().whiteListTileEntity(TileWalrus.class);
    }
}
