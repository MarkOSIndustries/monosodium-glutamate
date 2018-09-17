function matchesFilter(jsonObject, partiallySpecifiedJsonObject) {
  if(typeof jsonObject === 'object') {
    return (typeof partiallySpecifiedJsonObject === 'object') &&
      Object.keys(partiallySpecifiedJsonObject).map(key => {
        return jsonObject.hasOwnProperty(key) && matchesFilter(jsonObject[key], partiallySpecifiedJsonObject[key])
      }).indexOf(false) < 0
  }

  return jsonObject == partiallySpecifiedJsonObject
}

function coerceFilter(filterJsonString) {
  if(filterJsonString === null) {
    return x=>true
  }

  const partiallySpecifiedJsonObject = JSON.parse(filterJsonString)
  return (jsonObject) => matchesFilter(jsonObject, partiallySpecifiedJsonObject)
}

module.exports = {
  coerceFilter,
}
