package uk.gaz492.framland.tileentities;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import uk.gaz492.framland.ConfigHandler;

import javax.annotation.Nullable;

public class TileDrit extends TileEntity implements ITickable {

    private int tickCount = 0;

    public EnergyStorage energy = new EnergyStorage(ConfigHandler.dritConfig.maxRF, ConfigHandler.dritConfig.maxRF, ConfigHandler.dritConfig.maxRF);

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("energy", energy.getEnergyStored());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        nbt.getInteger("energy");
        energy = new EnergyStorage(ConfigHandler.dritConfig.maxRF, ConfigHandler.dritConfig.maxRF, ConfigHandler.dritConfig.maxRF, nbt.getInteger("energy"));
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = super.writeToNBT(new NBTTagCompound());
        writeToNBT(compound);
        return compound;
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ? (T) energy : super.getCapability(capability, facing);
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            Block blockUp = world.getBlockState(pos.up()).getBlock();
            if (blockUp instanceof IGrowable) {
                int growthTick = world.rand.nextInt((ConfigHandler.dritConfig.maxGrowthTicks - ConfigHandler.dritConfig.minGrowthTicks) + 1) + ConfigHandler.dritConfig.minGrowthTicks;
                if (tickCount >= growthTick) {
                    if (energy.getEnergyStored() >= ConfigHandler.dritConfig.rfToGrow) {
                        IBlockState blockPlant = world.getBlockState(pos.up(1));
                        IGrowable iGrowable = (IGrowable) blockPlant.getBlock();
                        if (iGrowable.canGrow(world, pos.up(1), blockPlant, false)) {
                            tickCount = 0;
                            energy.extractEnergy(ConfigHandler.dritConfig.rfToGrow, false);
                            world.playEvent(2005, pos.up(1), 0);
                            iGrowable.grow(world, world.rand, pos.up(), blockPlant);
                            world.markBlockRangeForRenderUpdate(pos, pos);
                        }
                    }
                }
                tickCount++;
            }
        }
    }
}
