package resonantinduction.mechanical.mech.process.mixer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.init.Blocks;
import resonant.content.factory.resources.RecipeType;
import resonant.engine.ResonantEngine;
import resonantinduction.mechanical.mech.TileMechanical;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidBlock;
import resonant.api.recipe.MachineRecipes;
import resonantinduction.core.Reference;
import resonantinduction.core.Timer;
import resonant.content.factory.resources.block.BlockFluidMixture;
import universalelectricity.api.core.grid.INode;
import universalelectricity.core.transform.rotation.Quaternion;
import universalelectricity.core.transform.vector.Vector3;

/**
 * @author Calclavia
 */
public class TileMixer extends TileMechanical
{
	public static final int PROCESS_TIME = 12 * 20;
	public static final Timer<EntityItem> timer = new Timer<EntityItem>();

	private boolean areaBlockedFromMoving = false;

	public TileMixer()
	{
		super(Material.iron);
		mechanicalNode = new MixerNode(this);
		isOpaqueCube(false);
		normalRender(false);
		customItemRender(true);
		setTextureName("material_metal_top");
	}

    @Override
    public void getNodes(List<INode> nodes)
    {
        if(mechanicalNode != null)
            nodes.add(this.mechanicalNode);
    }

	@Override
	public void update()
	{
        super.update();
		if (!world().isRemote && ticks() % 20 == 0)
		{
		    this.areaBlockedFromMoving = false;
			for (int x = -1; x <= 1; x++)
			{
				for (int z = -1; z <= 1; z++)
				{
					if (x != 0 && z != 0)
					{
						Block block = position().add(x, 0, z).getBlock(world());

						if (block != null && !(block instanceof IFluidBlock))
						{
							this.areaBlockedFromMoving = true;
							return;
						}
					}
				}
			}
		}

		if (canWork())
		{
			doWork();
		}
	}

	/**
	 * Can this machine work this tick?
	 *
	 * @return
	 */
	public boolean canWork()
	{
		return mechanicalNode.getAngularSpeed(ForgeDirection.UNKNOWN) != 0 && !areaBlockedFromMoving;
	}

	public void doWork()
	{
		boolean didWork = false;

		// Search for an item to "process"
		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(this.xCoord - 1, this.yCoord, this.zCoord - 1, this.xCoord + 2, this.yCoord + 1, this.zCoord + 2);
		List<Entity> entities = this.worldObj.getEntitiesWithinAABB(Entity.class, aabb);
		Set<EntityItem> processItems = new LinkedHashSet<EntityItem>();

		for (Entity entity : entities)
		{
			/**
			 * Rotate entities around the mixer
			 */
			Vector3 originalPosition = new Vector3(entity);
			Vector3 relativePosition = originalPosition.clone().subtract(new Vector3(this).add(0.5));
			relativePosition.transform(new Quaternion(- mechanicalNode.getAngularSpeed(ForgeDirection.UNKNOWN), new Vector3(1,0,0)));
			Vector3 newPosition = new Vector3(this).add(0.5).add(relativePosition);
			Vector3 difference = newPosition.subtract(originalPosition).multiply(0.5);

			entity.addVelocity(difference.x(), difference.y(), difference.z());
			entity.onGround = false;

			if (entity instanceof EntityItem)
			{
				if (MachineRecipes.INSTANCE.getOutput(RecipeType.MIXER.name(), ((EntityItem) entity).getEntityItem()).length > 0)
				{
					processItems.add((EntityItem) entity);
				}
			}
		}

		for (EntityItem processingItem : processItems)
		{
			if (!timer.containsKey(processingItem))
			{
				timer.put(processingItem, PROCESS_TIME);
			}

			if (!processingItem.isDead && new Vector3(this).add(0.5).distance(new Vector3(processingItem)) < 2)
			{
				int timeLeft = timer.decrease(processingItem);

				if (timeLeft <= 0)
				{
					if (doneWork(processingItem))
					{
						if (--processingItem.getEntityItem().stackSize <= 0)
						{
							processingItem.setDead();
							timer.remove(processingItem);
						}
						else
						{
							processingItem.setEntityItemStack(processingItem.getEntityItem());
							// Reset timer
							timer.put(processingItem, PROCESS_TIME);
						}
					}
				}
				else
				{
					processingItem.delayBeforeCanPickup = 20;
					this.worldObj.spawnParticle("bubble", processingItem.posX, processingItem.posY, processingItem.posZ, (Math.random() - 0.5f) * 3, (Math.random() - 0.5f) * 3, (Math.random() - 0.5f) * 3);
				}

				didWork = true;
			}
			else
			{
				timer.remove(processingItem);
			}
		}

		if (didWork)
		{
			if (this.ticks() % 20 == 0)
			{
				this.worldObj.playSoundEffect(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5, Reference.prefix() + "mixer", 0.5f, 1);
			}
		}
	}

	private boolean doneWork(EntityItem entity)
	{
		Vector3 mixPosition = new Vector3(entity.posX, yCoord, entity.posZ);

		if (mixPosition.getBlock(world()) != getBlockType())
		{
			Block block = mixPosition.getBlock(worldObj);
			Block blockFluidFinite = ResonantEngine.resourceFactory.getMixture(ResonantEngine.resourceFactory.getName(entity.getEntityItem()));

			if (blockFluidFinite != null)
			{
				if (block instanceof BlockFluidMixture)
				{
					ItemStack itemStack = entity.getEntityItem().copy();

					if (((BlockFluidMixture) block).mix(worldObj, mixPosition.xi(), mixPosition.yi(), mixPosition.zi(), itemStack))
					{
						worldObj.notifyBlocksOfNeighborChange(mixPosition.xi(), mixPosition.yi(), mixPosition.zi(), mixPosition.getBlock(worldObj));
						return true;
					}
				}
				else if (block != null && (block == Blocks.water || block == Blocks.flowing_water))
				{
					mixPosition.setBlock(worldObj, blockFluidFinite);
				}
			}
		}

		return false;
	}
}