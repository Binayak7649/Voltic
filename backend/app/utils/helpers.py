from typing import Any, Dict

def standard_response(success: bool = True, message: str = "", data: Any = None, error_code: str = None) -> Dict[str, Any]:
    resp = {
        "success": success,
        "message": message,
        "data": data
    }
    if error_code:
        resp["error_code"] = error_code
    return resp
