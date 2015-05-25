package com.calclavia.edx.mffs.field

import java.util.{Optional, Set => JSet}

import com.calclavia.edx.mffs.Settings
import com.calclavia.edx.mffs.api.machine.Projector
import com.calclavia.edx.mffs.api.modules.Module.ProjectState
import com.calclavia.edx.mffs.base.{BlockFieldMatrix, PacketBlock}
import com.calclavia.edx.mffs.content.{Content, Models, Textures}
import com.calclavia.edx.mffs.field.shape.ItemShapeCustom
import com.calclavia.edx.mffs.particle.{FXFortronBeam, FXHologramProgress, FieldColor}
import com.calclavia.edx.mffs.security.PermissionHandler
import com.calclavia.edx.mffs.util.CacheHandler
import nova.core.block.Block
import nova.core.block.component.{BlockCollider, LightEmitter, Oriented}
import nova.core.component.renderer.{DynamicRenderer, StaticRenderer}
import nova.core.entity.Entity
import nova.core.game.Game
import nova.core.inventory.InventorySimple
import nova.core.item.Item
import nova.core.network.{Packet, Sync}
import nova.core.player.Player
import nova.core.render.Color
import nova.core.render.model.{Model, Vertex}
import nova.core.retention.Stored
import nova.core.util.transform.matrix.MatrixStack
import nova.core.util.transform.shape.Cuboid
import nova.core.util.transform.vector.{Vector3d, Vector3i}

import scala.collection.convert.wrapAll._

class BlockProjector extends BlockFieldMatrix with Projector with PermissionHandler {

	@Stored
	@Sync(ids = Array(PacketBlock.description, PacketBlock.inventory))
	override val inventory = new InventorySimple(1 + 25 + 6)
	/** A set containing all positions of all force field blocks generated. */
	var forceFields = Set.empty[Vector3i]
	/** Marks the field for an update call */
	var markFieldUpdate = true
	/** True if the field is done constructing and the projector is simply maintaining the field  */
	private var isCompleteConstructing = false
	/** True to make the field constantly tick */
	private var fieldRequireTicks = false

	capacityBase = 30
	startModuleIndex = 1
	/** Are the filters in the projector inverted? */
	@Sync(ids = Array(PacketBlock.description))
	private var isInverted = false

	override def getID: String = "projector"

	add(new BlockCollider(this) {
		override def getBoundingBox: Cuboid = new Cuboid(0, 0, 0, 1, 0.8, 1)

		override def isCube: Boolean = false
	})

	add(new Oriented(this))

	add(new StaticRenderer(this) {
		override def renderStatic(model: Model) {
			model.rotate(get(classOf[Oriented]).get().direction.rotation)
			model.children.add(Models.projector.getModel)
			model.bindAll(if (isActive) Textures.projectorOn else Textures.projectorOff)
		}
	})

	add(new DynamicRenderer(this) {
		override def renderDynamic(model: Model) {
			/**
			 * Render the light beam
			 */
			if (getShapeItem != null) {
				val lightBeam = new Model()
				//TODO: Lighting, RenderHelper.disableStandardItemLighting

				val player = Game.instance.clientManager.getPlayer.asInstanceOf[Entity with Player]
				val xDifference: Double = player.position.x - (x + 0.5)
				val zDifference: Double = player.position.z - (y + 0.5)
				val rot = Math.atan2(zDifference, xDifference)
				lightBeam.matrix = new MatrixStack().rotate(Vector3d.yAxis, -rot + Math.toRadians(27)).getMatrix

				/*
				glDisable(GL_TEXTURE_2D)
				glShadeModel(GL_SMOOTH)
				glEnable(GL_BLEND)
				glBlendFunc(GL_SRC_ALPHA, GL_ONE)
				glDisable(GL_ALPHA_TEST)
				glEnable(GL_CULL_FACE)
				glDepthMask(false)
				glPushMatrix*/

				val height: Float = 2
				val width: Float = 2

				val face = lightBeam.createFace()
				//tessellator.setColorRGBA_I(0, 0)
				face.drawVertex(new Vertex(0, 0, 0, 0, 0))
				face.drawVertex(new Vertex(-0.866D * width, height, -0.5F * width, 0, 0))
				face.drawVertex(new Vertex(0.866D * width, height, -0.5F * width, 0, 0))
				face.drawVertex(new Vertex(0.0D, height, 1.0F * width, 0, 0))
				face.drawVertex(new Vertex(-0.866D * width, height, -0.5F * width, 0, 0))
				face.vertices.foreach(_.setColor(Color.rgb(72, 198, 255)))
				lightBeam.drawFace(face)

				/*
				glPopMatrix
				glDepthMask(true)
				glDisable(GL_CULL_FACE)
				glDisable(GL_BLEND)
				glShadeModel(GL_FLAT)
				glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
				glEnable(GL_TEXTURE_2D)
				glEnable(GL_ALPHA_TEST)
				RenderHelper.enableStandardItemLighting
				glPopMatrix*/

				model.children.add(lightBeam)

				/**
				 * Render hologram
				 */
				if (Settings.highGraphics) {

					val hologram = new Model()
					hologram.matrix = new MatrixStack()
						.translate(0, 0.85 + Math.sin(Math.toRadians(ticks * 3)).toFloat / 7, 0)
						.rotate(Vector3d.yAxis, Math.toRadians(ticks * 4))
						.rotate(new Vector3d(0, 1, 1), Math.toRadians(36f + ticks * 4))
						.getMatrix

					getShapeItem.render(BlockProjector.this, model)

					val color = if (isActive) FieldColor.blue else FieldColor.red
					hologram.faces.foreach(_.vertices.foreach(_.setColor(color.alpha((Math.sin(ticks.toDouble / 10) * 255).toInt))))
					hologram.bind(Textures.hologram)
				}
			}
		}
	})

