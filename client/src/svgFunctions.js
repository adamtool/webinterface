import {pointOnRect, pointOnCircle} from './shapeIntersections'

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

// Return the 'd' attribute for a svg path element that will represent a link in a Petri Net or
// other graph.
// 'this' should be a GraphEditor object.  So you can call this using e.g.
// pathForLink.call(this, link) from within a method of the GraphEditor component.
// (The file GraphEditor was just way too huge, so I chose to break it down a little bit this way.)
//
function pathForLink (d, options) {
  // Determine where the endpoint of the path should be
  // This can be specified via 'options.endpoint' or determined based on the 'target' of the link
  let targetPoint
  if (options && options.endpoint) {
    targetPoint = options.endpoint
  } else {
    switch (d.target.type) {
      case 'ENVPLACE':
      case 'SYSPLACE': {
        // The target node is a circle.
        targetPoint = pointOnCircle(
          d.source.x,
          d.source.y,
          this.nodeRadius,
          d.target.x,
          d.target.y,
          false)
        break
      }
      case 'TRANSITION':
      case 'GRAPH_STRATEGY_BDD_STATE': {
        // The target node is a rectangle.
        targetPoint = pointOnRect(
          d.source.x,
          d.source.y,
          d.target.x - this.calculateNodeWidth(d.target) / 2,
          d.target.y - this.calculateNodeHeight(d.target) / 2,
          d.target.x + this.calculateNodeWidth(d.target) / 2,
          d.target.y + this.calculateNodeHeight(d.target) / 2,
          false
        )
      }
    }
  }

  const targetX = targetPoint['x']
  const targetY = targetPoint['y']
  // Save the length of the link in order to place a label at its midpoint (see below)
  const dx = targetX - d.source.x
  const dy = targetY - d.source.y
  d.pathLength = Math.sqrt(dx * dx + dy * dy)

  // This means source -> target and target -> source are both links
  const multipleLinksBetweenNodes = this.links.find(link => link !== d && link.source === d.target && link.target === d.source)
  const linkIsLoop = d.target === d.source
  const isStraightLink = !multipleLinksBetweenNodes && !linkIsLoop
  if (isStraightLink) {
    // Straight line for a single edge between two distinct nodes
    return `M${d.source.x},${d.source.y} L${targetX},${targetY}`
  } else if (multipleLinksBetweenNodes) {
    return arcPath(d.source.x, d.source.y, targetX, targetY)
  } else if (linkIsLoop) {
    // Place the loop around the upper-right corner of the node.
    const x1 = d.source.x + this.calculateNodeWidth(d.source) / 2
    const y1 = d.source.y
    const x2 = d.source.x
    const y2 = d.source.y - this.calculateNodeHeight(d.source) / 2
    return loopPath(x1, y1, x2, y2)
  }
}

export {
  rectanglePath, arcPath, loopPath, containingBorder, pathForLink
}
