function matchesFilter(jsonObject, filterJsonObject) {
  if(typeof jsonObject === 'object') {
    return (typeof filterJsonObject === 'object') &&
      Object.keys(filterJsonObject).map(key => {
        return jsonObject.hasOwnProperty(key) && matchesFilter(jsonObject[key], filterJsonObject[key])
      }).indexOf(false) < 0
  }

  return jsonObject === filterJsonObject
}

module.exports = {
  matchesFilter
}
