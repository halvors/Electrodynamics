package resonantinduction.mechanical.belt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import resonantinduction.core.Reference;
import resonantinduction.core.ResonantInduction;
import resonantinduction.mechanical.Mechanical;
import calclavia.api.resonantinduction.mechanical.IBelt;
import calclavia.lib.content.module.TileBase;
import calclavia.lib.grid.INode;
import calclavia.lib.grid.INodeProvider;
import calclavia.lib.network.IPacketReceiverWithID;
import calclavia.lib.prefab.tile.IRotatable;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.network.PacketDispatcher;

/** Conveyer belt TileEntity that allows entities of all kinds to be moved
 * 
 * @author DarkGuardsman */
public class TileConveyorBelt extends TileBase implements IBelt, IRotatable, INodeProvider, IPacketReceiverWithID
{
    public enum BeltType
    {
        NORMAL,
        SLANT_UP,
        SLANT_DOWN,
        RAISED
    }

    /** Static constants. */
    public static final int MAX_FRAME = 13;
    public static final int MAX_SLANT_FRAME = 23;
    public static final int PACKET_SLANT = 0;
    public static final int PACKET_REFRESH = 1;
    /** Acceleration of entities on the belt */
    public static final float ACCELERATION = 0.01f;

    /** Frame count for texture animation from 0 - maxFrame */
    private int animationFrame = 0;

    private BeltType slantType = BeltType.NORMAL;

    /** Entities that are ignored allowing for other tiles to interact with them */
    public List<Entity> ignoreList = new ArrayList<Entity>();

    private boolean markRefresh = true;

    public BeltNode mechanicalNode;

    public TileConveyorBelt()
    {
        super(Material.iron);
        mechanicalNode = new BeltNode(this);
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();

        /* PROCESSES IGNORE LIST AND REMOVES UNNEED ENTRIES */
        Iterator<Entity> it = this.ignoreList.iterator();

        List<Entity> effect_list = this.getAffectedEntities();
        while (it.hasNext())
        {
            if (!effect_list.contains(it.next()))
            {
                it.remove();
            }
        }

        if (this.worldObj.isRemote)
        {
            if (this.ticks % 10 == 0 && this.worldObj.getBlockId(this.xCoord - 1, this.yCoord, this.zCoord) != Mechanical.blockConveyorBelt.blockID && this.worldObj.getBlockId(xCoord, yCoord, zCoord - 1) != Mechanical.blockConveyorBelt.blockID)
            {
                worldObj.playSound(this.xCoord, this.yCoord, this.zCoord, Reference.PREFIX + "conveyor", 0.5f, 0.5f + 0.15f * (float) getMoveVelocity(), true);
            }

            double beltPercentage = mechanicalNode.angle / (2 * Math.PI);

            // Sync the animation. Slant belts are slower.
            if (this.getBeltType() == BeltType.NORMAL || this.getBeltType() == BeltType.RAISED)
            {
                this.animationFrame = (int) (beltPercentage * MAX_FRAME);
                if (this.animationFrame < 0)
                    this.animationFrame = 0;
                if (this.animationFrame > MAX_FRAME)
                    this.animationFrame = MAX_FRAME;
            }
            else
            {
                this.animationFrame = (int) (beltPercentage * MAX_SLANT_FRAME);
                if (this.animationFrame < 0)
                    this.animationFrame = 0;
                if (this.animationFrame > MAX_SLANT_FRAME)
                    this.animationFrame = MAX_SLANT_FRAME;
            }
        }
        else
        {
            if (markRefresh)
            {
                sendRefreshPacket();
                markRefresh = false;
            }
        }
    }

    @Override
    public <N extends INode> N getNode(Class<? super N> nodeType, ForgeDirection from)
    {
        if (nodeType.isAssignableFrom(mechanicalNode.getClass()))
            return (N) mechanicalNode;
        return null;
    }

    @Override
    public Packet getDescriptionPacket()
    {
        if (this.getBeltType() != BeltType.NORMAL)
        {
            return ResonantInduction.PACKET_TILE.getPacket(this, PACKET_SLANT, this.getBeltType().ordinal());
        }
        return super.getDescriptionPacket();
    }

    public void sendRefreshPacket()
    {
        PacketDispatcher.sendPacketToAllPlayers(ResonantInduction.PACKET_TILE.getPacket(this, PACKET_REFRESH));
    }

    @Override
    public boolean onReceivePacket(int id, ByteArrayDataInput data, EntityPlayer player, Object... extra)
    {
        if (this.worldObj.isRemote)
        {
            if (id == PACKET_SLANT)
            {
                this.setBeltType(BeltType.values()[data.readInt()]);
                return true;
            }
            else if (id == PACKET_REFRESH)
            {
                mechanicalNode.reconstruct();
                return true;
            }
        }
        return false;
    }

    public void setSlant(BeltType slantType)
    {
        if (slantType == null)
        {
            slantType = BeltType.NORMAL;
        }

        this.setBeltType(slantType);
        mechanicalNode.reconstruct();
        this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void setDirection(ForgeDirection facingDirection)
    {
        this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, facingDirection.ordinal(), 3);
    }

    @Override
    public ForgeDirection getDirection()
    {
        return ForgeDirection.getOrientation(this.getBlockMetadata());
    }

    @Override
    public List<Entity> getAffectedEntities()
    {
        return worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 1, this.zCoord + 1));
    }

    /** NBT Data */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.setBeltType(BeltType.values()[nbt.getByte("slant")]);
    }

    /** Writes a tile entity to NBT. */
    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setByte("slant", (byte) this.getBeltType().ordinal());
    }

    @Override
    public void ignoreEntity(Entity entity)
    {
        if (!this.ignoreList.contains(entity))
        {
            this.ignoreList.add(entity);
        }
    }

    public double getMoveVelocity()
    {
        return Math.abs(mechanicalNode.getAngularVelocity());
    }

    public int getAnimationFrame()
    {
        return this.animationFrame;
    }

    public BeltType getBeltType()
    {
        return slantType;
    }

    public void setBeltType(BeltType slantType)
    {
        this.slantType = slantType;
    }
}