package resonantinduction.mechanical.fluid.transport;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import resonantinduction.core.grid.Node;
import resonantinduction.core.grid.fluid.IPressureNodeProvider;
import resonantinduction.core.grid.fluid.PressureNode;
import resonantinduction.mechanical.energy.grid.TileMechanical;
import universalelectricity.api.vector.Vector3;
import calclavia.lib.prefab.tile.IRotatable;

public class TilePump extends TileMechanical implements IPressureNodeProvider, IRotatable
{
	private final PressureNode pressureNode;

	public TilePump()
	{
		pressureNode = new PressureNode(this)
		{
			@Override
			public int getPressure(ForgeDirection dir)
			{
				if (mechanicalNode.getPower() > 0)
				{
					if (dir == getDirection())
					{
						return (int) Math.max(Math.abs(mechanicalNode.getTorque() / 1000d), 2);
					}
					else if (dir == getDirection().getOpposite())
					{
						return (int) -Math.max(Math.abs(mechanicalNode.getTorque() / 1000d), 2);
					}
				}

				return 0;
			}

			@Override
			public int getMaxFlowRate()
			{
				return (int) Math.abs(mechanicalNode.getAngularVelocity() * 20);
			}

			@Override
			public boolean canConnect(ForgeDirection from, Object source)
			{
				return super.canConnect(from, source) && (from == getDirection() || from == getDirection().getOpposite());
			}

		};
	}

	@Override
	public void initiate()
	{
		pressureNode.reconstruct();
		super.initiate();
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		pressureNode.deconstruct();
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();

		if (!worldObj.isRemote && mechanicalNode.getPower() > 0)
		{
			/**
			 * Try to suck fluid in
			 */
			TileEntity tileIn = new Vector3(this).translate(getDirection().getOpposite()).getTileEntity(this.worldObj);

			if (tileIn instanceof IFluidHandler)
			{
				FluidStack drain = ((IFluidHandler) tileIn).drain(getDirection(), pressureNode.getMaxFlowRate(), false);

				if (drain != null)
				{
					((IFluidHandler) tileIn).drain(getDirection(), fill(getDirection().getOpposite(), drain, true), true);
				}
			}
		}
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if (from == getDirection().getOpposite())
		{
			TileEntity tileOut = new Vector3(this).translate(from.getOpposite()).getTileEntity(this.worldObj);

			if (tileOut instanceof IFluidHandler)
				return ((IFluidHandler) tileOut).fill(from, resource, doFill);
		}

		return 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return from == this.getDirection().getOpposite();
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return from == this.getDirection();
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return null;
	}

	@Override
	public ForgeDirection getDirection()
	{
		return ForgeDirection.getOrientation(this.worldObj.getBlockMetadata(xCoord, yCoord, zCoord));
	}

	@Override
	public void setDirection(ForgeDirection direction)
	{
		this.worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, direction.ordinal(), 3);
	}

	@Override
	public FluidTank getPressureTank()
	{
		return null;
	}

	@Override
	public <N extends Node> N getNode(Class<? super N> nodeType, ForgeDirection from)
	{
		if (nodeType.isAssignableFrom(pressureNode.getClass()))
			return (N) pressureNode;

		return super.getNode(nodeType, from);
	}

	@Override
	public void onFluidChanged()
	{
		
	}
}
