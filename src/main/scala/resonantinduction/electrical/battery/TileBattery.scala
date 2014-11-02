package resonantinduction.electrical.battery

import java.util.ArrayList

import io.netty.buffer.ByteBuf
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import resonant.api.electric.EnergyStorage
import resonant.content.prefab.java.TileAdvanced
import resonant.lib.content.prefab.{TElectric, TEnergyStorage}
import resonant.lib.network.discriminator.{PacketTile, PacketType}
import resonant.lib.network.handle.IPacketReceiver
import resonant.lib.network.netty.AbstractPacket
import resonant.lib.transform.vector.Vector3

/** A modular battery box that allows shared connections with boxes next to it.
  *
  * @author Calclavia
  */
object TileBattery
{
  /**
   * @param tier - 0, 1, 2
   * @return
   */
  def getEnergyForTier(tier: Int): Long =
  {
    return Math.round(Math.pow(500000000, (tier / (maxTier + 0.7f)) + 1) / (500000000)) * (500000000)
  }

  /** Tiers: 0, 1, 2 */
  final val maxTier: Int = 2
  /** The transfer rate **/
  final val defaultPower: Long = getEnergyForTier(0)
}

class TileBattery extends TileAdvanced(Material.iron) with TElectric with IPacketReceiver with TEnergyStorage
{
  private var markClientUpdate: Boolean = false
  private var markDistributionUpdate: Boolean = false
  var renderEnergyAmount: Double = 0

  energy = new EnergyStorage
  textureName = "material_metal_side"
  ioMap = 0
  saveIOMap = true
  normalRender = false
  isOpaqueCube = false
  itemBlock = classOf[ItemBlockBattery]

  var doCharge = false

  override def update()
  {
    super.update()

    if (!world.isRemote)
    {
      //TODO: Test, remove this
      if (doCharge)
      {
        dcNode.buffer(100)
        doCharge = false
      }

      if (markDistributionUpdate && ticks % 5 == 0)
      {
        markDistributionUpdate = false
      }
      if (markClientUpdate && ticks % 5 == 0)
      {
        markClientUpdate = false
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord)
      }
    }
  }

  override def activate(player: EntityPlayer, side: Int, hit: Vector3): Boolean =
  {
    super.activate(player, side, hit)

    if (!world.isRemote)
    {
      if (player.isSneaking)
      {
        doCharge = !doCharge
      }

      println(dcNode)
    }

    return true
  }

  override def toggleIO(side: Int, entityPlayer: EntityPlayer): Boolean =
  {
    val res = super.toggleIO(side, entityPlayer)
    dcNode.positiveTerminals.clear()
    dcNode.positiveTerminals.addAll(getOutputDirections())
    return res
  }

  override def getDescPacket: AbstractPacket =
  {
    return new PacketTile(this, Array[Any](renderEnergyAmount, ioMap))
  }

  def read(data: ByteBuf, player: EntityPlayer, `type`: PacketType)
  {
    this.energy.setEnergy(data.readLong)
    this.ioMap_$eq(data.readShort)
  }

  override def setIO(dir: ForgeDirection, `type`: Int)
  {
    super.setIO(dir, `type`)
    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord)
  }

  override def onPlaced(entityliving: EntityLivingBase, itemStack: ItemStack)
  {
    if (!world.isRemote && itemStack.getItem.isInstanceOf[ItemBlockBattery])
    {
      energy.setCapacity(TileBattery.getEnergyForTier(ItemBlockBattery.getTier(itemStack)))
      energy.setEnergy((itemStack.getItem.asInstanceOf[ItemBlockBattery]).getEnergy(itemStack))
      world.setBlockMetadataWithNotify(xi, yi, zi, ItemBlockBattery.getTier(itemStack), 3)
    }
  }

  override def getDrops(metadata: Int, fortune: Int): ArrayList[ItemStack] =
  {
    val ret: ArrayList[ItemStack] = new ArrayList[ItemStack]
    val itemStack: ItemStack = new ItemStack(getBlockType, 1)
    val itemBlock: ItemBlockBattery = itemStack.getItem.asInstanceOf[ItemBlockBattery]
    ItemBlockBattery.setTier(itemStack, world.getBlockMetadata(xi, yi, zi).asInstanceOf[Byte])
    itemBlock.setEnergy(itemStack, energy.getEnergy)
    ret.add(itemStack)
    return ret
  }

  override def toString: String =
  {
    return "[TileBattery]" + x + "x " + y + "y " + z + "z "
  }
}