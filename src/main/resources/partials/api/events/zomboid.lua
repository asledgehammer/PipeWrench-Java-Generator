local Exports = {}

-- [PARTIAL:START]
function Exports.addEventListener(id, func) Events[id].Add(func) end
function Exports.removeEventListener(id, func) Events[id].Add(func) end
-- [PARTIAL:STOP]

return Exports
