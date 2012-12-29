package assemblyline.common.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.TranslationHelper;
import universalelectricity.prefab.network.PacketManager;
import assemblyline.common.machine.filter.ItemFilter;
import assemblyline.common.machine.filter.TileEntityFilterable;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;

/**
 * 
 * @author Darkguardsman
 * 
 */
public class TileEntityRejector extends TileEntityFilterable
{
	/**
	 * should the piston fire, or be extended
	 */
	public boolean firePiston = false;
	/**
	 * on/off value for the GUI buttons
	 */
	public boolean[] guiButtons = new boolean[] { true, true, true, true, true };

	private int playerUsing = 0;

	@Override
	public void onUpdate()
	{
		/**
		 * Has to update a bit faster than a conveyer belt
		 */
		if (this.ticks % 5 == 0 && !this.isDisabled())
		{
			int metadata = this.getBlockMetadata();
			this.firePiston = false;

			// area to search for items
			Vector3 searchPosition = new Vector3(this);
			searchPosition.modifyPositionFromSide(this.getDirection());
			TileEntity tileEntity = searchPosition.getTileEntity(this.worldObj);

			try
			{
				boolean flag = false;

				if (this.isRunning())
				{
					/**
					 * Find all entities in the position in which this block is facing and attempt
					 * to push it out of the way.
					 */
					AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(searchPosition.x, searchPosition.y, searchPosition.z, searchPosition.x + 1, searchPosition.y + 1, searchPosition.z + 1);
					List<Entity> entitiesInFront = this.worldObj.getEntitiesWithinAABB(Entity.class, bounds);

					for (Entity entity : entitiesInFront)
					{
						if (this.canEntityBeThrow(entity))
						{
							this.throwItem(this.getDirection(), entity);
							flag = true;
						}
					}
				}

				/**
				 * If a push happened, send a packet to the client to notify it for an animation.
				 */
				if (!this.worldObj.isRemote && flag)
				{
					// Packet packet = PacketManager.getPacket(AssemblyLine.CHANNEL, this,
					// this.getPacketData(PacketTypes.ANIMATION));
					// PacketManager.sendPacketToClients(packet, this.worldObj, new Vector3(this),
					// 30);
					PacketManager.sendPacketToClients(getDescriptionPacket());
				}

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && this.playerUsing > 0)
			{
				PacketManager.sendPacketToClients(this.getDescriptionPacket(), this.worldObj, new Vector3(this), 10);
			}
		}
	}

	/**
	 * Used to move after it has been rejected
	 * 
	 * @param side - used to do the offset
	 * @param entity - Entity being thrown
	 */
	public void throwItem(ForgeDirection side, Entity entity)
	{
		this.firePiston = true;

		entity.motionX = (double) side.offsetX * 0.1;
		entity.motionY += 0.10000000298023224D;
		entity.motionZ = (double) side.offsetZ * 0.1;

		PacketManager.sendPacketToClients(getDescriptionPacket());
	}

	public boolean canEntityBeThrow(Entity entity)
	{
		// TODO Add other things than items
		if (entity instanceof EntityItem)
		{
			EntityItem entityItem = (EntityItem) entity;
			ItemStack itemStack = entityItem.func_92014_d();

			if (getFilter() != null)
			{
				ArrayList<ItemStack> checkStacks = ItemFilter.getFilters(getFilter());

				// Reject matching items
				for (int i = 0; i < checkStacks.size(); i++)
				{
					if (checkStacks.get(i) != null)
					{
						if (checkStacks.get(i).isItemEqual(itemStack)) { return true; }
					}
				}
			}
		}

		return false;
	}

	/**
	 * Inventory Methods
	 */
	@Override
	public String getInvName()
	{
		return TranslationHelper.getLocal("tile.rejector.name");
	}

	/**
	 * UE methods
	 */
	@Override
	public double getVoltage()
	{
		return 120;
	}

	/**
	 * NBT Data
	 */
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);

		firePiston = nbt.getBoolean("piston");
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);

		nbt.setBoolean("piston", firePiston);
	}
}
