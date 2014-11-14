package resonantinduction.core.interfaces

import resonant.api.grid.INode
import resonant.lib.transform.vector.IVectorWorld

/**
 * Applied to any node that will act as a mechanical object
 *
 * @author Darkguardsman, Calclavia
 */
trait TMechanicalNode extends INode with IVectorWorld
{
  /**
   * Gets the angular velocity of the mechanical device from a specific side
   *
   * @return Angular velocity in meters per second
   */
  def angularVelocity: Double

  /**
   * Gets the torque of the mechanical device from a specific side
   *
   * @return force
   */
  def torque: Double

  /**
   * The mechanical load
   * @return Torque in Newton meters per second
   */
  def getLoad = 10D

  /**
   * Moment of inertia = m * r * r
   * Where "m" is the mass and "r" is the radius of the object.
   */
  def momentOfInertia = 2 * 0.5 * 0.5

  /**
   * Does the direction flip on this side for rotation
   *
   * @param other - The other mechanical node
   * @return boolean, true = flipped, false = not
   */
  def inverseRotation(other: TMechanicalNode): Boolean = true

  /**
   * Applies rotational force and velocity to this node increasing its current rotation value
   *
   * @param torque          - force at an angle
   */
  def rotate(torque: Double)
}