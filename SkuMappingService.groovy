@Transactional
@Slf4j
class SkuMappingService {
    def setShippingTemplates (skus, selectedTemplate) {
        // check if skus are provided
        if (!skus) {
            return [success: false, message: "No Skus Provided"]
        }
        // check if shipping template is provided
        if (!selectedTemplate) {
            return [success: false, message: "No Shipping Template Selected"]
        }
        // list of api request entry ids to be returned
        def apiRequestEntryIds = []
        // loop through each sku and record APIRequestEntry
        for (String sku: skus) {
            APIRequestEntry newEntry = new APIRequestEntry()
            APIRequestEntry.withTransaction {
                newEntry.apiRequestEntryId = UUID.randomUUID().toString()
                apiRequestEntryIds.add(newEntry.apiRequestEntryId)
                // getting the create date in UTC format
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                newEntry.createdAt = utcFormat.format(new Date())
                newEntry.updatedAt = newEntry.createdAt
                def map = [:]
                map["sku"] = sku
                map["selectedTemplate"] = selectedTemplate
                newEntry.requestDataMap = map
                newEntry.status = APIQueueService.PENDING
                newEntry.priority = 1
                newEntry.requestType = APIQueueService.@setShippingTemplate
                newEntry.waitTime = 250 // the rate Amazon provides is 5 request per second, if we perform a request every 0.25 s, it can make sure we never get throttled
                newEntry.save(flush: true, failOnError: true)
            }
        }
        // trigger the queue to start processing
        Thread.start {
            APIQueueService.processQueue()
        }
        return [success: true, apiRequestEntryIds: apiRequestEntryIds]
    }
    def setHandlingTimes (skus, handlingTime) {
        // check if sku is provided
        if (!skus) {
            return [success: false, message: "No Skus Provided"]
        }
        // check if handling time is provided
        if (!handlingTime) {
            return [success: false, message: "No Handling Time Provided"]
        }
        // list of api request entry ids to be returned
        def apiRequestEntryIds = []
        // loop through each sku and record APIRequestEntry
        for (String sku: skus) {
            APIRequestEntry newEntry = new APIRequestEntry()
            APIRequestEntry.withTransaction {
                newEntry.apiRequestEntryId = UUID.randomUUID().toString()
                apiRequestEntryIds.add(newEntry.apiRequestEntryId)
                // getting the create date in UTC format
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                newEntry.createdAt = utcFormat.format(new Date())
                newEntry.updatedAt = newEntry.createdAt
                def map = [:]
                map["sku"] = sku
                map["handlingTime"] = handlingTime
                newEntry.requestDataMap = map
                newEntry.status = APIQueueService.PENDING
                newEntry.priority = 1
                newEntry.requestType = APIQueueService.@setHandlingTime
                newEntry.waitTime = 250 // the rate Amazon provides is 5 request per second, if we perform a request every 0.25 s, it can make sure we never get throttled
                newEntry.save(flush: true, failOnError: true)
            }
        }
        // trigger the queue to start processing
        Thread.start {
            APIQueueService.processQueue()
        }
        return [success: true, apiRequestEntryIds: apiRequestEntryIds]
    }
}