	add(new LightEmitter() {
		override def getEmittedLightLevel: Float = if (getShapeItem() != null) 1 else 0
	})

	/*
	override def isItemValidForSlot(slotID: Int, Item: Item): Boolean = {
		slotID match {
			case 0 => Item.getItem.isInstanceOf[ItemCard]
			case `modeSlotID` => Item.getItem.isInstanceOf[IProjectorMode]
			case x: Int if x < 26 => Item.getItem.isInstanceOf[IModule]
			case _ => true
		}
	}*/

	override def start() {
		super.start()
		calculateField(postCalculation)
	}

	def postCalculation() = if (clientSideSimulationRequired) Game.instance.networkManager.sync(PacketBlock.field, this)

	private def clientSideSimulationRequired: Boolean = {
		return getModuleCount(Content.moduleRepulsion) > 0
	}

	/**
	 * Initiate a field calculation
	 */
	protected override def calculateField(callBack: () => Unit = null) {
		if (Game.instance.networkManager.isServer && !isCalculating) {
			if (getShapeItem != null) {
				forceFields = Set.empty
			}

			super.calculateField(callBack)
			isCompleteConstructing = false
			fieldRequireTicks = getModules().exists(_.requireTicks)
		}
	}

	override def write(packet: Packet) {
		super.write(packet)
		packet.getID match {
			case PacketBlock.field =>
			/*
			val nbt = new NBTTagCompound
			val nbtList = new NBTTagList
			calculatedField foreach (vec => nbtList.appendTag(vec.toNBT))
			nbt.setTag("blockList", nbtList)
			ModularForceFieldSystem.packetHandler.sendToAll(new PacketTile(this, PacketBlock.field.id: Integer, nbt))*/
			case PacketBlock.effect =>
				packet <<< position
			case PacketBlock.effect2 =>
				packet <<< position
			case _ =>
		}
	}

	override def read(packet: Packet) {
		super.read(packet)

		if (Game.instance.networkManager.isClient) {
			if (packet.getID == PacketBlock.effect) {
				//Spawns a holographic beam
				val packetType = packet.readInt
				val target = new Vector3d(packet.readInt, packet.readInt, packet.readInt) + 0.5
				val pos = position.toDouble + 0.5

				if (packetType == 1) {
					world.addClientEntity(Content.fxFortronBeam).setPosition(pos).asInstanceOf[FXFortronBeam].setTarget(target)
					world.addClientEntity(Content.fxHologramProgress).setPosition(pos)
				}
				else if (packetType == 2) {
					world.addClientEntity(Content.fxFortronBeam).setPosition(pos).asInstanceOf[FXFortronBeam].setTarget(target).setColor(FieldColor.red)
					world.addClientEntity(Content.fxHologramProgress).setPosition(pos).asInstanceOf[FXHologramProgress].setColor(FieldColor.red)
				}
			}
			else if (packet.getID == PacketBlock.field) {
				//Receives the entire force field
				//				val nbt = PacketUtils.readTag(packet)
				//				val nbtList = nbt.getTagList("blockList", 10)
				//				calculatedField = mutable.Set(((0 until nbtList.tagCount) map (i => new Vector3d(nbtList.getCompoundTagAt(i)))).toArray: _ *)
			}
		}
		else {
			if (packet.getID == PacketBlock.toggleMode2) {
				isInverted = !isInverted
			}
		}
	}

	override def update(deltaTime: Double) {
		super.update(deltaTime)

		if (isActive && getShapeItem != null && removeFortron(getFortronCost, false) >= this.getFortronCost) {
			consumeCost()

			if (ticks % 10 == 0 || markFieldUpdate || fieldRequireTicks) {
				if (calculatedField == null) {
					calculateField(postCalculation)
				}
				else {
					projectField()
				}
			}

			if (isActive && Game.instance.networkManager.isClient) {
				animation += getFortronCost / 100f
			}

			if (ticks % (2 * 20) == 0 && getModuleCount(Content.moduleSilence) <= 0) {
				//TODO: Fix world sound effects
				//worldObj.playSoundEffect(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D, Reference.prefix + "field", 0.6f, (1 - this.worldObj.rand.nextFloat * 0.1f))
			}
		}
		else if (Game.instance.networkManager.isServer) {
			destroyField()
		}
	}

