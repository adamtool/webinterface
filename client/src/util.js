export {deepCopy}

/**
 * Perform a deep copy of an arbitrary object.
 * This has some caveats.
 * See https://stackoverflow.com/questions/20662319/javascript-deep-copy-using-json
 * @param object
 * @returns A deep copy/clone of object
 */
function deepCopy(object) {
  return JSON.parse(JSON.stringify(object))
}

