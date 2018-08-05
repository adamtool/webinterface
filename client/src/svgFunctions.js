function rectanglePath (x0, y0, x1, y1) {
  return `M${x0},${y0}
              L${x0},${y1}
              L${x1},${y1}
              L${x1}, ${y0}
              L${x0}, ${y0}`
}

function arcPath (x1, y1, x2, y2) {
  // Draw an arc between two points
  const dx = x2 - x1
  const dy = y2 - y1
  const dr = Math.sqrt(dx * dx + dy * dy)
  // Defaults for normal edge.
  const drx = dr
  const dry = dr
  const xRotation = 0 // degrees
  const largeArc = 0 // 1 or 0
  const sweep = 1 // 1 or 0

  return 'M' + x1 + ',' + y1 + 'A' + drx + ',' + dry + ' ' + xRotation + ',' + largeArc + ',' + sweep + ' ' + x2 + ',' + y2
}

function loopPath (x1, y1, x2, y2) {
  // Self edge.
  // Fiddle with this angle to get loop oriented.
  const xRotation = 0

  // Needs to be 1.
  const largeArc = 1
  // Change sweep to change orientation of loop.
  const sweep = 0

  // Make drx and dry different to get an ellipse
  // instead of a circle.
  const drx = 45
  const dry = 45

  return 'M' + x1 + ',' + y1 + 'A' + drx + ',' + dry + ' ' + xRotation + ',' + largeArc + ',' + sweep + ' ' + x2 + ',' + y2
}

// Return the rectangular bounding box surrounding a set of dom nodes (e.g. svg elements)
function containingBorder (domNodes) {
  let xMin = 9999999
  let yMin = 9999999
  let xMax = -9999999
  let yMax = -9999999
  domNodes.forEach(domNode => {
    // const rect = this.getBoundingClientRect()
    // const {left: x0, right: x1, top: y0, bottom: y1} = rect
    const bbox = transformedBoundingBox(domNode)
    const {x: x0, y: y0, width, height} = bbox
    const x1 = x0 + width
    const y1 = y0 + height

    if (x0 < xMin) {
      xMin = x0
    }
    if (x1 > xMax) {
      xMax = x1
    }
    if (y0 < yMin) {
      yMin = y0
    }
    if (y1 > yMax) {
      yMax = y1
    }
  })
  return [xMin, yMin, xMax, yMax]
}

// Calculate the bounding box of an element with respect to its parent element
function transformedBoundingBox (el) {
  var bb = el.getBBox()
  var svg = el.ownerSVGElement
  // This method has been removed from the DOM api in chrome and firefox.  Below is a polyfill.
  // var m = el.getTransformToElement(el.parentNode)
  var m = el.parentNode.getScreenCTM().inverse().multiply(el.getScreenCTM())

  // Create an array of all four points for the original bounding box
  var pts = [
    svg.createSVGPoint(), svg.createSVGPoint(),
    svg.createSVGPoint(), svg.createSVGPoint()
  ]
  pts[0].x = bb.x
  pts[0].y = bb.y
  pts[1].x = bb.x + bb.width
  pts[1].y = bb.y
  pts[2].x = bb.x + bb.width
  pts[2].y = bb.y + bb.height
  pts[3].x = bb.x
  pts[3].y = bb.y + bb.height

  // Transform each into the space of the parent,
  // and calculate the min/max points from that.
  var xMin = Infinity
  var xMax = -Infinity
  var yMin = Infinity
  var yMax = -Infinity
  pts.forEach(function (pt) {
    pt = pt.matrixTransform(m)
    xMin = Math.min(xMin, pt.x)
    xMax = Math.max(xMax, pt.x)
    yMin = Math.min(yMin, pt.y)
    yMax = Math.max(yMax, pt.y)
  })

  // Update the bounding box with the new values
  bb.x = xMin
  bb.width = xMax - xMin
  bb.y = yMin
  bb.height = yMax - yMin
  return bb
}

export {
  rectanglePath, arcPath, loopPath, containingBorder
}
