package ca.exponet.firebolt
class APIRequestEntry {
    static mapWith = "mongo"
    static mapping = {
        collection "apirequestentry"
        database "exponetamz"
        version false
        apiRequestEntryId index: true
        createdAt inedx: true
        status index: true
    }
    String apiRequestEntryId
    Map<String, String> requestDataMap
    String requestType // SET_SHIPPING_TEMPLATE, SET_HANDLING_TIME
    String createdAt
    String updatedAt
    String status // Pending, Processing, Completed, Failed
    Integer priority // for 1 ~ 5 as 1 is highest priority
    String requestFeedback
    Integer waitTime // how long the queue should wait after this request has been executed
    static constraints = {
        apiRequestEntryId nullable: false
        status nullable: false
        createdAt nullable: false
        updatedAt nullable: false
        requestType nullable: false
        requestDataMap nullable: true
        priority nullable: false
        requestFeedback nullable: true
        waitTime nullable: false
    }
}