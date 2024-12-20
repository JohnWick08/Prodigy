package ca.exponet.firebolt
import grails.gorm.transactions.Transactional
import org.springframework.scheduling.annotation.Async
import java.text.SimpleDateFormat
import java.util.concurrent.locks.ReentrantLock
@Transactional
class APIQueueService {
    // API requests states
    private static final String PENDING = "Pending"
    private static final String PROCESSING = "Processing"
    private static final String COMPLETED = "Completed"
    private static final String FAILED = "Failed"
    // API requests type
    private static final String setShippingTemplate = "SET_SHIPPING_TEMPLATE"
    private static final String setHandlingTime = "SET_HANDLING_TIME"
    // mark if the queue is running or not
    private static final ReentrantLock queueLock = new ReentrantLock()
    def skuMappingService
    List<APIRequestEntry> fetchPendingRequests() {
        return APIRequestEntry.findAllByStatus(PENDING, [sort: "createdAt", order: "asc"])
    }
    void updateStatus(String id, String status) {
        def request = APIRequestEntry.findByApiRequestEntryId(id)
        if (request) {
            request.status = status
            request.updatedAt = new Date()
            request.save(flush: true, failOnError: true)
        }
    }
    /**
     * Process all the requests in the queue until the queue is empty, it emptys by the priority of the request
     */
    def processQueue() {
        if (queueLock.tryLock()) {
            try {
                println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] Processing API Request Queue: STARTED")
                while (true) {
                    // Fetch the next top-priority request
                    def request = APIRequestEntry.findByStatus(PENDING, [sort: "priority", order: "asc"])
                    // Exit the loop if all the requests are cleared
                    if (!request) {
                        break
                    }
                    // Mark this API Request as processing and record updateTime
                    APIRequestEntry.withTransaction {
                        request.status = PROCESSING
                        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                        request.updatedAt = utcFormat.format(new Date())
                        request.save(flush: true, failOnError: true)
                    }
                    // Start processing this request
                    processAPIRequestEntry(request)
                    // wait after a request is being processed
                    Thread.sleep(request.waitTime)
                }
                println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] Processing API Request Queue: COMPLETED")
            }
            catch (e) {
                println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Processing API Request Queue: FAILED")
                e.printStackTrace()
                // mark the queue is no longer processing
            }
            finally {
                queueLock.unlock()
            }
        }
        else {
            println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] Queue is already being processed.")
        }
    }
    /**
     * Process a single request from the queue
     *
     * @param request: the APIRequestEntry object that is gonna be processed
     */
    synchronized private def processAPIRequestEntry(APIRequestEntry request) {
        switch (request.requestType) {
            case setShippingTemplate:
                def productId = request.requestDataMap?.sku
                def selectedTemplate = request.requestDataMap?.selectedTemplate
                def message = ""
                try {
                    WebstoreProduct webstoreProduct = WebstoreProduct.findByWebstoreProductId(productId)
                    println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Trying to set shipping template for webstore product with product ID:" + productId + " from " + webstoreProduct.shippingTemplate + " to " + selectedTemplate)
                    if (webstoreProduct) {
                        def result = skuMappingService.updatefulfillmentAvailabilityByUser(true, false, webstoreProduct, selectedTemplate, null)
                        if (result.success) {
                            WebstoreProduct.withTransaction {
                                webstoreProduct.shippingTemplate = selectedTemplate
                                webstoreProduct.save(flush: true, failOnError: true)
                            }
                            println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Successfully set shipping template for productId " + productId)
                            request.status = COMPLETED
                        }
                        else {
                            request.status = FAILED
                            message = result.message
                        }
                    } else {
                        println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Failed to set shipping template for productId " + productId + ", Reason: No webstore product found for with this product Id")
                        request.status = FAILED
                        message = "No related webstore product found with productId " + productId
                    }
                }
                catch (e) {
                    message = "Failed to set shipping template for productId " + productId + ", an exception has been thrown to the log"
                    println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Failed to set shipping template for productId " + productId)
                    e.printStackTrace()
                    request.status = FAILED
                }
                finally {
                    APIRequestEntry.withTransaction {
                        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                        request.updatedAt = utcFormat.format(new Date())
                        request.requestFeedback = message
                        request.save(flush: true, failOnError: true)
                    }
                }
                break
            case setHandlingTime:
                def productId = request.requestDataMap?.sku
                def handlingTime = request.requestDataMap?.handlingTime
                def message = ""
                try {
                    WebstoreProduct webstoreProduct = WebstoreProduct.findByWebstoreProductId(productId)
                    println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Trying to set handling time for webstore product with product ID: " + productId + " from " + webstoreProduct.handlingTime + " to " + handlingTime)
                    if (webstoreProduct) {
                        def result = skuMappingService.updatefulfillmentAvailabilityByUser(false, true, webstoreProduct, null, handlingTime)
                        if (result.success) {
                            WebstoreProduct.withTransaction {
                                webstoreProduct.handlingTime = handlingTime
                                webstoreProduct.save(flush: true, failOnError: true)
                            }
                            println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Successfully set handling time for productId " + productId)
                            request.status = COMPLETED
                        }
                        else {
                            request.status = FAILED
                            message = result.message
                        }
                        message = result.message
                    } else {
                        println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Failed to set handling time for product with productId " + productId + ", Reason: No webstore product found for with this product Id")
                        request.status = FAILED
                        message = "No related webstore product found with productId " + productId
                    }
                }
                catch (e) {
                    message = "Failed to set handling time for productId " + productId + ", an exception has been thrown to the log"
                    println("["+new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())+"] Failed to set handling time for productId " + productId)
                    e.printStackTrace()
                    request.status = FAILED
                }
                finally {
                    APIRequestEntry.withTransaction {
                        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                        request.updatedAt = utcFormat.format(new Date())
                        request.requestFeedback = message
                        request.save(flush: true, failOnError: true)
                    }
                }
                break
        }
    }
    /**
     * Returns the sum of two integers.
     *
     * @param a the first integer
     * @param b the second integer
     * @return the sum of a and b
     * @throws IllegalArgumentException if both arguments are negative
     */
    def fetchRequestStatuses (apiRequestEntryIds) {
        int total = apiRequestEntryIds.size()
        int completed = 0
        int failed = 0
        String messages = ""
        for (String apiRequestEntryId: apiRequestEntryIds) {
            APIRequestEntry entry = APIRequestEntry.findByApiRequestEntryId(apiRequestEntryId)
            if (entry) {
                switch (entry.status) {
                    case COMPLETED:
                        completed++
                        break
                    case FAILED:
                        failed++
                        messages += entry.requestFeedback + "\$"
                        break;
                }
            }
        }
        messages = completed + " out of " + total + " updated \$" + messages
        return [success: true, message: messages, completed: completed + failed == total]
    }
}