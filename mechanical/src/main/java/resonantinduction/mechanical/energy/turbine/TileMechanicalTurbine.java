package resonantinduction.mechanical.energy.turbine;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import resonantinduction.mechanical.energy.network.IMechanicalNodeProvider;
import resonantinduction.mechanical.energy.network.MechanicalNode;
import universalelectricity.api.energy.EnergyStorageHandler;
import calclavia.lib.network.Synced.SyncedInput;
import calclavia.lib.network.Synced.SyncedOutput;
import calclavia.lib.prefab.turbine.TileTurbine;

public class TileMechanicalTurbine extends TileTurbine implements IMechanicalNodeProvider
{
	protected MechanicalNode node;

	public TileMechanicalTurbine()
	{
		super();
		energy = new EnergyStorageHandler(0);
		node = new MechanicalNode(this)
		{
			@Override
			public boolean canConnect(ForgeDirection from, Object source)
			{
				if (source instanceof MechanicalNode && !(source instanceof TileMechanicalTurbine))
				{
					/**
					 * Face to face stick connection.
					 */
					TileEntity sourceTile = position().translate(from).getTileEntity(getWorld());

					if (sourceTile instanceof IMechanicalNodeProvider)
					{
						MechanicalNode sourceInstance = ((IMechanicalNodeProvider) sourceTile).getNode(from.getOpposite());
						return sourceInstance == source && from == getDirection().getOpposite();
					}
				}

				return false;
			}

			@Override
			public boolean inverseRotation(ForgeDirection dir, MechanicalNode with)
			{
				return true;
			}

			@Override
			public float getRatio(ForgeDirection dir, MechanicalNode with)
			{
				return getMultiBlock().isConstructed() ? multiBlockRadius - 0.5f : 0.5f;
			}
		};
	}

	@Override
	public void initiate()
	{
		node.reconstruct();
		super.initiate();
	}

	@Override
	public void invalidate()
	{
		node.deconstruct();
		super.invalidate();
	}

	@Override
	public void onProduce()
	{
		node.torque += (torque - node.torque) / 10;
		node.angularVelocity += (angularVelocity - node.angularVelocity) / 10;
	}

	@Override
	public MechanicalNode getNode(ForgeDirection dir)
	{
		return ((TileMechanicalTurbine) getMultiBlock().get()).node;
	}

	@Override
	@SyncedInput
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		tier = nbt.getInteger("tier");
		node.load(nbt);
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	@SyncedOutput
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setInteger("tier", tier);
		node.save(nbt);
	}
}