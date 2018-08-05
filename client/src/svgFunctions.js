function rectanglePath (x0, y0, x1, y1) {
  return `M${x0},${y0}
              L${x0},${y1}
              L${x1},${y1}
              L${x1}, ${y0}
              L${x0}, ${y0}`
}

export {
  rectanglePath
}
