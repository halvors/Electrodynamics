package assemblyline.client.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import assemblyline.client.model.ModelAngledBelt;
import assemblyline.client.model.ModelConveyorBelt;
import assemblyline.common.AssemblyLine;
import assemblyline.common.machine.belt.TileEntityConveyorBelt;
import assemblyline.common.machine.belt.TileEntityConveyorBelt.SlantType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderConveyorBelt extends TileEntitySpecialRenderer
{
	public static final ModelConveyorBelt MODEL = new ModelConveyorBelt();
	public static final ModelAngledBelt MODEL2 = new ModelAngledBelt();

	private void renderAModelAt(TileEntityConveyorBelt tileEntity, double x, double y, double z, float f)
	{
		boolean mid = tileEntity.getIsMiddleBelt();
		SlantType slantType = tileEntity.getSlant();
		int face = tileEntity.getDirection().ordinal();

		GL11.glPushMatrix();
		GL11.glTranslatef((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
		GL11.glRotatef(180f, 0f, 0f, 1f);

		int frame = tileEntity.getAnimationFrame();

		if (slantType != null && slantType != SlantType.NONE)
		{
			switch (face)
			{
				case 2:
					GL11.glRotatef(180f, 0f, 1f, 0f);
					break;
				case 3:
					GL11.glRotatef(0f, 0f, 1f, 0f);
					break;
				case 4:
					GL11.glRotatef(90f, 0f, 1f, 0f);
					break;
				case 5:
					GL11.glRotatef(-90f, 0f, 1f, 0f);
					break;
			}
			
			this.bindTextureByName(AssemblyLine.TEXTURE_PATH + "slantedbelt/frame" + frame + ".png");

			if (slantType == SlantType.UP)
			{
				GL11.glTranslatef(0f, 0.8f, -0.8f);
				GL11.glRotatef(180f, 0f, 1f, 1f);
				MODEL2.render(0.0625F);
			}
			else
			{
				GL11.glRotatef(180f, 0f, 1f, 0f);
				MODEL2.render(0.0625F);
			}
		}
		else
		{
			switch (face)
			{
				case 2:
					GL11.glRotatef(0f, 0f, 1f, 0f);
					break;
				case 3:
					GL11.glRotatef(180f, 0f, 1f, 0f);
					break;
				case 4:
					GL11.glRotatef(-90f, 0f, 1f, 0f);
					break;
				case 5:
					GL11.glRotatef(90f, 0f, 1f, 0f);
					break;
			}

			this.bindTextureByName(AssemblyLine.TEXTURE_PATH + "belt/frame" + frame + ".png");
			MODEL.render(0.0625F, (float) Math.toRadians(tileEntity.wheelRotation), tileEntity.getIsLastBelt(), tileEntity.getIsFirstBelt(), false);

		}

		int ent = tileEntity.worldObj.getBlockId(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);

		GL11.glPopMatrix();

	}

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double var2, double var4, double var6, float var8)
	{
		this.renderAModelAt((TileEntityConveyorBelt) tileEntity, var2, var4, var6, var8);
	}

}