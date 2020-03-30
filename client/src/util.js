export {deepCopy, sleep}

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

/**
 * An awaitable 'sleep' function which works like you would expect.
 * @param ms Time to sleep in milliseconds
 * @returns {Promise<void>}
 */
async function sleep (ms) {
  await new Promise(resolve => setTimeout(resolve, ms))
}
