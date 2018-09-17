function coerceShape(shapeString) {
  if(shapeString == null) {
    return x => x
  }

  const shapeStringFixedForEval = `_unused = ${shapeString}`
  return (msg) => eval(shapeStringFixedForEval)
}

module.exports = {
  coerceShape,
}
