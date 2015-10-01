package myschedule.rest.util;

/**
 * 
 * @author gigdata@semaifour.com
 *
 */
public class ResponseBuilder {
    public static String resourceAlreadyExists(String resourceType, String resourceName) {
        return String.format("CONFLICT",
                String.format("{\"reason\" : \"%s %s Already Exists\"}", resourceType, resourceName));
    }

    public static String resourceNotFound(String resourceType, String resourceName) {
        return response("NOT_FOUND",
                String.format("{\"reason\" : \"%s %s Does Not exist\"}", resourceType, resourceName));
    }

    public static String resourceCreated(String resourceType, String resourceName) {
        return String.format("CREATED",
                String.format("{\"message\" : \"Resource %s %s created\"}", resourceType, resourceName));
    }

    public static String response(String status, Object message) {
        return status + message.toString();
    }

    public static String internalServerError(Exception e) {
        return response("INTERNAL_SERVER_ERROR", e);
    }

    public static String badRequest(String message) {
        return response("BAD_REQUEST",
                String.format("{\"reason\" : \"%s\"}", message));
    }

    public static String resourceDeleted(String resourceType, String resourceName) {
        return response("OK",
                String.format("{\"message\" : \"Resource %s %s deleted\"}", resourceType, resourceName));
    }
    
    public static String success(String name ,String message) {
        return String.format("{\"message\" : \" %s operation sucessful. %s  \"}",name, message);
    }
}

