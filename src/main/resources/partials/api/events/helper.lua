local Exports = {}

-- [PARTIAL:START]
function Exports.tonumber(arg) return tonumber(arg) end
function Exports.tostring(arg) return tostring(arg) end
function Exports.global(id) return _G[id] end
-- [PARTIAL:STOP]

return Exports