	/**
	 * Projects a force field based on the calculations made.
	 */
	def projectField() {
		//TODO: We cannot construct a field if it intersects another field with different frequency.
		if (!isCalculating) {
			val potentialField = calculatedField
			val relevantModules = getModules(getModuleSlots: _*)

			if (!relevantModules.exists(_.onCreateField(this, potentialField))) {
				if (!isCompleteConstructing || markFieldUpdate || fieldRequireTicks) {
					markFieldUpdate = false

					if (forceFields.size <= 0) {
						if (getShapeItem.isInstanceOf[CacheHandler]) {
							getShapeItem.asInstanceOf[CacheHandler].clearCache
						}
					}

					val constructionSpeed = Math.min(getProjectionSpeed, Settings.maxForceFieldsPerTick)

					//Creates a collection of positions that will be evaluated
					val evaluateField = potentialField
						.view.par
						.filter(!_.equals(position))
						.filter(v => !world.getBlock(v).isPresent || canReplaceBlock(v, world.getBlock(v).get()))
						.filter(v => world.getBlock(v).isPresent && world.getBlock(v).get().sameType(Content.forceField))
						.take(constructionSpeed)

					//The collection containing the coordinates to actually place the field blocks.
					var constructField = Set.empty[Vector3i]

					val result = evaluateField.forall(
						vector => {
							var flag = ProjectState.pass

							for (module <- relevantModules) {
								if (flag == ProjectState.pass) {
									flag = module.onProject(this, vector)
								}
							}

							if (flag == ProjectState.pass) {
								constructField += vector
							}

							flag != ProjectState.cancel
						})

					if (result) {
						constructField.foreach(
							pos => {
								/**
								 * Default force field block placement action.
								 */
								if (Game.instance.networkManager.isServer) {
									world.setBlock(pos, Content.forceField)
									world.getBlock(pos).get().asInstanceOf[BlockForceField].setProjector(position)
								}

								forceFields += pos
							})
					}

					isCompleteConstructing = evaluateField.size == 0
				}
			}
		}
	}

	private def canReplaceBlock(vector: Vector3i, block: Block): Boolean = {
		/*	return block == null ||
				(getModuleCount(Content.moduleDisintegration) > 0 && block.getBlockHardness(this.worldObj, vector.xi, vector.yi, vector.zi) != -1) ||
				(block.getMaterial.isLiquid || block == Blocks.snow || block == Blocks.vine || block == Blocks.tallgrass || block == Blocks.deadbush || block.isReplaceable(world, vector.xi, vector.yi, vector.zi))
	*/
		return true
	}

	def getProjectionSpeed: Int = 28 + 28 * getModuleCount(Content.moduleSpeed, getModuleSlots: _*)

	override def markDirty() {
		super.markDirty()

		if (world != null) {
			destroyField()
		}
	}

	override def unload() {
		destroyField()
		super.unload()
	}

	def destroyField() {
		if (Game.instance.networkManager.isServer && calculatedField != null && !isCalculating) {
			getModules(getModuleSlots: _*).forall(!_.onDestroyField(this, calculatedField))
			//TODO: Parallelism?
			calculatedField
				.view
				.filter(v => world.getBlock(v).isPresent && world.getBlock(v).get().sameType(Content.forceField))
				.foreach(v => world.removeBlock(v))

			forceFields = Set.empty
			calculatedField = null
			isCompleteConstructing = false
			fieldRequireTicks = false
		}
	}

	override def getForceFields: JSet[Vector3i] = forceFields

	override def isInField(position: Vector3d) = if (getShapeItem != null) getStructure.intersects(position) else false

	/*
	def isAccessGranted(checkWorld: World, checkPos: Vector3d, player: EntityPlayer, action: PlayerInteractEvent.Action): Boolean = {
		var hasPerm = true

		if (action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && checkPos.getTileEntity(checkWorld) != null) {
			if (getModuleCount(Content.moduleBlockAccess) > 0) {
				hasPerm = hasPermission(player.getGameProfile, MFFSPermissions.world)
			}
		}

		if (hasPerm) {
			if (getModuleCount(Content.moduleBlockAlter) > 0 && (player.getCurrentEquippedItem != null || action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK)) {
				hasPerm = hasPermission(player.getGameProfile, MFFSPermissions.blockAlter)
			}
		}

		return hasPerm
	}*/

	def getFilterItems: Set[Item] = (26 until 32).map(inventory.get).collect { case op: Optional[Item] if op.isPresent => op.get }.toSet

	def isInvertedFilter: Boolean = isInverted

	/**
	 * Returns Fortron cost in ticks.
	 */
	protected override def doGetFortronCost: Int = if (this.getShapeItem != null) Math.round(super.doGetFortronCost + this.getShapeItem.getFortronCost(this.getAmplifier)) else 0

	protected override def getAmplifier: Float = {
		if (getShapeItem.isInstanceOf[ItemShapeCustom]) {
			return Math.max(getShapeItem.asInstanceOf[ItemShapeCustom].fieldSize / 100, 1)
		}
		return Math.max(Math.min((if (calculatedField != null) calculatedField.size else 0) / 1000, 10), 1)
	}
}