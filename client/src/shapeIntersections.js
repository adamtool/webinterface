export {
  pointOnRect,
  pointOnCircle
}

// TODO Find out what the license is for this piece of code.  (I've emailed Rob.)
/**
 * Finds the intersection point between
 *     * the rectangle
 *       with parallel sides to the x and y axes
 *     * the half-line pointing towards (x,y)
 *       originating from the middle of the rectangle
 *
 * Note: the function works given min[XY] <= max[XY],
 *       even though minY may not be the "top" of the rectangle
 *       because the coordinate system is flipped.
 * Note: if the input is inside the rectangle,
 *       the line segment wouldn't have an intersection with the rectangle,
 *       but the projected half-line does.
 * Warning: passing in the middle of the rectangle will return the midpoint itself
 *          there are infinitely many half-lines projected in all directions,
 *          so let's just shortcut to midpoint (GIGO).
 *
 * @param x:Number x coordinate of point to build the half-line from
 * @param y:Number y coordinate of point to build the half-line from
 * @param minX:Number the "left" side of the rectangle
 * @param minY:Number the "top" side of the rectangle
 * @param maxX:Number the "right" side of the rectangle
 * @param maxY:Number the "bottom" side of the rectangle
 * @param validate:boolean (optional) whether to treat point inside the rect as error
 * @return an object with x and y members for the intersection
 * @throws if validate == true and (x,y) is inside the rectangle
 * @author TWiStErRob
 * @see <a href="http://stackoverflow.com/a/31254199/253468">source</a>
 * @see <a href="http://stackoverflow.com/a/18292964/253468">based on</a>
 */
function pointOnRect (x, y, minX, minY, maxX, maxY, validate) {
  // assert minX <= maxX;
  // assert minY <= maxY;
  if (validate && (minX < x && x < maxX) && (minY < y && y < maxY)) {
    throw new Error('Point ' + [x, y] + 'cannot be inside ' +
      'the rectangle: ' + [minX, minY] + ' - ' + [maxX, maxY] + '.')
  }
  var midX = (minX + maxX) / 2
  var midY = (minY + maxY) / 2
  // if (midX - x == 0) -> m == ±Inf -> minYx/maxYx == x (because value / ±Inf = ±0)
  var m = (midY - y) / (midX - x)

  if (x <= midX) { // check "left" side
    var minXy = m * (minX - x) + y
    if (minY <= minXy && minXy <= maxY) { return {x: minX, y: minXy} }
  }

  if (x >= midX) { // check "right" side
    var maxXy = m * (maxX - x) + y
    if (minY <= maxXy && maxXy <= maxY) { return {x: maxX, y: maxXy} }
  }

  if (y <= midY) { // check "top" side
    var minYx = (minY - y) / m + x
    if (minX <= minYx && minYx <= maxX) { return {x: minYx, y: minY} }
  }

  if (y >= midY) { // check "bottom" side
    var maxYx = (maxY - y) / m + x
    if (minX <= maxYx && maxYx <= maxX) { return {x: maxYx, y: maxY} }
  }

  // edge case when finding midpoint intersection: m = 0/0 = NaN
  if (x === midX && y === midY) return {x: x, y: y}

  // Should never happen :) If it does, please tell me!
  throw new Error('Cannot find intersection for ' + [x, y] +
    ' inside rectangle ' + [minX, minY] + ' - ' + [maxX, maxY] + '.')
}

/**
 * Finds the intersection point between:
 *     * The circle of radius at (circleCenterX, circleCenterY)
 *     and
 *     * The half line going from (x, y) to the center of the circle
 *
 * This is only guaranteed to work if (x, y) lies outside of the circle.
 * If (x, y) is in the circle, it might give some kind of nonsense result, but that's probably OK
 * as long as we're just using this to draw links between nodes -- if a node is overlapping another
 * node like that, the link will probably not be visible.
 *
 * @param x:Number x coordinate of point to build the half-line from
 * @param y:Number y coordinate of point to build the half-line from
 * @param radius:Number the radius of the circle
 * @param circleCenterX:Number The X coordinate of the center of the circle
 * @param circleCenterY:Number The Y coordinate of the center of the circle
 * @param validate:boolean (optional) whether to treat point inside the circle as error
 * @return an object with x and y members for the intersection
 * @throws if validate == true and (x,y) is inside the circle
 */
function pointOnCircle (x, y, radius, circleCenterX, circleCenterY, validate) {
  const dx = circleCenterX - x
  const dy = circleCenterY - y
  const distance = Math.sqrt(dx * dx + dy * dy)
  if (validate && distance < radius) {
    throw new Error('Point ' + [x, y] + 'cannot be inside the circle centered at ' +
      [circleCenterX, circleCenterY] + ' with radius' + radius + '.')
  }
  const normX = dx / distance
  const normY = dy / distance
  const targetX = circleCenterX - radius * normX
  const targetY = circleCenterY - radius * normY
  return {
    x: targetX,
    y: targetY
  }
}
