// Use Cytoscape's cose layout to lay out nodes in an aesthetically pleasing way
const cytoscape = require('cytoscape')

export {
  layoutNodes
}
// Returns a promise that yields a map of node IDs to x/y coordinates
// of the form { nodeId: {x: <xPosition> y: <yPostion>} }
function layoutNodes (nodes, links, marginPercentage, xMin, xMax, yMin, yMax) {
  console.log(`Laying out nodes with a ${marginPercentage * 100}% border, xMin ${xMin}, xMax ${xMax}, yMin ${yMin}, yMax ${yMax}`)
  // Convert D3's nodes/links into Cytoscape's elements
  const nodeElements = nodes.map(node => {
    return ({
      data: {id: node.id}
    })
  })
  console.log('links we are given')
  console.log(links)
  const linkElements = links.map(link => {
    return ({
      data: {
        id: `${link.source.id}_${link.target.id}`,
        source: link.source.id,
        target: link.target.id
      }
    })
  })
  console.log('our link elements')
  console.log(linkElements)
  const elements = nodeElements.concat(linkElements)

  const sizeX = xMax - xMin
  const sizeY = yMax - yMin
  const marginX = marginPercentage * sizeX
  const marginY = marginPercentage * sizeY

  const boundingBox = {
    x1: xMin + marginX,
    y1: yMin + marginY,
    x2: xMax - marginX,
    y2: yMax - marginY
  }

  const cy = cytoscape({
    headless: true,
    elements: elements,
    style: [
      {
        selector: 'node',
        style: {
          'width': 100,
          'height': 100
        }
      }
    ],
    layout: {
      name: 'random',
      boundingBox: boundingBox
    }
  })

  // http://js.cytoscape.org/#layouts/cose
  const layout = cy.layout({
    name: 'cose',
    animate: false,
    randomize: false, // You have to specify initial positions for the nodes for cose to work.
    // See https://stackoverflow.com/questions/43392468/cytoscape-js-layout-doesnt-work-in-firefox-works-in-chrome
    refresh: 20,
    boundingBox: boundingBox,
    // Node repulsion (non overlapping) multiplier
    nodeRepulsion: function (node) { return 2048 },

    // Node repulsion (overlapping) multiplier
    nodeOverlap: 4,

    // Ideal edge (non nested) length
    idealEdgeLength: function (edge) { return 32 },

    // Divisor to compute edge forces
    edgeElasticity: function (edge) { return 32 },

    // Nesting factor (multiplier) to compute ideal edge length for nested edges
    nestingFactor: 1.2,

    // Gravity force (constant)
    gravity: 1,

    // Maximum number of iterations to perform
    numIter: 1000,

    // Initial temperature (maximum node displacement)
    initialTemp: 1000,

    // Cooling factor (how the temperature is reduced between consecutive iterations
    coolingFactor: 0.99,

    // Lower temperature threshold (below this point the layout will end)
    minTemp: 1.0
  })

  const promise = layout.promiseOn('layoutstop').then(function (event) {
    console.log('Layout stopped')
    const positions = {}
    // Put nodes' positions into a map
    cy.nodes().forEach(node => {
      positions[node.id()] = node.position()
    })
    console.log(positions)
    return positions
  })
  layout.run()
  return promise
}
